//! Batch distance tests. One FFI call per array, not per entity.
//!
//! Kernels are compiled with `target_feature` so AVX2 / AVX-512 actually
//! emit wide compares. The exported functions pick a kernel from a
//! configurable policy and the CPUID bits. Missing features fall back
//! to the next legal kernel, then to scalar. That keeps old hardware running.

use std::sync::atomic::{AtomicU32, Ordering};

/// 0 = auto, 1 = scalar, 2 = AVX2, 3 = AVX-512.
static SIMD_MODE: AtomicU32 = AtomicU32::new(0);

const MODE_AUTO: u32 = 0;
const MODE_SCALAR: u32 = 1;
const MODE_AVX2: u32 = 2;
const MODE_AVX512: u32 = 3;

#[derive(Clone, Copy, PartialEq, Eq)]
enum Kernel {
    Scalar = 1,
    Avx2 = 2,
    Avx512 = 3,
}

#[inline]
fn quality_at(dist_sq: f64, max_dist: f64, start_factor: f64, min_q: f64) -> f64 {
    if !(max_dist > 0.0) {
        return 1.0;
    }
    let start_factor = start_factor.clamp(0.15, 0.95);
    let min_q = min_q.clamp(0.05, 1.0);
    if !(dist_sq > 0.0) {
        return 1.0;
    }
    let start_sq = (max_dist * start_factor) * (max_dist * start_factor);
    if dist_sq <= start_sq {
        return 1.0;
    }
    let max_sq = max_dist * max_dist;
    if dist_sq >= max_sq {
        return min_q;
    }
    let span = max_sq - start_sq;
    if span <= 0.0001 {
        return min_q;
    }
    let mut t = ((dist_sq - start_sq) / span).clamp(0.0, 1.0);
    t = t * t * (3.0 - 2.0 * t);
    (1.0 - t * (1.0 - min_q)).clamp(min_q, 1.0)
}

fn cull_scalar(input: &[f64], limit_sq: f64, out: &mut [u8]) {
    let n = input.len().min(out.len());
    let mut i = 0;
    while i + 8 <= n {
        out[i] = u8::from(input[i] > limit_sq);
        out[i + 1] = u8::from(input[i + 1] > limit_sq);
        out[i + 2] = u8::from(input[i + 2] > limit_sq);
        out[i + 3] = u8::from(input[i + 3] > limit_sq);
        out[i + 4] = u8::from(input[i + 4] > limit_sq);
        out[i + 5] = u8::from(input[i + 5] > limit_sq);
        out[i + 6] = u8::from(input[i + 6] > limit_sq);
        out[i + 7] = u8::from(input[i + 7] > limit_sq);
        i += 8;
    }
    while i < n {
        out[i] = u8::from(input[i] > limit_sq);
        i += 1;
    }
}

fn quality_scalar(
    input: &[f64],
    max_dist: f64,
    start_factor: f64,
    min_q: f64,
    out: &mut [f64],
) {
    let n = input.len().min(out.len());
    for i in 0..n {
        out[i] = quality_at(input[i], max_dist, start_factor, min_q);
    }
}

fn cpu_has_avx2() -> bool {
    #[cfg(target_arch = "x86_64")]
    {
        return is_x86_feature_detected!("avx2");
    }
    #[cfg(not(target_arch = "x86_64"))]
    false
}

fn avx512_built() -> bool {
    cfg!(hsn_has_avx512_obj)
}

fn cpu_has_avx512() -> bool {
    if !avx512_built() {
        return false;
    }
    #[cfg(target_arch = "x86_64")]
    {
        return is_x86_feature_detected!("avx512f");
    }
    #[cfg(not(target_arch = "x86_64"))]
    false
}

fn selected_kernel() -> Kernel {
    let mode = SIMD_MODE.load(Ordering::Relaxed);
    let avx2 = cpu_has_avx2();
    let avx512 = cpu_has_avx512();
    match mode {
        MODE_SCALAR => Kernel::Scalar,
        MODE_AVX2 => {
            if avx2 {
                Kernel::Avx2
            } else {
                Kernel::Scalar
            }
        }
        MODE_AVX512 => {
            if avx512 {
                Kernel::Avx512
            } else if avx2 {
                Kernel::Avx2
            } else {
                Kernel::Scalar
            }
        }
        _ => {
            if avx512 {
                Kernel::Avx512
            } else if avx2 {
                Kernel::Avx2
            } else {
                Kernel::Scalar
            }
        }
    }
}

/// Bit 0 = library, bit 1 = AVX, bit 2 = AVX2, bit 3 = FMA, bit 4 = AVX-512F.
#[no_mangle]
pub extern "C" fn hsn_cpu_flags() -> u32 {
    let mut flags = 1u32;
    #[cfg(target_arch = "x86_64")]
    {
        if is_x86_feature_detected!("avx") {
            flags |= 2;
        }
        if is_x86_feature_detected!("avx2") {
            flags |= 4;
        }
        if is_x86_feature_detected!("fma") {
            flags |= 8;
        }
        if avx512_built() && is_x86_feature_detected!("avx512f") {
            flags |= 16;
        }
    }
    flags
}

/// 0 auto, 1 scalar, 2 AVX2, 3 AVX-512. Unknown values become auto.
#[no_mangle]
pub extern "C" fn hsn_set_simd_mode(mode: u32) {
    let mode = match mode {
        MODE_SCALAR | MODE_AVX2 | MODE_AVX512 => mode,
        _ => MODE_AUTO,
    };
    SIMD_MODE.store(mode, Ordering::Relaxed);
}

/// Kernel that will actually run after policy + CPUID. 1 scalar, 2 AVX2, 3 AVX-512.
#[no_mangle]
pub extern "C" fn hsn_active_simd() -> u32 {
    selected_kernel() as u32
}

#[cfg(target_arch = "x86_64")]
#[target_feature(enable = "avx2")]
unsafe fn cull_avx2(input: &[f64], limit_sq: f64, out: &mut [u8]) {
    use std::arch::x86_64::*;
    let n = input.len().min(out.len());
    let vlimit = _mm256_set1_pd(limit_sq);
    let mut i = 0;
    while i + 8 <= n {
        let a = _mm256_loadu_pd(input.as_ptr().add(i));
        let b = _mm256_loadu_pd(input.as_ptr().add(i + 4));
        let ma = _mm256_movemask_pd(_mm256_cmp_pd(a, vlimit, _CMP_GT_OQ));
        let mb = _mm256_movemask_pd(_mm256_cmp_pd(b, vlimit, _CMP_GT_OQ));
        out[i] = (ma & 1) as u8;
        out[i + 1] = ((ma >> 1) & 1) as u8;
        out[i + 2] = ((ma >> 2) & 1) as u8;
        out[i + 3] = ((ma >> 3) & 1) as u8;
        out[i + 4] = (mb & 1) as u8;
        out[i + 5] = ((mb >> 1) & 1) as u8;
        out[i + 6] = ((mb >> 2) & 1) as u8;
        out[i + 7] = ((mb >> 3) & 1) as u8;
        i += 8;
    }
    while i + 4 <= n {
        let v = _mm256_loadu_pd(input.as_ptr().add(i));
        let mask = _mm256_movemask_pd(_mm256_cmp_pd(v, vlimit, _CMP_GT_OQ));
        out[i] = (mask & 1) as u8;
        out[i + 1] = ((mask >> 1) & 1) as u8;
        out[i + 2] = ((mask >> 2) & 1) as u8;
        out[i + 3] = ((mask >> 3) & 1) as u8;
        i += 4;
    }
    while i < n {
        out[i] = u8::from(input[i] > limit_sq);
        i += 1;
    }
}

#[cfg(target_arch = "x86_64")]
#[target_feature(enable = "avx2")]
unsafe fn quality_avx2(
    input: &[f64],
    max_dist: f64,
    start_factor: f64,
    min_q: f64,
    out: &mut [f64],
) {
    use std::arch::x86_64::*;
    let n = input.len().min(out.len());
    if !(max_dist > 0.0) {
        let mut i = 0;
        while i < n {
            out[i] = 1.0;
            i += 1;
        }
        return;
    }
    let start_factor = start_factor.clamp(0.15, 0.95);
    let min_q = min_q.clamp(0.05, 1.0);
    let start_sq = (max_dist * start_factor) * (max_dist * start_factor);
    let max_sq = max_dist * max_dist;
    let span = max_sq - start_sq;
    if span <= 0.0001 {
        let mut i = 0;
        while i < n {
            out[i] = if input[i] <= start_sq { 1.0 } else { min_q };
            i += 1;
        }
        return;
    }

    let vstart = _mm256_set1_pd(start_sq);
    let vmax = _mm256_set1_pd(max_sq);
    let vminq = _mm256_set1_pd(min_q);
    let vone = _mm256_set1_pd(1.0);
    let vzero = _mm256_set1_pd(0.0);
    let vspan = _mm256_set1_pd(span);
    let vthree = _mm256_set1_pd(3.0);
    let vtwo = _mm256_set1_pd(2.0);

    let mut i = 0;
    while i + 4 <= n {
        let d = _mm256_loadu_pd(input.as_ptr().add(i));
        let t = _mm256_div_pd(_mm256_sub_pd(d, vstart), vspan);
        let t = _mm256_min_pd(vone, _mm256_max_pd(vzero, t));
        let s = _mm256_mul_pd(
            _mm256_mul_pd(t, t),
            _mm256_sub_pd(vthree, _mm256_mul_pd(vtwo, t)),
        );
        let mut q = _mm256_sub_pd(vone, _mm256_mul_pd(s, _mm256_sub_pd(vone, vminq)));
        q = _mm256_min_pd(vone, _mm256_max_pd(vminq, q));
        let far = _mm256_cmp_pd(d, vmax, _CMP_GE_OQ);
        let near = _mm256_cmp_pd(d, vstart, _CMP_LE_OQ);
        q = _mm256_blendv_pd(q, vminq, far);
        q = _mm256_blendv_pd(q, vone, near);
        _mm256_storeu_pd(out.as_mut_ptr().add(i), q);
        i += 4;
    }
    while i < n {
        out[i] = quality_at(input[i], max_dist, start_factor, min_q);
        i += 1;
    }
}

#[cfg(hsn_has_avx512_obj)]
extern "C" {
    fn hsn_cull_avx512_impl(input: *const f64, limit_sq: f64, out: *mut u8, n: usize);
    fn hsn_quality_avx512_impl(
        input: *const f64,
        max_dist: f64,
        start_factor: f64,
        min_q: f64,
        out: *mut f64,
        n: usize,
    );
}

unsafe fn cull_avx512(input: &[f64], limit_sq: f64, out: &mut [u8]) {
    let n = input.len().min(out.len());
    #[cfg(hsn_has_avx512_obj)]
    {
        hsn_cull_avx512_impl(input.as_ptr(), limit_sq, out.as_mut_ptr(), n);
        return;
    }
    #[cfg(not(hsn_has_avx512_obj))]
    {
        let _ = n;
        cull_scalar(input, limit_sq, out);
    }
}

unsafe fn quality_avx512(
    input: &[f64],
    max_dist: f64,
    start_factor: f64,
    min_q: f64,
    out: &mut [f64],
) {
    let n = input.len().min(out.len());
    #[cfg(hsn_has_avx512_obj)]
    {
        hsn_quality_avx512_impl(
            input.as_ptr(),
            max_dist,
            start_factor,
            min_q,
            out.as_mut_ptr(),
            n,
        );
        return;
    }
    #[cfg(not(hsn_has_avx512_obj))]
    {
        let _ = n;
        quality_scalar(input, max_dist, start_factor, min_q, out);
    }
}

fn run_cull(input: &[f64], limit_sq: f64, output: &mut [u8]) {
    match selected_kernel() {
        Kernel::Avx512 => {
            #[cfg(target_arch = "x86_64")]
            unsafe {
                cull_avx512(input, limit_sq, output);
                return;
            }
            #[cfg(not(target_arch = "x86_64"))]
            cull_scalar(input, limit_sq, output);
        }
        Kernel::Avx2 => {
            #[cfg(target_arch = "x86_64")]
            unsafe {
                cull_avx2(input, limit_sq, output);
                return;
            }
            #[cfg(not(target_arch = "x86_64"))]
            cull_scalar(input, limit_sq, output);
        }
        Kernel::Scalar => cull_scalar(input, limit_sq, output),
    }
}

fn run_quality(
    input: &[f64],
    max_dist: f64,
    start_factor: f64,
    min_q: f64,
    output: &mut [f64],
) {
    match selected_kernel() {
        Kernel::Avx512 => {
            #[cfg(target_arch = "x86_64")]
            unsafe {
                quality_avx512(input, max_dist, start_factor, min_q, output);
                return;
            }
            #[cfg(not(target_arch = "x86_64"))]
            quality_scalar(input, max_dist, start_factor, min_q, output);
        }
        Kernel::Avx2 => {
            #[cfg(target_arch = "x86_64")]
            unsafe {
                quality_avx2(input, max_dist, start_factor, min_q, output);
                return;
            }
            #[cfg(not(target_arch = "x86_64"))]
            quality_scalar(input, max_dist, start_factor, min_q, output);
        }
        Kernel::Scalar => quality_scalar(input, max_dist, start_factor, min_q, output),
    }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_cull_mask(
    dist_sq: *const f64,
    limit_sq: f64,
    out: *mut u8,
    len: usize,
) {
    if dist_sq.is_null() || out.is_null() || len == 0 {
        return;
    }
    let input = std::slice::from_raw_parts(dist_sq, len);
    let output = std::slice::from_raw_parts_mut(out, len);
    run_cull(input, limit_sq, output);
}

#[no_mangle]
pub unsafe extern "C" fn hsn_quality_batch(
    dist_sq: *const f64,
    max_dist: f64,
    start_factor: f64,
    min_q: f64,
    out: *mut f64,
    len: usize,
) {
    if dist_sq.is_null() || out.is_null() || len == 0 {
        return;
    }
    let input = std::slice::from_raw_parts(dist_sq, len);
    let output = std::slice::from_raw_parts_mut(out, len);
    run_quality(input, max_dist, start_factor, min_q, output);
}

#[cfg(test)]
mod tests {
    use super::*;

    fn compare_cull(kernel_name: &str, run: impl Fn(&[f64], f64, &mut [u8])) {
        let input: Vec<f64> = (0..67).map(|i| i as f64 * 1.5).collect();
        let mut a = vec![0u8; input.len()];
        let mut b = vec![0u8; input.len()];
        cull_scalar(&input, 40.0, &mut a);
        run(&input, 40.0, &mut b);
        assert_eq!(a, b, "{kernel_name} cull mismatch");
    }

    fn compare_quality(kernel_name: &str, run: impl Fn(&[f64], &mut [f64])) {
        let input: Vec<f64> = (0..51).map(|i| (i as f64) * (i as f64)).collect();
        let mut a = vec![0.0; input.len()];
        let mut b = vec![0.0; input.len()];
        quality_scalar(&input, 32.0, 0.5, 0.15, &mut a);
        run(&input, &mut b);
        for i in 0..input.len() {
            let diff = (a[i] - b[i]).abs();
            assert!(
                diff < 1e-12,
                "{kernel_name} idx {i}: {} vs {}",
                a[i],
                b[i]
            );
        }
    }

    #[test]
    fn avx2_matches_scalar() {
        if !cpu_has_avx2() {
            return;
        }
        unsafe {
            compare_cull("avx2", |i, l, o| cull_avx2(i, l, o));
            compare_quality("avx2", |i, o| quality_avx2(i, 32.0, 0.5, 0.15, o));
        }
    }

    #[test]
    fn avx512_matches_scalar() {
        if !cpu_has_avx512() {
            return;
        }
        unsafe {
            compare_cull("avx512", |i, l, o| cull_avx512(i, l, o));
            compare_quality("avx512", |i, o| quality_avx512(i, 32.0, 0.5, 0.15, o));
        }
    }

    #[test]
    fn forced_scalar_ignores_avx() {
        hsn_set_simd_mode(MODE_SCALAR);
        assert_eq!(hsn_active_simd(), MODE_SCALAR);
        hsn_set_simd_mode(MODE_AUTO);
    }
}
