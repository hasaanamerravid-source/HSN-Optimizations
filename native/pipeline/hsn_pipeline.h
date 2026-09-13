#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void hsn_simd_rsqrt_f32(const float *in, float *out, uint64_t n);
int hsn_simd_abi(void);

void hsn_engine_cull_aabb(const float *planes24, const float *aabb6, uint8_t *out, size_t n);
void hsn_engine_cull_sphere(const float *planes24, const float *xyzr, uint8_t *out, size_t n);
void hsn_engine_rsqrt_f32(const float *in, float *out, size_t n);
int hsn_engine_abi(void);

int hsn_cpp_cacheline_aligned(const void *p);
int hsn_cpp_aligned_to(const void *p, size_t align);
void hsn_cpp_copy_bytes(void *dst, const void *src, size_t n);
void hsn_cpp_prefetch_read(const void *p, size_t n);
void *hsn_cpp_map_anon(size_t n);
int hsn_cpp_unmap(void *p, size_t n);
int hsn_cpp23_abi(void);

void hsn_lod_thresholds(const float *x, const float *y, const float *z,
                        float ox, float oy, float oz,
                        float t0, float t1, float t2,
                        uint8_t *out, size_t n);
void hsn_lod_dist_sq(const float *x, const float *y, const float *z,
                     float ox, float oy, float oz,
                     float *out, size_t n);
int hsn_lod_abi(void);

#ifdef __cplusplus
}
#endif
