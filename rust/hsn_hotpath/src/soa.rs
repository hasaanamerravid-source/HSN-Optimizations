//! Structure-of-arrays distance / LOD helpers.

/// Spread 10 bits into every-other bit position for Morton encoding.
#[inline(always)]
fn part1by2(mut n: u32) -> u32 {
    n &= 0x0000_03ff;
    n = (n | (n << 16)) & 0x0300_00FF;
    n = (n | (n << 8))  & 0x0300_F00F;
    n = (n | (n << 4))  & 0x030C_30C3;
    n = (n | (n << 2))  & 0x0924_9249;
    n
}

/// Squared distances in f32 SoA layout, 8-wide unrolled for auto-vectorization.
pub fn dist_sq_f32(x: &[f32], y: &[f32], z: &[f32], ox: f32, oy: f32, oz: f32, out: &mut [f32]) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len());
    let mut i = 0;
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

pub fn lod_band(dist_sq: &[f32], near_sq: f32, mid_sq: f32, out: &mut [u8]) {
    let n = out.len().min(dist_sq.len());
    for i in 0..n {
        let d = dist_sq[i];
        out[i] = if d < near_sq {
            0
        } else if d < mid_sq {
            1
        } else {
            2
        };
    }
}

/// 3D Morton code (Z-order curve). Uses integer interleaving for cache-friendly spatial access.
pub fn morton3(x: &[i32], y: &[i32], z: &[i32], out: &mut [u32]) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len());
    for i in 0..n {
        let xi = (x[i] as u32) & 0x3ff;
        let yi = (y[i] as u32) & 0x3ff;
        let zi = (z[i] as u32) & 0x3ff;
        out[i] = part1by2(xi) | (part1by2(yi) << 1) | (part1by2(zi) << 2);
    }
}

// --- C FFI exports ---

unsafe fn sf32<'a>(ptr: *const f32, n: usize) -> &'a [f32] {
    if ptr.is_null() || n == 0 { &[] }
    else { unsafe { std::slice::from_raw_parts(ptr, n) } }
}

unsafe fn sf32m<'a>(ptr: *mut f32, n: usize) -> &'a mut [f32] {
    if ptr.is_null() || n == 0 { &mut [] }
    else { unsafe { std::slice::from_raw_parts_mut(ptr, n) } }
}

unsafe fn su8m<'a>(ptr: *mut u8, n: usize) -> &'a mut [u8] {
    if ptr.is_null() || n == 0 { &mut [] }
    else { unsafe { std::slice::from_raw_parts_mut(ptr, n) } }
}

unsafe fn si32<'a>(ptr: *const i32, n: usize) -> &'a [i32] {
    if ptr.is_null() || n == 0 { &[] }
    else { unsafe { std::slice::from_raw_parts(ptr, n) } }
}

unsafe fn su32m<'a>(ptr: *mut u32, n: usize) -> &'a mut [u32] {
    if ptr.is_null() || n == 0 { &mut [] }
    else { unsafe { std::slice::from_raw_parts_mut(ptr, n) } }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_dist_sq_f32(
    x: *const f32, y: *const f32, z: *const f32,
    ox: f32, oy: f32, oz: f32,
    out: *mut f32, n: u64,
) {
    let len = n as usize;
    dist_sq_f32(sf32(x, len), sf32(y, len), sf32(z, len), ox, oy, oz, sf32m(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_lod_band_f32(
    dist_sq: *const f32, near_sq: f32, mid_sq: f32,
    out: *mut u8, n: u64,
) {
    let len = n as usize;
    lod_band(sf32(dist_sq, len), near_sq, mid_sq, su8m(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_morton3(
    x: *const i32, y: *const i32, z: *const i32,
    out: *mut u32, n: u64,
) {
    let len = n as usize;
    morton3(si32(x, len), si32(y, len), si32(z, len), su32m(out, len));
}
