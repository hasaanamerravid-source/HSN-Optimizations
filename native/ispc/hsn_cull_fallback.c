/* Scalar stand-in used when `ispc` is not installed.
 * Same exported names as hsn_cull.ispc so Java can bind either .so.
 */
#include <stdint.h>
#include <math.h>

int hsn_ispc_abi(void) { return 0x49535043; }

void hsn_ispc_cull_f32(const float* in, float limit_sq, uint8_t* out, int n) {
    if (!in || !out || n <= 0) return;
    int i = 0;
    for (; i + 8 <= n; i += 8) {
        out[i]     = in[i]     > limit_sq ? 1u : 0u;
        out[i + 1] = in[i + 1] > limit_sq ? 1u : 0u;
        out[i + 2] = in[i + 2] > limit_sq ? 1u : 0u;
        out[i + 3] = in[i + 3] > limit_sq ? 1u : 0u;
        out[i + 4] = in[i + 4] > limit_sq ? 1u : 0u;
        out[i + 5] = in[i + 5] > limit_sq ? 1u : 0u;
        out[i + 6] = in[i + 6] > limit_sq ? 1u : 0u;
        out[i + 7] = in[i + 7] > limit_sq ? 1u : 0u;
    }
    for (; i < n; ++i) out[i] = in[i] > limit_sq ? 1u : 0u;
}

void hsn_ispc_cull_f64(const double* in, double limit_sq, uint8_t* out, int n) {
    if (!in || !out || n <= 0) return;
    int i = 0;
    for (; i + 8 <= n; i += 8) {
        out[i]     = in[i]     > limit_sq ? 1u : 0u;
        out[i + 1] = in[i + 1] > limit_sq ? 1u : 0u;
        out[i + 2] = in[i + 2] > limit_sq ? 1u : 0u;
        out[i + 3] = in[i + 3] > limit_sq ? 1u : 0u;
        out[i + 4] = in[i + 4] > limit_sq ? 1u : 0u;
        out[i + 5] = in[i + 5] > limit_sq ? 1u : 0u;
        out[i + 6] = in[i + 6] > limit_sq ? 1u : 0u;
        out[i + 7] = in[i + 7] > limit_sq ? 1u : 0u;
    }
    for (; i < n; ++i) out[i] = in[i] > limit_sq ? 1u : 0u;
}

void hsn_ispc_dist_sq_f32(const float* x, const float* y, const float* z,
                          float ox, float oy, float oz, float* out, int n) {
    if (!x || !y || !z || !out || n <= 0) return;
    for (int i = 0; i < n; ++i) {
        float dx = x[i] - ox, dy = y[i] - oy, dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
    }
}

void hsn_ispc_cull_xyz_f32(const float* x, const float* y, const float* z,
                           float ox, float oy, float oz, float limit_sq,
                           uint8_t* out, int n) {
    if (!x || !y || !z || !out || n <= 0) return;
    for (int i = 0; i < n; ++i) {
        float dx = x[i] - ox, dy = y[i] - oy, dz = z[i] - oz;
        out[i] = (dx * dx + dy * dy + dz * dz) > limit_sq ? 1u : 0u;
    }
}

void hsn_ispc_keep_u8(const float* scores, float keep, uint8_t* out, int n) {
    if (!scores || !out || n <= 0) return;
    for (int i = 0; i < n; ++i) out[i] = scores[i] <= keep ? 1u : 0u;
}

void hsn_ispc_lod_band_f32(const float* dist_sq, float t0, float t1, float t2,
                           uint8_t* out, int n) {
    if (!dist_sq || !out || n <= 0) return;
    for (int i = 0; i < n; ++i) {
        float d = dist_sq[i];
        out[i] = d <= t0 ? 0u : d <= t1 ? 1u : d <= t2 ? 2u : 3u;
    }
}

void hsn_ispc_horizon_y_f32(const float* max_y, float floor, uint8_t* out, long n) {
    if (!max_y || !out || n <= 0) return;
    long i = 0;
    for (; i + 8 <= n; i += 8) {
        out[i]     = max_y[i]     < floor ? 1u : 0u;
        out[i + 1] = max_y[i + 1] < floor ? 1u : 0u;
        out[i + 2] = max_y[i + 2] < floor ? 1u : 0u;
        out[i + 3] = max_y[i + 3] < floor ? 1u : 0u;
        out[i + 4] = max_y[i + 4] < floor ? 1u : 0u;
        out[i + 5] = max_y[i + 5] < floor ? 1u : 0u;
        out[i + 6] = max_y[i + 6] < floor ? 1u : 0u;
        out[i + 7] = max_y[i + 7] < floor ? 1u : 0u;
    }
    for (; i < n; ++i) out[i] = max_y[i] < floor ? 1u : 0u;
}

void hsn_ispc_rsqrt_f32(const float* in, float* out, int n) {
    if (!in || !out || n <= 0) return;
    for (int i = 0; i < n; ++i) {
        float x = in[i];
        if (!(x > 0.f)) { out[i] = 0.f; continue; }
        float y = 1.f / sqrtf(x);
        out[i] = y * (1.5f - 0.5f * x * y * y);
    }
}

void hsn_ispc_frustum_aabb_f32(const float* planes24, const float* aabb,
                               uint8_t* out, int n) {
    if (!planes24 || !aabb || !out || n <= 0) return;
    for (int i = 0; i < n; ++i) {
        const float* box = aabb + i * 6;
        uint8_t drop = 0;
        for (int p = 0; p < 6; ++p) {
            float a = planes24[p * 4], b = planes24[p * 4 + 1];
            float c = planes24[p * 4 + 2], d = planes24[p * 4 + 3];
            float px = a >= 0.f ? box[3] : box[0];
            float py = b >= 0.f ? box[4] : box[1];
            float pz = c >= 0.f ? box[5] : box[2];
            if (a * px + b * py + c * pz + d < 0.f) { drop = 1; break; }
        }
        out[i] = drop;
    }
}

void hsn_ispc_minmax_f32(const float* in, float* out_minmax, int n) {
    if (!in || !out_minmax || n <= 0) return;
    float lo = in[0], hi = in[0];
    for (int i = 1; i < n; ++i) {
        if (in[i] < lo) lo = in[i];
        if (in[i] > hi) hi = in[i];
    }
    out_minmax[0] = lo;
    out_minmax[1] = hi;
}
