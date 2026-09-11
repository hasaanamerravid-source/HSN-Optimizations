#include <cstddef>
#include <cstdint>
#include <cmath>

#if defined(__x86_64__)
#include <immintrin.h>
#include <cpuid.h>
#endif

// Extra SIMD math used when packing particle / section batches.

namespace {

#if defined(__x86_64__)
bool cpu_avx2() {
    static const bool yes = []() {
        unsigned eax = 0, ebx = 0, ecx = 0, edx = 0;
        return __get_cpuid_count(7, 0, &eax, &ebx, &ecx, &edx) && (ebx & (1u << 5));
    }();
    return yes;
}

__attribute__((target("avx2")))
void add3_avx2(const float* a, const float* b, float* out, size_t n) {
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        _mm256_storeu_ps(out + i, _mm256_add_ps(_mm256_loadu_ps(a + i), _mm256_loadu_ps(b + i)));
    }
    for (; i < n; ++i) {
        out[i] = a[i] + b[i];
    }
}

__attribute__((target("avx2,fma")))
void madd_avx2(const float* a, const float* b, const float* c, float* out, size_t n) {
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 va = _mm256_loadu_ps(a + i);
        const __m256 vb = _mm256_loadu_ps(b + i);
        const __m256 vc = _mm256_loadu_ps(c + i);
        _mm256_storeu_ps(out + i, _mm256_fmadd_ps(va, vb, vc));
    }
    for (; i < n; ++i) {
        out[i] = a[i] * b[i] + c[i];
    }
}

__attribute__((target("avx2")))
void minmax_avx2(const float* in, float* mn, float* mx, size_t n) {
    if (n == 0) {
        return;
    }
    __m256 vmin = _mm256_set1_ps(in[0]);
    __m256 vmax = vmin;
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 v = _mm256_loadu_ps(in + i);
        vmin = _mm256_min_ps(vmin, v);
        vmax = _mm256_max_ps(vmax, v);
    }
    alignas(32) float lo[8];
    alignas(32) float hi[8];
    _mm256_store_ps(lo, vmin);
    _mm256_store_ps(hi, vmax);
    float mnv = lo[0], mxv = hi[0];
    for (int k = 1; k < 8; ++k) {
        mnv = lo[k] < mnv ? lo[k] : mnv;
        mxv = hi[k] > mxv ? hi[k] : mxv;
    }
    for (; i < n; ++i) {
        mnv = in[i] < mnv ? in[i] : mnv;
        mxv = in[i] > mxv ? in[i] : mxv;
    }
    *mn = mnv;
    *mx = mxv;
}
#endif

}  // namespace

extern "C" {

__attribute__((visibility("default")))
void hsn_cpp_add_f32(const float* a, const float* b, float* out, uint64_t n) {
    if (!a || !b || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        add3_avx2(a, b, out, static_cast<size_t>(n));
        return;
    }
#endif
    for (uint64_t i = 0; i < n; ++i) {
        out[i] = a[i] + b[i];
    }
}

__attribute__((visibility("default")))
void hsn_cpp_madd_f32(const float* a, const float* b, const float* c,
                      float* out, uint64_t n) {
    if (!a || !b || !c || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        madd_avx2(a, b, c, out, static_cast<size_t>(n));
        return;
    }
#endif
    for (uint64_t i = 0; i < n; ++i) {
        out[i] = a[i] * b[i] + c[i];
    }
}

__attribute__((visibility("default")))
void hsn_cpp_minmax_f32(const float* in, float* mn, float* mx, uint64_t n) {
    if (!in || !mn || !mx || n == 0) {
        if (mn) *mn = 0.f;
        if (mx) *mx = 0.f;
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        minmax_avx2(in, mn, mx, static_cast<size_t>(n));
        return;
    }
#endif
    float lo = in[0], hi = in[0];
    for (uint64_t i = 1; i < n; ++i) {
        lo = in[i] < lo ? in[i] : lo;
        hi = in[i] > hi ? in[i] : hi;
    }
    *mn = lo;
    *mx = hi;
}

__attribute__((visibility("default")))
void hsn_cpp_lerp_f32(const float* a, const float* b, float t, float* out, uint64_t n) {
    if (!a || !b || !out || n == 0) {
        return;
    }
    const float u = 1.f - t;
#if defined(__x86_64__)
    if (cpu_avx2()) {
        const __m256 vu = _mm256_set1_ps(u);
        const __m256 vt = _mm256_set1_ps(t);
        size_t i = 0;
        const size_t nn = static_cast<size_t>(n);
        for (; i + 8 <= nn; i += 8) {
            const __m256 va = _mm256_loadu_ps(a + i);
            const __m256 vb = _mm256_loadu_ps(b + i);
            _mm256_storeu_ps(out + i, _mm256_fmadd_ps(vb, vt, _mm256_mul_ps(va, vu)));
        }
        for (; i < nn; ++i) {
            out[i] = a[i] * u + b[i] * t;
        }
        return;
    }
#endif
    for (uint64_t i = 0; i < n; ++i) {
        out[i] = a[i] * u + b[i] * t;
    }
}

__attribute__((visibility("default")))
void hsn_cpp_smoothstep_f64(const double* x, double edge0, double edge1,
                            double* out, uint64_t n) {
    if (!x || !out || n == 0) {
        return;
    }
    const double span = edge1 - edge0;
    if (!(span > 0.0)) {
        for (uint64_t i = 0; i < n; ++i) {
            out[i] = x[i] < edge0 ? 0.0 : 1.0;
        }
        return;
    }
    for (uint64_t i = 0; i < n; ++i) {
        double t = (x[i] - edge0) / span;
        if (t < 0.0) t = 0.0;
        if (t > 1.0) t = 1.0;
        out[i] = t * t * (3.0 - 2.0 * t);
    }
}

}  // extern "C"
