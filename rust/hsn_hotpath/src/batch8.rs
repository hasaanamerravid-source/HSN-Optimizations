//! Eight-wide scalar kernels used when AVX2 is off or n is small.
//! 8-wide unroll matches what Rust/LLVM auto-vectorizes into SSE2 on any x86_64.

#[inline(always)]
fn drop_at(v: f64, limit_sq: f64) -> u8 {
    u8::from(v > limit_sq)
}

pub fn cull8(input: &[f64], limit_sq: f64, out: &mut [u8]) {
    let n = input.len().min(out.len());
    let mut i = 0;
    // 16-wide for LLVM's vectorizer to emit 2×128-bit or 1×256-bit pass
    while i + 16 <= n {
        out[i]      = drop_at(input[i],      limit_sq);
        out[i + 1]  = drop_at(input[i + 1],  limit_sq);
        out[i + 2]  = drop_at(input[i + 2],  limit_sq);
        out[i + 3]  = drop_at(input[i + 3],  limit_sq);
        out[i + 4]  = drop_at(input[i + 4],  limit_sq);
        out[i + 5]  = drop_at(input[i + 5],  limit_sq);
        out[i + 6]  = drop_at(input[i + 6],  limit_sq);
        out[i + 7]  = drop_at(input[i + 7],  limit_sq);
        out[i + 8]  = drop_at(input[i + 8],  limit_sq);
        out[i + 9]  = drop_at(input[i + 9],  limit_sq);
        out[i + 10] = drop_at(input[i + 10], limit_sq);
        out[i + 11] = drop_at(input[i + 11], limit_sq);
        out[i + 12] = drop_at(input[i + 12], limit_sq);
        out[i + 13] = drop_at(input[i + 13], limit_sq);
        out[i + 14] = drop_at(input[i + 14], limit_sq);
        out[i + 15] = drop_at(input[i + 15], limit_sq);
        i += 16;
    }
    while i + 8 <= n {
        out[i]     = drop_at(input[i],     limit_sq);
        out[i + 1] = drop_at(input[i + 1], limit_sq);
        out[i + 2] = drop_at(input[i + 2], limit_sq);
        out[i + 3] = drop_at(input[i + 3], limit_sq);
        out[i + 4] = drop_at(input[i + 4], limit_sq);
        out[i + 5] = drop_at(input[i + 5], limit_sq);
        out[i + 6] = drop_at(input[i + 6], limit_sq);
        out[i + 7] = drop_at(input[i + 7], limit_sq);
        i += 8;
    }
    while i < n {
        out[i] = drop_at(input[i], limit_sq);
        i += 1;
    }
}

pub fn dist_sq8(
    x: &[f64],
    y: &[f64],
    z: &[f64],
    ox: f64,
    oy: f64,
    oz: f64,
    out: &mut [f64],
) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len());
    let mut i = 0;
    // 8-wide for LLVM's vectorizer; matched loads allow 2×AVX2 passes
    while i + 8 <= n {
        macro_rules! dsq {
            ($j:expr) => {{
                let dx = x[i + $j] - ox;
                let dy = y[i + $j] - oy;
                let dz = z[i + $j] - oz;
                dx * dx + dy * dy + dz * dz
            }};
        }
        out[i]     = dsq!(0);
        out[i + 1] = dsq!(1);
        out[i + 2] = dsq!(2);
        out[i + 3] = dsq!(3);
        out[i + 4] = dsq!(4);
        out[i + 5] = dsq!(5);
        out[i + 6] = dsq!(6);
        out[i + 7] = dsq!(7);
        i += 8;
    }
    while i < n {
        let dx = x[i] - ox;
        let dy = y[i] - oy;
        let dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
        i += 1;
    }
}

pub fn lod_band(dist_sq: &[f64], near_sq: f64, mid_sq: f64, far_sq: f64, out: &mut [u8]) {
    let n = dist_sq.len().min(out.len());
    for i in 0..n {
        let d = dist_sq[i];
        out[i] = if d <= near_sq {
            0
        } else if d <= mid_sq {
            1
        } else if d <= far_sq {
            2
        } else {
            3
        };
    }
}

pub fn count_keep(mask: &[u8]) -> u64 {
    // SWAR popcount: count non-zero bytes 8 at a time
    let mut count = 0u64;
    let mut i = 0;
    while i + 8 <= mask.len() {
        let chunk = u64::from_ne_bytes(mask[i..i + 8].try_into().unwrap());
        // Zero-byte detector: bit 7 set iff that byte is 0 (keep in cull masks).
        let zeros = chunk.wrapping_sub(0x0101_0101_0101_0101u64)
            & !chunk
            & 0x8080_8080_8080_8080u64;
        count += (zeros >> 7).count_ones() as u64;
        i += 8;
    }
    while i < mask.len() {
        count += u64::from(mask[i] == 0);
        i += 1;
    }
    count
}

unsafe fn slf<'a>(ptr: *const f64, n: usize) -> &'a [f64] {
    if ptr.is_null() || n == 0 {
        &[]
    } else {
        unsafe { std::slice::from_raw_parts(ptr, n) }
    }
}

unsafe fn slf_mut<'a>(ptr: *mut f64, n: usize) -> &'a mut [f64] {
    if ptr.is_null() || n == 0 {
        &mut []
    } else {
        unsafe { std::slice::from_raw_parts_mut(ptr, n) }
    }
}

unsafe fn slu_mut<'a>(ptr: *mut u8, n: usize) -> &'a mut [u8] {
    if ptr.is_null() || n == 0 {
        &mut []
    } else {
        unsafe { std::slice::from_raw_parts_mut(ptr, n) }
    }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_cull8(input: *const f64, limit_sq: f64, out: *mut u8, n: u64) {
    let len = n as usize;
    cull8(slf(input, len), limit_sq, slu_mut(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_dist_sq8(
    x: *const f64,
    y: *const f64,
    z: *const f64,
    ox: f64,
    oy: f64,
    oz: f64,
    out: *mut f64,
    n: u64,
) {
    let len = n as usize;
    dist_sq8(slf(x, len), slf(y, len), slf(z, len), ox, oy, oz, slf_mut(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_lod_band(
    dist_sq: *const f64,
    near_sq: f64,
    mid_sq: f64,
    far_sq: f64,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    lod_band(slf(dist_sq, len), near_sq, mid_sq, far_sq, slu_mut(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_count_keep(mask: *const u8, n: u64) -> u64 {
    if mask.is_null() || n == 0 {
        return 0;
    }
    let sl = unsafe { std::slice::from_raw_parts(mask, n as usize) };
    count_keep(sl)
}
