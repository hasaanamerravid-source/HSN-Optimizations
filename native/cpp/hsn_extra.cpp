#include <cstddef>
#include <cstdint>
#include <cmath>
#include <algorithm>

#if defined(__x86_64__)
#include <immintrin.h>
#include <cpuid.h>
#endif

// Extra C++ SoA / LOD / Morton helpers. Linked into libhsn_cpp.so.
// Fail-open: Java never requires these symbols on the per-entity path.

namespace {

#if defined(__x86_64__)
bool cpu_avx2() {
    static const bool yes = []() {
        unsigned eax = 0, ebx = 0, ecx = 0, edx = 0;
        if (!__get_cpuid_count(7, 0, &eax, &ebx, &ecx, &edx)) {
            return false;
        }
        return (ebx & (1u << 5)) != 0u;
    }();
    return yes;
}

__attribute__((target("avx2,fma")))
void dist_sq_avx2(const float* x, const float* y, const float* z,
                  float ox, float oy, float oz, float* out, size_t n) {
    const __m256 vx = _mm256_set1_ps(ox);
    const __m256 vy = _mm256_set1_ps(oy);
    const __m256 vz = _mm256_set1_ps(oz);
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 dx = _mm256_sub_ps(_mm256_loadu_ps(x + i), vx);
        const __m256 dy = _mm256_sub_ps(_mm256_loadu_ps(y + i), vy);
        const __m256 dz = _mm256_sub_ps(_mm256_loadu_ps(z + i), vz);
        const __m256 s = _mm256_fmadd_ps(dz, dz, _mm256_fmadd_ps(dy, dy, _mm256_mul_ps(dx, dx)));
        _mm256_storeu_ps(out + i, s);
    }
    for (; i < n; ++i) {
        const float dx = x[i] - ox;
        const float dy = y[i] - oy;
        const float dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
    }
}

__attribute__((target("avx2")))
void cull_f32_avx2(const float* in, float limit_sq, int8_t* out, size_t n) {
    const __m256 lim = _mm256_set1_ps(limit_sq);
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 v = _mm256_loadu_ps(in + i);
        const int mask = _mm256_movemask_ps(_mm256_cmp_ps(v, lim, _CMP_GT_OQ));
        for (int lane = 0; lane < 8; ++lane) {
            out[i + static_cast<size_t>(lane)] = static_cast<int8_t>((mask >> lane) & 1);
        }
    }
    for (; i < n; ++i) {
        out[i] = static_cast<int8_t>(in[i] > limit_sq);
    }
}
#endif

inline uint32_t part1by2(uint32_t n) {
    n &= 0x000003ffu;
    n = (n | (n << 16)) & 0x030000FFu;
    n = (n | (n << 8)) & 0x0300F00Fu;
    n = (n | (n << 4)) & 0x030C30C3u;
    n = (n | (n << 2)) & 0x09249249u;
    return n;
}

} // namespace

extern "C" {

__attribute__((visibility("default")))
void hsn_cpp_dist_sq_f32(const float* x, const float* y, const float* z,
                         float ox, float oy, float oz, float* out, size_t n) {
    if (!x || !y || !z || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        dist_sq_avx2(x, y, z, ox, oy, oz, out, n);
        return;
    }
#endif
    for (size_t i = 0; i < n; ++i) {
        const float dx = x[i] - ox;
        const float dy = y[i] - oy;
        const float dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
    }
}

__attribute__((visibility("default")))
void hsn_cpp_cull_f32(const float* in, float limit_sq, int8_t* out, size_t n) {
    if (!in || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        cull_f32_avx2(in, limit_sq, out, n);
        return;
    }
#endif
    for (size_t i = 0; i < n; ++i) {
        out[i] = static_cast<int8_t>(in[i] > limit_sq);
    }
}

__attribute__((visibility("default")))
void hsn_cpp_lod_band_f32(const float* dist_sq, float near_sq, float mid_sq,
                          uint8_t* out, size_t n) {
    if (!dist_sq || !out || n == 0) {
        return;
    }
    for (size_t i = 0; i < n; ++i) {
        const float d = dist_sq[i];
        out[i] = static_cast<uint8_t>(d < near_sq ? 0 : (d < mid_sq ? 1 : 2));
    }
}

__attribute__((visibility("default")))
void hsn_cpp_morton3(const int32_t* x, const int32_t* y, const int32_t* z,
                     uint32_t* out, size_t n) {
    if (!x || !y || !z || !out || n == 0) {
        return;
    }
    for (size_t i = 0; i < n; ++i) {
        const uint32_t xi = static_cast<uint32_t>(x[i]) & 0x3ffu;
        const uint32_t yi = static_cast<uint32_t>(y[i]) & 0x3ffu;
        const uint32_t zi = static_cast<uint32_t>(z[i]) & 0x3ffu;
        out[i] = part1by2(xi) | (part1by2(yi) << 1) | (part1by2(zi) << 2);
    }
}

__attribute__((visibility("default")))
void hsn_cpp_aabb_merge(const float* aabb, size_t n, float* out6) {
    if (!aabb || !out6 || n == 0) {
        return;
    }
    float minx = aabb[0], miny = aabb[1], minz = aabb[2];
    float maxx = aabb[3], maxy = aabb[4], maxz = aabb[5];
    for (size_t i = 1; i < n; ++i) {
        const float* b = aabb + i * 6;
        minx = std::min(minx, b[0]);
        miny = std::min(miny, b[1]);
        minz = std::min(minz, b[2]);
        maxx = std::max(maxx, b[3]);
        maxy = std::max(maxy, b[4]);
        maxz = std::max(maxz, b[5]);
    }
    out6[0] = minx; out6[1] = miny; out6[2] = minz;
    out6[3] = maxx; out6[4] = maxy; out6[5] = maxz;
}

#if defined(__x86_64__)
__attribute__((target("avx2")))
static void rsqrt_avx2(const float* in, float* out, size_t n) {
    const __m256 veps = _mm256_set1_ps(1.0e-12f);
    const __m256 vhalf = _mm256_set1_ps(0.5f);
    const __m256 vth = _mm256_set1_ps(1.5f);
    const __m256 vz = _mm256_setzero_ps();
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 x = _mm256_loadu_ps(in + i);
        const __m256 ok = _mm256_cmp_ps(x, veps, _CMP_GT_OQ);
        const __m256 c = _mm256_max_ps(x, veps);
        const __m256 e = _mm256_rsqrt_ps(c);
        const __m256 y = _mm256_mul_ps(e, _mm256_sub_ps(vth,
            _mm256_mul_ps(vhalf, _mm256_mul_ps(c, _mm256_mul_ps(e, e)))));
        _mm256_storeu_ps(out + i, _mm256_blendv_ps(vz, y, ok));
    }
    for (; i < n; ++i) {
        const float v = in[i];
        out[i] = (v > 1.0e-12f) ? (1.0f / std::sqrt(v)) : 0.0f;
    }
}
#endif

__attribute__((visibility("default")))
void hsn_cpp_rsqrt_f32(const float* in, float* out, size_t n) {
    if (!in || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        rsqrt_avx2(in, out, n);
        return;
    }
#endif
    for (size_t i = 0; i < n; ++i) {
        const float v = in[i];
        if (!(v > 0.0f)) {
            out[i] = 0.0f;
            continue;
        }
        float y = 1.0f / std::sqrt(v);
        y = y * (1.5f - 0.5f * v * y * y);
        out[i] = y;
    }
}

__attribute__((visibility("default")))
int hsn_cpp_popcount_u8(const uint8_t* in, size_t n) {
    if (!in || n == 0) {
        return 0;
    }
    int acc = 0;
    for (size_t i = 0; i < n; ++i) {
        acc += in[i] != 0 ? 1 : 0;
    }
    return acc;
}

} // extern "C"
