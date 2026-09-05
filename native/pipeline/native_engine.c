#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* C ABI in front of simd_math.s plus a zero-alloc frustum/AABB culler.
 * Planes are 6 * 4 floats (a,b,c,d) in view space. Each AABB is 6 floats
 * packed as minx,miny,minz,maxx,maxy,maxz. out[i] = 1 if culled. */

extern void hsn_simd_rsqrt_f32(const float *in, float *out, uint64_t n);

static int plane_rejects(const float *p, float minx, float miny, float minz,
                         float maxx, float maxy, float maxz) {
    const float px = p[0] >= 0.0f ? maxx : minx;
    const float py = p[1] >= 0.0f ? maxy : miny;
    const float pz = p[2] >= 0.0f ? maxz : minz;
    return (p[0] * px + p[1] * py + p[2] * pz + p[3]) < 0.0f;
}

static void cull_aabb_scalar(const float *planes24, const float *aabb6, uint8_t *out, size_t n) {
    for (size_t i = 0; i < n; i++) {
        const float *b = aabb6 + i * 6;
        const float minx = b[0], miny = b[1], minz = b[2];
        const float maxx = b[3], maxy = b[4], maxz = b[5];
        uint8_t drop = 0;
        for (int p = 0; p < 6; p++) {
            if (plane_rejects(planes24 + (size_t)p * 4, minx, miny, minz, maxx, maxy, maxz)) {
                drop = 1;
                break;
            }
        }
        out[i] = drop;
    }
}

__attribute__((visibility("default")))
void hsn_engine_cull_aabb(const float *planes24, const float *aabb6, uint8_t *out, size_t n) {
    if (!planes24 || !aabb6 || !out || n == 0) {
        return;
    }
    cull_aabb_scalar(planes24, aabb6, out, n);
}

/* Sphere vs frustum: center xyz + radius per item (4 floats). out[i]=1 if culled. */
__attribute__((visibility("default")))
void hsn_engine_cull_sphere(const float *planes24, const float *xyzr, uint8_t *out, size_t n) {
    if (!planes24 || !xyzr || !out || n == 0) {
        return;
    }
    for (size_t i = 0; i < n; i++) {
        const float *s = xyzr + i * 4;
        const float x = s[0], y = s[1], z = s[2], r = s[3];
        uint8_t drop = 0;
        for (int p = 0; p < 6; p++) {
            const float *pl = planes24 + (size_t)p * 4;
            const float d = pl[0] * x + pl[1] * y + pl[2] * z + pl[3];
            if (d < -r) {
                drop = 1;
                break;
            }
        }
        out[i] = drop;
    }
}

__attribute__((visibility("default")))
void hsn_engine_rsqrt_f32(const float *in, float *out, size_t n) {
    if (!in || !out || n == 0) {
        return;
    }
    hsn_simd_rsqrt_f32(in, out, (uint64_t)n);
}

__attribute__((visibility("default")))
int hsn_engine_abi(void) {
    return 0x454E474E; /* 'ENGN' */
}

#ifdef __cplusplus
}
#endif
