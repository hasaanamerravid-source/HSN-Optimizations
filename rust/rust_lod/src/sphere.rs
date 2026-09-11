//! Sphere LOD bands and radius-aware keep masks.

#[inline]
fn dist_sq(x: f32, y: f32, z: f32, ox: f32, oy: f32, oz: f32) -> f32 {
    let dx = x - ox;
    let dy = y - oy;
    let dz = z - oz;
    dx * dx + dy * dy + dz * dz
}

pub fn sphere_lod(
    x: &[f32],
    y: &[f32],
    z: &[f32],
    r: &[f32],
    ox: f32,
    oy: f32,
    oz: f32,
    t0: f32,
    t1: f32,
    t2: f32,
    out: &mut [u8],
) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len()).min(r.len());
    for i in 0..n {
        let d = dist_sq(x[i], y[i], z[i], ox, oy, oz);
        // Larger radii stay visible a bit longer.
        let adj = (d - r[i] * r[i]).max(0.0);
        out[i] = if adj <= t0 {
            0
        } else if adj <= t1 {
            1
        } else if adj <= t2 {
            2
        } else {
            3
        };
    }
}

pub fn keep_inside(
    x: &[f32],
    y: &[f32],
    z: &[f32],
    ox: f32,
    oy: f32,
    oz: f32,
    limit_sq: f32,
    out: &mut [u8],
) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len());
    for i in 0..n {
        out[i] = u8::from(dist_sq(x[i], y[i], z[i], ox, oy, oz) > limit_sq);
    }
}

unsafe fn sl<'a>(ptr: *const f32, n: usize) -> &'a [f32] {
    if ptr.is_null() || n == 0 {
        &[]
    } else {
        unsafe { std::slice::from_raw_parts(ptr, n) }
    }
}

unsafe fn slu<'a>(ptr: *mut u8, n: usize) -> &'a mut [u8] {
    if ptr.is_null() || n == 0 {
        &mut []
    } else {
        unsafe { std::slice::from_raw_parts_mut(ptr, n) }
    }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_lod_sphere(
    x: *const f32,
    y: *const f32,
    z: *const f32,
    r: *const f32,
    ox: f32,
    oy: f32,
    oz: f32,
    t0: f32,
    t1: f32,
    t2: f32,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    sphere_lod(
        sl(x, len),
        sl(y, len),
        sl(z, len),
        sl(r, len),
        ox,
        oy,
        oz,
        t0,
        t1,
        t2,
        slu(out, len),
    );
}

#[no_mangle]
pub unsafe extern "C" fn hsn_lod_keep_inside(
    x: *const f32,
    y: *const f32,
    z: *const f32,
    ox: f32,
    oy: f32,
    oz: f32,
    limit_sq: f32,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    keep_inside(sl(x, len), sl(y, len), sl(z, len), ox, oy, oz, limit_sq, slu(out, len));
}
