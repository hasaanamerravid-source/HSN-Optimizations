#include <cstddef>
#include <cstdint>
#include <algorithm>
#include <cmath>

#if defined(__x86_64__)
#include <immintrin.h>
#include <cpuid.h>
#endif

// Structure-of-arrays helpers used by the batch cull path.
// Packed XYZ + optional w (priority). Missing symbols fail-open in Java.

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
void dist_sq_avx2(const double* x, const double* y, const double* z,
                  double ox, double oy, double oz, double* out, size_t n) {
    const __m256d vx0 = _mm256_set1_pd(ox);
    const __m256d vy0 = _mm256_set1_pd(oy);
    const __m256d vz0 = _mm256_set1_pd(oz);
    size_t i = 0;
    for (; i + 4 <= n; i += 4) {
        const __m256d dx = _mm256_sub_pd(_mm256_loadu_pd(x + i), vx0);
        const __m256d dy = _mm256_sub_pd(_mm256_loadu_pd(y + i), vy0);
        const __m256d dz = _mm256_sub_pd(_mm256_loadu_pd(z + i), vz0);
        const __m256d d2 = _mm256_fmadd_pd(dz, dz, _mm256_fmadd_pd(dy, dy, _mm256_mul_pd(dx, dx)));
        _mm256_storeu_pd(out + i, d2);
    }
    for (; i < n; ++i) {
        const double dx = x[i] - ox;
        const double dy = y[i] - oy;
        const double dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
    }
}

__attribute__((target("avx2")))
void scale_mask_avx2(const double* dist_sq, const double* weight,
                     double limit_sq, int8_t* out, size_t n) {
    const __m256d vlim = _mm256_set1_pd(limit_sq);
    size_t i = 0;
    for (; i + 4 <= n; i += 4) {
        const __m256d d = _mm256_loadu_pd(dist_sq + i);
        const __m256d w = _mm256_loadu_pd(weight + i);
        const __m256d adj = _mm256_mul_pd(d, w);
        const int mask = _mm256_movemask_pd(_mm256_cmp_pd(adj, vlim, _CMP_GT_OQ));
        out[i]     = static_cast<int8_t>(mask & 1);
        out[i + 1] = static_cast<int8_t>((mask >> 1) & 1);
        out[i + 2] = static_cast<int8_t>((mask >> 2) & 1);
        out[i + 3] = static_cast<int8_t>((mask >> 3) & 1);
    }
    for (; i < n; ++i) {
        out[i] = static_cast<int8_t>((dist_sq[i] * weight[i]) > limit_sq);
    }
}
#endif

void dist_sq_scalar(const double* x, const double* y, const double* z,
                    double ox, double oy, double oz, double* out, size_t n) {
    for (size_t i = 0; i < n; ++i) {
        const double dx = x[i] - ox;
        const double dy = y[i] - oy;
        const double dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
    }
}

}  // namespace

extern "C" {

__attribute__((visibility("default")))
void hsn_cpp_dist_sq(const double* x, const double* y, const double* z,
                     double ox, double oy, double oz, double* out, uint64_t n) {
    if (!x || !y || !z || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        dist_sq_avx2(x, y, z, ox, oy, oz, out, static_cast<size_t>(n));
        return;
    }
#endif
    dist_sq_scalar(x, y, z, ox, oy, oz, out, static_cast<size_t>(n));
}

__attribute__((visibility("default")))
void hsn_cpp_weighted_mask(const double* dist_sq, const double* weight,
                           double limit_sq, int8_t* out, uint64_t n) {
    if (!dist_sq || !weight || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        scale_mask_avx2(dist_sq, weight, limit_sq, out, static_cast<size_t>(n));
        return;
    }
#endif
    for (uint64_t i = 0; i < n; ++i) {
        out[i] = static_cast<int8_t>((dist_sq[i] * weight[i]) > limit_sq);
    }
}

__attribute__((visibility("default")))
void hsn_cpp_lod_band(const double* dist_sq, double near_sq, double mid_sq,
                      double far_sq, int8_t* out, uint64_t n) {
    if (!dist_sq || !out || n == 0) {
        return;
    }
    for (uint64_t i = 0; i < n; ++i) {
        const double d = dist_sq[i];
        int8_t band = 3;
        if (d <= near_sq) {
            band = 0;
        } else if (d <= mid_sq) {
            band = 1;
        } else if (d <= far_sq) {
            band = 2;
        }
        out[i] = band;
    }
}

__attribute__((visibility("default")))
uint64_t hsn_cpp_count_keep(const int8_t* mask, uint64_t n) {
    if (!mask || n == 0) {
        return 0;
    }
    uint64_t keep = 0;
    for (uint64_t i = 0; i < n; ++i) {
        keep += static_cast<uint8_t>(mask[i] == 0);
    }
    return keep;
}

__attribute__((visibility("default")))
void hsn_cpp_compact_xyz(const double* x, const double* y, const double* z,
                         const int8_t* drop, double* ox, double* oy, double* oz,
                         uint64_t n, uint64_t* kept) {
    if (!x || !y || !z || !drop || !ox || !oy || !oz || !kept) {
        if (kept) {
            *kept = 0;
        }
        return;
    }
    uint64_t w = 0;
    for (uint64_t i = 0; i < n; ++i) {
        if (drop[i] != 0) {
            continue;
        }
        ox[w] = x[i];
        oy[w] = y[i];
        oz[w] = z[i];
        ++w;
    }
    *kept = w;
}

__attribute__((visibility("default")))
int hsn_cpp_soa_abi() {
    return 0x534F4132;  // "SOA2"
}

}  // extern "C"
