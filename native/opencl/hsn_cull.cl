// HSN OpenCL batch kernels.
// Built only when an OpenCL ICD exists. The Java frame path never waits
// on a GPU queue — this is an optional large-batch sidecar.

#ifndef HSN_WG
#define HSN_WG 64
#endif

__kernel void hsn_cl_cull_f32(__global const float* in,
                              const float limit_sq,
                              __global uchar* out,
                              const uint n) {
    const uint i = get_global_id(0);
    if (i >= n) return;
    out[i] = in[i] > limit_sq ? (uchar)1 : (uchar)0;
}

__kernel void hsn_cl_dist_sq_f32(__global const float* x,
                                 __global const float* y,
                                 __global const float* z,
                                 const float ox,
                                 const float oy,
                                 const float oz,
                                 __global float* out,
                                 const uint n) {
    const uint i = get_global_id(0);
    if (i >= n) return;
    const float dx = x[i] - ox;
    const float dy = y[i] - oy;
    const float dz = z[i] - oz;
    out[i] = dx * dx + dy * dy + dz * dz;
}

__kernel void hsn_cl_cull_xyz_f32(__global const float* x,
                                  __global const float* y,
                                  __global const float* z,
                                  const float ox,
                                  const float oy,
                                  const float oz,
                                  const float limit_sq,
                                  __global uchar* out,
                                  const uint n) {
    const uint i = get_global_id(0);
    if (i >= n) return;
    const float dx = x[i] - ox;
    const float dy = y[i] - oy;
    const float dz = z[i] - oz;
    out[i] = (dx * dx + dy * dy + dz * dz) > limit_sq ? (uchar)1 : (uchar)0;
}

__kernel void hsn_cl_rsqrt_f32(__global const float* in,
                               __global float* out,
                               const uint n) {
    const uint i = get_global_id(0);
    if (i >= n) return;
    const float v = in[i];
    if (v <= 0.0f) {
        out[i] = 0.0f;
        return;
    }
    float y = rsqrt(v);
    y = y * (1.5f - 0.5f * v * y * y);
    out[i] = y;
}

__kernel void hsn_cl_lod_band_f32(__global const float* dist_sq,
                                  const float t0,
                                  const float t1,
                                  const float t2,
                                  __global uchar* out,
                                  const uint n) {
    const uint i = get_global_id(0);
    if (i >= n) return;
    const float d = dist_sq[i];
    uchar band = 3;
    if (d <= t0) band = 0;
    else if (d <= t1) band = 1;
    else if (d <= t2) band = 2;
    out[i] = band;
}

__kernel void hsn_cl_keep_u8(__global const float* scores,
                             const float keep,
                             __global uchar* out,
                             const uint n) {
    const uint i = get_global_id(0);
    if (i >= n) return;
    out[i] = scores[i] <= keep ? (uchar)1 : (uchar)0;
}
