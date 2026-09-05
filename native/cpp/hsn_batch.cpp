#include <cstddef>
#include <cstdint>
#include <cmath>

#if defined(__x86_64__)
#include <immintrin.h>
#include <cpuid.h>
#endif

// C++ hot path: packed XYZ distance + forward-cone tests.
// Called from Java via Panama in batches of 16+ points (chunk sections, entities).
// AVX2 kernels are selected at runtime; scalar is always legal.

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

__attribute__((target("avx2")))
void cull_xyz_avx2(const double* x, const double* y, const double* z,
                   double ox, double oy, double oz, double limit_sq,
                   int8_t* out, size_t n) {
    const __m256d vx0 = _mm256_set1_pd(ox);
    const __m256d vy0 = _mm256_set1_pd(oy);
    const __m256d vz0 = _mm256_set1_pd(oz);
    const __m256d vlim = _mm256_set1_pd(limit_sq);
    size_t i = 0;
    for (; i + 4 <= n; i += 4) {
        const __m256d dx = _mm256_sub_pd(_mm256_loadu_pd(x + i), vx0);
        const __m256d dy = _mm256_sub_pd(_mm256_loadu_pd(y + i), vy0);
        const __m256d dz = _mm256_sub_pd(_mm256_loadu_pd(z + i), vz0);
        const __m256d d2 = _mm256_add_pd(
            _mm256_add_pd(_mm256_mul_pd(dx, dx), _mm256_mul_pd(dy, dy)),
            _mm256_mul_pd(dz, dz));
        const int mask = _mm256_movemask_pd(_mm256_cmp_pd(d2, vlim, _CMP_GT_OQ));
        out[i]     = static_cast<int8_t>(mask & 1);
        out[i + 1] = static_cast<int8_t>((mask >> 1) & 1);
        out[i + 2] = static_cast<int8_t>((mask >> 2) & 1);
        out[i + 3] = static_cast<int8_t>((mask >> 3) & 1);
    }
    for (; i < n; ++i) {
        const double dx = x[i] - ox;
        const double dy = y[i] - oy;
        const double dz = z[i] - oz;
        out[i] = static_cast<int8_t>((dx * dx + dy * dy + dz * dz) > limit_sq);
    }
}

__attribute__((target("avx2")))
void cone_mask_avx2(const double* x, const double* z,
                    double ox, double oz, double fx, double fz,
                    int8_t* out, size_t n) {
    const __m256d vx0 = _mm256_set1_pd(ox);
    const __m256d vz0 = _mm256_set1_pd(oz);
    const __m256d vfx = _mm256_set1_pd(fx);
    const __m256d vfz = _mm256_set1_pd(fz);
    const __m256d vzero = _mm256_setzero_pd();
    size_t i = 0;
    for (; i + 4 <= n; i += 4) {
        const __m256d dx = _mm256_sub_pd(_mm256_loadu_pd(x + i), vx0);
        const __m256d dz = _mm256_sub_pd(_mm256_loadu_pd(z + i), vz0);
        const __m256d dot = _mm256_add_pd(_mm256_mul_pd(dx, vfx), _mm256_mul_pd(dz, vfz));
        const int mask = _mm256_movemask_pd(_mm256_cmp_pd(dot, vzero, _CMP_LT_OQ));
        out[i]     = static_cast<int8_t>(mask & 1);
        out[i + 1] = static_cast<int8_t>((mask >> 1) & 1);
        out[i + 2] = static_cast<int8_t>((mask >> 2) & 1);
        out[i + 3] = static_cast<int8_t>((mask >> 3) & 1);
    }
    for (; i < n; ++i) {
        const double dx = x[i] - ox;
        const double dz = z[i] - oz;
        out[i] = static_cast<int8_t>((dx * fx + dz * fz) < 0.0);
    }
}
#endif

void cull_xyz_scalar(const double* x, const double* y, const double* z,
                     double ox, double oy, double oz, double limit_sq,
                     int8_t* out, size_t n) {
    size_t i = 0;
    const size_t bound = n & ~size_t{3};
    for (; i < bound; i += 4) {
        const double dx0 = x[i]     - ox;
        const double dy0 = y[i]     - oy;
        const double dz0 = z[i]     - oz;
        const double dx1 = x[i + 1] - ox;
        const double dy1 = y[i + 1] - oy;
        const double dz1 = z[i + 1] - oz;
        const double dx2 = x[i + 2] - ox;
        const double dy2 = y[i + 2] - oy;
        const double dz2 = z[i + 2] - oz;
        const double dx3 = x[i + 3] - ox;
        const double dy3 = y[i + 3] - oy;
        const double dz3 = z[i + 3] - oz;
        out[i]     = static_cast<int8_t>((dx0 * dx0 + dy0 * dy0 + dz0 * dz0) > limit_sq);
        out[i + 1] = static_cast<int8_t>((dx1 * dx1 + dy1 * dy1 + dz1 * dz1) > limit_sq);
        out[i + 2] = static_cast<int8_t>((dx2 * dx2 + dy2 * dy2 + dz2 * dz2) > limit_sq);
        out[i + 3] = static_cast<int8_t>((dx3 * dx3 + dy3 * dy3 + dz3 * dz3) > limit_sq);
    }
    for (; i < n; ++i) {
        const double dx = x[i] - ox;
        const double dy = y[i] - oy;
        const double dz = z[i] - oz;
        out[i] = static_cast<int8_t>((dx * dx + dy * dy + dz * dz) > limit_sq);
    }
}

void cone_mask_scalar(const double* x, const double* z,
                      double ox, double oz, double fx, double fz,
                      int8_t* out, size_t n) {
    for (size_t i = 0; i < n; ++i) {
        const double dx = x[i] - ox;
        const double dz = z[i] - oz;
        out[i] = static_cast<int8_t>((dx * fx + dz * fz) < 0.0);
    }
}

} // namespace

extern "C" {

__attribute__((visibility("default")))
void hsn_cpp_cull_xyz(const double* x, const double* y, const double* z,
                      double ox, double oy, double oz, double limit_sq,
                      int8_t* out, size_t n) {
    if (!x || !y || !z || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        cull_xyz_avx2(x, y, z, ox, oy, oz, limit_sq, out, n);
        return;
    }
#endif
    cull_xyz_scalar(x, y, z, ox, oy, oz, limit_sq, out, n);
}

__attribute__((visibility("default")))
void hsn_cpp_cone_mask(const double* x, const double* z,
                       double ox, double oz,
                       double fx, double fz,
                       int8_t* out, size_t n) {
    if (!x || !z || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        cone_mask_avx2(x, z, ox, oz, fx, fz, out, n);
        return;
    }
#endif
    cone_mask_scalar(x, z, ox, oz, fx, fz, out, n);
}

__attribute__((visibility("default")))
int hsn_cpp_abi() {
    return 0x43322B2B; // 'C++'
}

} // extern "C"
