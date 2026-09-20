//! Forward-cone and combined distance+cone masks.
//! Used for section occupancy when the camera is looking along +XZ.

#[inline]
fn cone_drop(dx: f64, dz: f64, fx: f64, fz: f64) -> u8 {
    u8::from(dx * fx + dz * fz < 0.0)
}

pub fn cone_mask(x: &[f64], z: &[f64], ox: f64, oz: f64, fx: f64, fz: f64, out: &mut [u8]) {
    let n = out.len().min(x.len()).min(z.len());
    let mut i = 0;
    while i + 8 <= n {
        out[i]     = cone_drop(x[i]     - ox, z[i]     - oz, fx, fz);
        out[i + 1] = cone_drop(x[i + 1] - ox, z[i + 1] - oz, fx, fz);
        out[i + 2] = cone_drop(x[i + 2] - ox, z[i + 2] - oz, fx, fz);
        out[i + 3] = cone_drop(x[i + 3] - ox, z[i + 3] - oz, fx, fz);
        out[i + 4] = cone_drop(x[i + 4] - ox, z[i + 4] - oz, fx, fz);
        out[i + 5] = cone_drop(x[i + 5] - ox, z[i + 5] - oz, fx, fz);
        out[i + 6] = cone_drop(x[i + 6] - ox, z[i + 6] - oz, fx, fz);
        out[i + 7] = cone_drop(x[i + 7] - ox, z[i + 7] - oz, fx, fz);
        i += 8;
    }
    while i + 4 <= n {
        out[i]     = cone_drop(x[i]     - ox, z[i]     - oz, fx, fz);
        out[i + 1] = cone_drop(x[i + 1] - ox, z[i + 1] - oz, fx, fz);
        out[i + 2] = cone_drop(x[i + 2] - ox, z[i + 2] - oz, fx, fz);
        out[i + 3] = cone_drop(x[i + 3] - ox, z[i + 3] - oz, fx, fz);
        i += 4;
    }
    while i < n {
        out[i] = cone_drop(x[i] - ox, z[i] - oz, fx, fz);
        i += 1;
    }
}

/// Combined distance+cone cull, 4-wide unrolled to match cone_mask.
pub fn combined_mask(
    x: &[f64],
    y: &[f64],
    z: &[f64],
    ox: f64,
    oy: f64,
    oz: f64,
    limit_sq: f64,
    fx: f64,
    fz: f64,
    out: &mut [u8],
) {
    let n = out.len().min(x.len()).min(y.len()).min(z.len());
    let mut i = 0;
    while i + 4 <= n {
        let dx0 = x[i]     - ox; let dy0 = y[i]     - oy; let dz0 = z[i]     - oz;
        let dx1 = x[i + 1] - ox; let dy1 = y[i + 1] - oy; let dz1 = z[i + 1] - oz;
        let dx2 = x[i + 2] - ox; let dy2 = y[i + 2] - oy; let dz2 = z[i + 2] - oz;
        let dx3 = x[i + 3] - ox; let dy3 = y[i + 3] - oy; let dz3 = z[i + 3] - oz;
        out[i]     = u8::from(dx0*dx0 + dy0*dy0 + dz0*dz0 > limit_sq || dx0*fx + dz0*fz < 0.0);
        out[i + 1] = u8::from(dx1*dx1 + dy1*dy1 + dz1*dz1 > limit_sq || dx1*fx + dz1*fz < 0.0);
        out[i + 2] = u8::from(dx2*dx2 + dy2*dy2 + dz2*dz2 > limit_sq || dx2*fx + dz2*fz < 0.0);
        out[i + 3] = u8::from(dx3*dx3 + dy3*dy3 + dz3*dz3 > limit_sq || dx3*fx + dz3*fz < 0.0);
        i += 4;
    }
    while i < n {
        let dx = x[i] - ox;
        let dy = y[i] - oy;
        let dz = z[i] - oz;
        out[i] = u8::from(dx * dx + dy * dy + dz * dz > limit_sq || dx * fx + dz * fz < 0.0);
        i += 1;
    }
}

unsafe fn sl<'a>(ptr: *const f64, n: usize) -> &'a [f64] {
    if ptr.is_null() || n == 0 { &[] } else { unsafe { std::slice::from_raw_parts(ptr, n) } }
}

unsafe fn sl_mut<'a>(ptr: *mut u8, n: usize) -> &'a mut [u8] {
    if ptr.is_null() || n == 0 { &mut [] } else { unsafe { std::slice::from_raw_parts_mut(ptr, n) } }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_cone_mask(
    x: *const f64,
    z: *const f64,
    ox: f64,
    oz: f64,
    fx: f64,
    fz: f64,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    cone_mask(sl(x, len), sl(z, len), ox, oz, fx, fz, sl_mut(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_combined_mask(
    x: *const f64,
    y: *const f64,
    z: *const f64,
    ox: f64,
    oy: f64,
    oz: f64,
    limit_sq: f64,
    fx: f64,
    fz: f64,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    combined_mask(
        sl(x, len), sl(y, len), sl(z, len),
        ox, oy, oz, limit_sq, fx, fz,
        sl_mut(out, len),
    );
}
