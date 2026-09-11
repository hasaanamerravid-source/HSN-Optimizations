//! Packed keep-mask helpers.

/// Pack up to 64 flags into a bitmask, 8 bytes at a time.
pub fn pack_u8(flags: &[u8]) -> u64 {
    let n = flags.len().min(64);
    let mut acc = 0u64;
    let mut i = 0;
    // Process 8 bytes at a time using bitwise ops
    while i + 8 <= n {
        let chunk = u64::from_ne_bytes(flags[i..i + 8].try_into().unwrap());
        // For each byte: non-zero -> set the corresponding bit.
        // Spread non-zero bytes into their bit position.
        let nz = chunk_nonzero_mask(chunk);
        acc |= nz << i;
        i += 8;
    }
    while i < n {
        if flags[i] != 0 {
            acc |= 1u64 << i;
        }
        i += 1;
    }
    acc
}

/// Returns a u64 with bit i set iff byte i of `chunk` is non-zero.
#[inline]
fn chunk_nonzero_mask(chunk: u64) -> u64 {
    // Saturating byte subtraction trick: (x | (x.wrapping_sub(1))) >> 7 & 1 per byte
    // but simpler: for each byte b, (b.min(1)) gives 0 or 1.
    // We use the SWAR technique: detect non-zero bytes in parallel.
    // byte is non-zero iff (byte - 1) underflows into high bit when ORed with byte.
    let lo = chunk & 0x0101_0101_0101_0101u64;
    let has_zero = chunk.wrapping_sub(lo) & !chunk & 0x8080_8080_8080_8080u64;
    // has_zero has 0x80 in each zero byte position — invert for non-zero
    // Compress the 8 high bits (one per byte) into the low 8 bits
    let nonzero_bytes = !has_zero & 0x8080_8080_8080_8080u64;
    compress_high_bits(nonzero_bytes)
}

/// Collect bit 7 of each byte into the low 8 bits of the result.
#[inline]
fn compress_high_bits(v: u64) -> u64 {
    // Each byte's bit 7 -> output bit i
    let b0 = (v >> 7) & 1;
    let b1 = (v >> 14) & 2;
    let b2 = (v >> 21) & 4;
    let b3 = (v >> 28) & 8;
    let b4 = (v >> 35) & 16;
    let b5 = (v >> 42) & 32;
    let b6 = (v >> 49) & 64;
    let b7 = (v >> 56) & 128;
    b0 | b1 | b2 | b3 | b4 | b5 | b6 | b7
}

pub fn unpack_u64(word: u64, out: &mut [u8]) {
    let n = out.len().min(64);
    for i in 0..n {
        out[i] = ((word >> i) & 1) as u8;
    }
}

/// Count non-zero flags using popcount on 8-byte chunks.
pub fn popcount(flags: &[u8]) -> u32 {
    let mut count = 0u32;
    let mut i = 0;
    while i + 8 <= flags.len() {
        let chunk = u64::from_ne_bytes(flags[i..i + 8].try_into().unwrap());
        let zeros = chunk.wrapping_sub(0x0101_0101_0101_0101u64)
            & !chunk
            & 0x8080_8080_8080_8080u64;
        let nonzero = !zeros & 0x8080_8080_8080_8080u64;
        count += (nonzero >> 7).count_ones();
        i += 8;
    }
    while i < flags.len() {
        count += u32::from(flags[i] != 0);
        i += 1;
    }
    count
}

pub fn and_mask(a: &[u8], b: &[u8], out: &mut [u8]) {
    let n = out.len().min(a.len()).min(b.len());
    let mut i = 0;
    while i + 8 <= n {
        let ca = u64::from_ne_bytes(a[i..i + 8].try_into().unwrap());
        let cb = u64::from_ne_bytes(b[i..i + 8].try_into().unwrap());
        out[i..i + 8].copy_from_slice(&(ca & cb).to_ne_bytes());
        i += 8;
    }
    while i < n {
        out[i] = a[i] & b[i];
        i += 1;
    }
}

pub fn or_mask(a: &[u8], b: &[u8], out: &mut [u8]) {
    let n = out.len().min(a.len()).min(b.len());
    let mut i = 0;
    while i + 8 <= n {
        let ca = u64::from_ne_bytes(a[i..i + 8].try_into().unwrap());
        let cb = u64::from_ne_bytes(b[i..i + 8].try_into().unwrap());
        out[i..i + 8].copy_from_slice(&(ca | cb).to_ne_bytes());
        i += 8;
    }
    while i < n {
        out[i] = a[i] | b[i];
        i += 1;
    }
}

pub fn keep_u8(rnd: &[u8], thresh: u8, out: &mut [u8]) {
    let n = out.len().min(rnd.len());
    for i in 0..n {
        out[i] = u8::from(rnd[i] < thresh);
    }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_pack_u8(flags: *const u8, n: u64) -> u64 {
    if flags.is_null() || n == 0 {
        return 0;
    }
    pack_u8(std::slice::from_raw_parts(flags, n as usize))
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_unpack_u64(word: u64, out: *mut u8, n: u64) {
    if out.is_null() || n == 0 {
        return;
    }
    unpack_u64(word, std::slice::from_raw_parts_mut(out, n as usize));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_popcount_u8(flags: *const u8, n: u64) -> u32 {
    if flags.is_null() || n == 0 {
        return 0;
    }
    popcount(std::slice::from_raw_parts(flags, n as usize))
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_and_mask_u8(
    a: *const u8,
    b: *const u8,
    out: *mut u8,
    n: u64,
) {
    if a.is_null() || b.is_null() || out.is_null() || n == 0 {
        return;
    }
    let n = n as usize;
    and_mask(
        std::slice::from_raw_parts(a, n),
        std::slice::from_raw_parts(b, n),
        std::slice::from_raw_parts_mut(out, n),
    );
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_keep_u8(rnd: *const u8, thresh: u8, out: *mut u8, n: u64) {
    if rnd.is_null() || out.is_null() || n == 0 {
        return;
    }
    let n = n as usize;
    keep_u8(
        std::slice::from_raw_parts(rnd, n),
        thresh,
        std::slice::from_raw_parts_mut(out, n),
    );
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pack_matches_naive() {
        let flags: Vec<u8> = (0..64).map(|i| if i % 3 == 0 { 1 } else { 0 }).collect();
        let mut naive = 0u64;
        for (i, &f) in flags.iter().enumerate() {
            if f != 0 { naive |= 1u64 << i; }
        }
        assert_eq!(pack_u8(&flags), naive);
    }

    #[test]
    fn popcount_matches_naive() {
        let flags: Vec<u8> = (0..100).map(|i| if i % 5 == 0 { 1 } else { 0 }).collect();
        let naive: u32 = flags.iter().map(|&f| u32::from(f != 0)).sum();
        assert_eq!(popcount(&flags), naive);
    }

    #[test]
    fn and_or_8wide() {
        let a = vec![1u8, 0, 1, 0, 1, 0, 1, 0, 1];
        let b = vec![1u8, 1, 0, 0, 1, 1, 0, 0, 1];
        let mut out = vec![0u8; 9];
        and_mask(&a, &b, &mut out);
        assert_eq!(out, vec![1, 0, 0, 0, 1, 0, 0, 0, 1]);
        or_mask(&a, &b, &mut out);
        assert_eq!(out, vec![1, 1, 1, 0, 1, 1, 1, 0, 1]);
    }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_or_mask_u8(
    a: *const u8,
    b: *const u8,
    out: *mut u8,
    n: u64,
) {
    if a.is_null() || b.is_null() || out.is_null() || n == 0 {
        return;
    }
    let n = n as usize;
    or_mask(
        std::slice::from_raw_parts(a, n),
        std::slice::from_raw_parts(b, n),
        std::slice::from_raw_parts_mut(out, n),
    );
}
