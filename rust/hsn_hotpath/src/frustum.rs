//! Packed frustum tests. One FFI call per section batch.
//!
//! Core logic lives here; aabb.rs re-exports the same functions under the
//! hsn_hot_* symbol names so both ABIs remain available without duplicating
//! the implementation.

#[inline(always)]
pub(crate) fn plane_out(a: f32, b: f32, c: f32, d: f32, min: [f32; 3], max: [f32; 3]) -> bool {
    let px = if a >= 0.0 { max[0] } else { min[0] };
    let py = if b >= 0.0 { max[1] } else { min[1] };
    let pz = if c >= 0.0 { max[2] } else { min[2] };
    a * px + b * py + c * pz + d < 0.0
}

pub fn cull_aabb(planes: &[f32], aabb: &[f32], out: &mut [u8]) {
    if planes.len() < 24 {
        return;
    }
    let n = out.len().min(aabb.len() / 6);
    // Prefetch planes into registers for all iterations
    let p = [
        [planes[0],  planes[1],  planes[2],  planes[3]],
        [planes[4],  planes[5],  planes[6],  planes[7]],
        [planes[8],  planes[9],  planes[10], planes[11]],
        [planes[12], planes[13], planes[14], planes[15]],
        [planes[16], planes[17], planes[18], planes[19]],
        [planes[20], planes[21], planes[22], planes[23]],
    ];
    for i in 0..n {
        let b = i * 6;
        let min = [aabb[b], aabb[b + 1], aabb[b + 2]];
        let max = [aabb[b + 3], aabb[b + 4], aabb[b + 5]];
        out[i] = u8::from(
            plane_out(p[0][0], p[0][1], p[0][2], p[0][3], min, max)
                || plane_out(p[1][0], p[1][1], p[1][2], p[1][3], min, max)
                || plane_out(p[2][0], p[2][1], p[2][2], p[2][3], min, max)
                || plane_out(p[3][0], p[3][1], p[3][2], p[3][3], min, max)
                || plane_out(p[4][0], p[4][1], p[4][2], p[4][3], min, max)
                || plane_out(p[5][0], p[5][1], p[5][2], p[5][3], min, max),
        );
    }
}

pub fn cull_sphere(planes: &[f32], xyzr: &[f32], out: &mut [u8]) {
    if planes.len() < 24 {
        return;
    }
    let n = out.len().min(xyzr.len() / 4);
    let p = [
        [planes[0],  planes[1],  planes[2],  planes[3]],
        [planes[4],  planes[5],  planes[6],  planes[7]],
        [planes[8],  planes[9],  planes[10], planes[11]],
        [planes[12], planes[13], planes[14], planes[15]],
        [planes[16], planes[17], planes[18], planes[19]],
        [planes[20], planes[21], planes[22], planes[23]],
    ];
    for i in 0..n {
        let b = i * 4;
        let x = xyzr[b];
        let y = xyzr[b + 1];
        let z = xyzr[b + 2];
        let r = xyzr[b + 3];
        let nr = -r;
        out[i] = u8::from(
            p[0][0] * x + p[0][1] * y + p[0][2] * z + p[0][3] < nr
                || p[1][0] * x + p[1][1] * y + p[1][2] * z + p[1][3] < nr
                || p[2][0] * x + p[2][1] * y + p[2][2] * z + p[2][3] < nr
                || p[3][0] * x + p[3][1] * y + p[3][2] * z + p[3][3] < nr
                || p[4][0] * x + p[4][1] * y + p[4][2] * z + p[4][3] < nr
                || p[5][0] * x + p[5][1] * y + p[5][2] * z + p[5][3] < nr,
        );
    }
}

/// Extract and normalize 6 frustum planes from a column-major 4x4 view-projection matrix.
/// Output is 24 floats: [a,b,c,d] × 6 planes (left, right, bottom, top, near, far).
pub fn extract_planes(viewproj: &[f32], out: &mut [f32]) {
    if viewproj.len() < 16 || out.len() < 24 {
        return;
    }
    let m = viewproj;
    let mut store = |idx: usize, a: f32, b: f32, c: f32, d: f32| {
        let len_sq = a * a + b * b + c * c;
        let inv = if len_sq > 1e-12 { 1.0 / len_sq.sqrt() } else { 0.0 };
        let o = idx * 4;
        out[o]     = a * inv;
        out[o + 1] = b * inv;
        out[o + 2] = c * inv;
        out[o + 3] = d * inv;
    };
    store(0, m[3] + m[0],  m[7] + m[4],  m[11] + m[8],  m[15] + m[12]);
    store(1, m[3] - m[0],  m[7] - m[4],  m[11] - m[8],  m[15] - m[12]);
    store(2, m[3] + m[1],  m[7] + m[5],  m[11] + m[9],  m[15] + m[13]);
    store(3, m[3] - m[1],  m[7] - m[5],  m[11] - m[9],  m[15] - m[13]);
    store(4, m[3] + m[2],  m[7] + m[6],  m[11] + m[10], m[15] + m[14]);
    store(5, m[3] - m[2],  m[7] - m[6],  m[11] - m[10], m[15] - m[14]);
}

// --- Legacy hsn_rs_* exports (kept for ABI compatibility) ---

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_cull_aabb_f32(
    planes: *const f32,
    aabb: *const f32,
    out: *mut u8,
    n: u64,
) {
    if planes.is_null() || aabb.is_null() || out.is_null() || n == 0 {
        return;
    }
    let planes = std::slice::from_raw_parts(planes, 24);
    let aabb   = std::slice::from_raw_parts(aabb, n as usize * 6);
    let out    = std::slice::from_raw_parts_mut(out, n as usize);
    cull_aabb(planes, aabb, out);
}

#[no_mangle]
pub unsafe extern "C" fn hsn_rs_cull_sphere_f32(
    planes: *const f32,
    xyzr: *const f32,
    out: *mut u8,
    n: u64,
) {
    if planes.is_null() || xyzr.is_null() || out.is_null() || n == 0 {
        return;
    }
    let planes = std::slice::from_raw_parts(planes, 24);
    let xyzr   = std::slice::from_raw_parts(xyzr, n as usize * 4);
    let out    = std::slice::from_raw_parts_mut(out, n as usize);
    cull_sphere(planes, xyzr, out);
}
