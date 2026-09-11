//! Morton codes for section sorting before LOD banding.

#[inline]
fn part1by2(mut n: u32) -> u32 {
    n &= 0x0000_03ff;
    n = (n | (n << 16)) & 0x0300_00FF;
    n = (n | (n << 8)) & 0x0300_F00F;
    n = (n | (n << 4)) & 0x030C_30C3;
    n = (n | (n << 2)) & 0x0924_9249;
    n
}

pub fn encode(x: i32, y: i32, z: i32) -> u32 {
    let xi = (x as u32) & 0x3ff;
    let yi = (y as u32) & 0x3ff;
    let zi = (z as u32) & 0x3ff;
    part1by2(xi) | (part1by2(yi) << 1) | (part1by2(zi) << 2)
}

pub fn encode_batch(x: &[i32], y: &[i32], z: &[i32], out: &mut [u32]) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len());
    for i in 0..n {
        out[i] = encode(x[i], y[i], z[i]);
    }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_lod_morton3(
    x: *const i32,
    y: *const i32,
    z: *const i32,
    out: *mut u32,
    n: u64,
) {
    if x.is_null() || y.is_null() || z.is_null() || out.is_null() || n == 0 {
        return;
    }
    let n = n as usize;
    encode_batch(
        std::slice::from_raw_parts(x, n),
        std::slice::from_raw_parts(y, n),
        std::slice::from_raw_parts(z, n),
        std::slice::from_raw_parts_mut(out, n),
    );
}
