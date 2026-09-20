//! hsn_hot_* FFI exports for frustum culling.
//!
//! All logic lives in frustum.rs. This file is thin wrappers only —
//! no duplicated implementation.

use crate::frustum::{cull_aabb, cull_sphere, extract_planes};

unsafe fn slice_f32<'a>(ptr: *const f32, n: usize) -> &'a [f32] {
    if ptr.is_null() || n == 0 { &[] } else { unsafe { std::slice::from_raw_parts(ptr, n) } }
}

unsafe fn slice_f32_mut<'a>(ptr: *mut f32, n: usize) -> &'a mut [f32] {
    if ptr.is_null() || n == 0 { &mut [] } else { unsafe { std::slice::from_raw_parts_mut(ptr, n) } }
}

unsafe fn slice_u8_mut<'a>(ptr: *mut u8, n: usize) -> &'a mut [u8] {
    if ptr.is_null() || n == 0 { &mut [] } else { unsafe { std::slice::from_raw_parts_mut(ptr, n) } }
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_cull_aabb_f32(
    planes: *const f32,
    aabb: *const f32,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    cull_aabb(slice_f32(planes, 24), slice_f32(aabb, len * 6), slice_u8_mut(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_cull_sphere_f32(
    planes: *const f32,
    xyzr: *const f32,
    out: *mut u8,
    n: u64,
) {
    let len = n as usize;
    cull_sphere(slice_f32(planes, 24), slice_f32(xyzr, len * 4), slice_u8_mut(out, len));
}

#[no_mangle]
pub unsafe extern "C" fn hsn_hot_extract_planes(viewproj: *const f32, planes: *mut f32) {
    extract_planes(slice_f32(viewproj, 16), slice_f32_mut(planes, 24));
}
