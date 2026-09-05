//! Level-of-detail thresholds on packed XYZ. No heap allocation on the
//! exported path: only raw slices over caller memory.

use std::sync::atomic::{AtomicU32, Ordering};

static ABI: AtomicU32 = AtomicU32::new(0x4C4F4431); // 'LOD1'

#[inline]
fn lod_of(dist_sq: f32, t0: f32, t1: f32, t2: f32) -> u8 {
    if dist_sq <= t0 {
        0
    } else if dist_sq <= t1 {
        1
    } else if dist_sq <= t2 {
        2
    } else {
        3
    }
}

#[inline]
fn write_lod(
    x: &[f32],
    y: &[f32],
    z: &[f32],
    ox: f32,
    oy: f32,
    oz: f32,
    t0: f32,
    t1: f32,
    t2: f32,
    out: &mut [u8],
) {
    let n = x.len().min(y.len()).min(z.len()).min(out.len());
    let mut i = 0;
    while i + 4 <= n {
        let dx0 = x[i] - ox;
        let dy0 = y[i] - oy;
        let dz0 = z[i] - oz;
        let dx1 = x[i + 1] - ox;
        let dy1 = y[i + 1] - oy;
        let dz1 = z[i + 1] - oz;
        let dx2 = x[i + 2] - ox;
        let dy2 = y[i + 2] - oy;
        let dz2 = z[i + 2] - oz;
        let dx3 = x[i + 3] - ox;
        let dy3 = y[i + 3] - oy;
        let dz3 = z[i + 3] - oz;
        out[i] = lod_of(dx0 * dx0 + dy0 * dy0 + dz0 * dz0, t0, t1, t2);
        out[i + 1] = lod_of(dx1 * dx1 + dy1 * dy1 + dz1 * dz1, t0, t1, t2);
        out[i + 2] = lod_of(dx2 * dx2 + dy2 * dy2 + dz2 * dz2, t0, t1, t2);
        out[i + 3] = lod_of(dx3 * dx3 + dy3 * dy3 + dz3 * dz3, t0, t1, t2);
        i += 4;
    }
    while i < n {
        let dx = x[i] - ox;
        let dy = y[i] - oy;
        let dz = z[i] - oz;
        out[i] = lod_of(dx * dx + dy * dy + dz * dz, t0, t1, t2);
        i += 1;
    }
}

/// out[i] = LOD band 0..3 from squared distance vs t0<=t1<=t2.
#[no_mangle]
pub unsafe extern "C" fn hsn_lod_thresholds(
    x: *const f32,
    y: *const f32,
    z: *const f32,
    ox: f32,
    oy: f32,
    oz: f32,
    t0: f32,
    t1: f32,
    t2: f32,
    out: *mut u8,
    n: usize,
) {
    if x.is_null() || y.is_null() || z.is_null() || out.is_null() || n == 0 {
        return;
    }
    let xs = std::slice::from_raw_parts(x, n);
    let ys = std::slice::from_raw_parts(y, n);
    let zs = std::slice::from_raw_parts(z, n);
    let dst = std::slice::from_raw_parts_mut(out, n);
    write_lod(xs, ys, zs, ox, oy, oz, t0, t1, t2, dst);
}

/// Squared distances into a caller-owned f32 buffer. No allocation.
#[no_mangle]
pub unsafe extern "C" fn hsn_lod_dist_sq(
    x: *const f32,
    y: *const f32,
    z: *const f32,
    ox: f32,
    oy: f32,
    oz: f32,
    out: *mut f32,
    n: usize,
) {
    if x.is_null() || y.is_null() || z.is_null() || out.is_null() || n == 0 {
        return;
    }
    let xs = std::slice::from_raw_parts(x, n);
    let ys = std::slice::from_raw_parts(y, n);
    let zs = std::slice::from_raw_parts(z, n);
    let dst = std::slice::from_raw_parts_mut(out, n);
    let len = xs.len().min(ys.len()).min(zs.len()).min(dst.len());
    let mut i = 0;
    while i < len {
        let dx = xs[i] - ox;
        let dy = ys[i] - oy;
        let dz = zs[i] - oz;
        dst[i] = dx * dx + dy * dy + dz * dz;
        i += 1;
    }
}

#[no_mangle]
pub extern "C" fn hsn_lod_abi() -> i32 {
    ABI.load(Ordering::Relaxed) as i32
}

/// Zero-copy views over caller memory. No Vec, no clone, no drop glue on the
/// FFI path — the Java / C side owns the buffers for the duration of the call.
#[inline]
unsafe fn view_f32(p: *const f32, n: usize) -> &'static [f32] {
    if p.is_null() || n == 0 {
        &[]
    } else {
        std::slice::from_raw_parts(p, n)
    }
}

#[inline]
unsafe fn view_u8_mut(p: *mut u8, n: usize) -> &'static mut [u8] {
    if p.is_null() || n == 0 {
        &mut []
    } else {
        std::slice::from_raw_parts_mut(p, n)
    }
}

#[inline]
fn plane_rejects(p: &[f32], minx: f32, miny: f32, minz: f32, maxx: f32, maxy: f32, maxz: f32) -> bool {
    let px = if p[0] >= 0.0 { maxx } else { minx };
    let py = if p[1] >= 0.0 { maxy } else { miny };
    let pz = if p[2] >= 0.0 { maxz } else { minz };
    p[0] * px + p[1] * py + p[2] * pz + p[3] < 0.0
}

/// Packed AABB frustum test. `aabb` is n * 6 floats. Writes 1 if culled.
#[no_mangle]
pub unsafe extern "C" fn hsn_lod_cull_aabb_f32(
    planes24: *const f32,
    aabb: *const f32,
    out: *mut u8,
    n: usize,
) {
    if planes24.is_null() || aabb.is_null() || out.is_null() || n == 0 {
        return;
    }
    let planes = view_f32(planes24, 24);
    if planes.len() < 24 {
        return;
    }
    let boxes = view_f32(aabb, n * 6);
    let dst = view_u8_mut(out, n);
    let count = dst.len().min(boxes.len() / 6);
    let mut i = 0;
    while i < count {
        let b = i * 6;
        let mut drop = 0u8;
        let mut p = 0;
        while p < 6 {
            if plane_rejects(&planes[p * 4..], boxes[b], boxes[b + 1], boxes[b + 2], boxes[b + 3], boxes[b + 4], boxes[b + 5]) {
                drop = 1;
                break;
            }
            p += 1;
        }
        dst[i] = drop;
        i += 1;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn bands() {
        assert_eq!(lod_of(1.0, 4.0, 16.0, 64.0), 0);
        assert_eq!(lod_of(9.0, 4.0, 16.0, 64.0), 1);
        assert_eq!(lod_of(36.0, 4.0, 16.0, 64.0), 2);
        assert_eq!(lod_of(100.0, 4.0, 16.0, 64.0), 3);
    }
}
