#include <math.h>
#include <stddef.h>
#include <stdint.h>
#include <string.h>

#if defined(__x86_64__)
#include <immintrin.h>
#include <cpuid.h>
#endif

/* Zero-cost C ABI wrappers. Java Panama calls these with heap arrays
 * (Linker.Option.critical). No allocation, no TLS, no runtime. */

#if defined(__x86_64__)
static int cpu_avx2(void) {
    static int cached = -1;
    if (cached < 0) {
        unsigned eax = 0, ebx = 0, ecx = 0, edx = 0;
        cached = (__get_cpuid_count(7, 0, &eax, &ebx, &ecx, &edx) && (ebx & (1u << 5))) ? 1 : 0;
    }
    return cached;
}

__attribute__((target("avx2")))
static void cull_avx2(const double *in, double limit_sq, uint8_t *out, size_t n) {
    const __m256d vlim = _mm256_set1_pd(limit_sq);
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256d a = _mm256_loadu_pd(in + i);
        const __m256d b = _mm256_loadu_pd(in + i + 4);
        const int ma = _mm256_movemask_pd(_mm256_cmp_pd(a, vlim, _CMP_GT_OQ));
        const int mb = _mm256_movemask_pd(_mm256_cmp_pd(b, vlim, _CMP_GT_OQ));
        out[i]     = (uint8_t)(ma & 1);
        out[i + 1] = (uint8_t)((ma >> 1) & 1);
        out[i + 2] = (uint8_t)((ma >> 2) & 1);
        out[i + 3] = (uint8_t)((ma >> 3) & 1);
        out[i + 4] = (uint8_t)(mb & 1);
        out[i + 5] = (uint8_t)((mb >> 1) & 1);
        out[i + 6] = (uint8_t)((mb >> 2) & 1);
        out[i + 7] = (uint8_t)((mb >> 3) & 1);
    }
    for (; i < n; i++) {
        out[i] = (uint8_t)(in[i] > limit_sq);
    }
}
#endif

static void cull_scalar(const double *in, double limit_sq, uint8_t *out, size_t n) {
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        out[i]     = (uint8_t)(in[i]     > limit_sq);
        out[i + 1] = (uint8_t)(in[i + 1] > limit_sq);
        out[i + 2] = (uint8_t)(in[i + 2] > limit_sq);
        out[i + 3] = (uint8_t)(in[i + 3] > limit_sq);
        out[i + 4] = (uint8_t)(in[i + 4] > limit_sq);
        out[i + 5] = (uint8_t)(in[i + 5] > limit_sq);
        out[i + 6] = (uint8_t)(in[i + 6] > limit_sq);
        out[i + 7] = (uint8_t)(in[i + 7] > limit_sq);
    }
    for (; i < n; i++) {
        out[i] = (uint8_t)(in[i] > limit_sq);
    }
}

__attribute__((visibility("default")))
void hsn_c_cull_f64(const double *in, double limit_sq, uint8_t *out, size_t n) {
    if (!in || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        cull_avx2(in, limit_sq, out, n);
        return;
    }
#endif
    cull_scalar(in, limit_sq, out, n);
}

#if defined(__x86_64__)
__attribute__((target("avx2")))
static void rsqrt_avx2(const float *in, float *out, size_t n) {
    const __m256 veps = _mm256_set1_ps(1.0e-12f);
    const __m256 vone = _mm256_set1_ps(1.0f);
    const __m256 vzero = _mm256_setzero_ps();
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 x = _mm256_loadu_ps(in + i);
        const __m256 ok = _mm256_cmp_ps(x, veps, _CMP_GT_OQ);
        const __m256 clamped = _mm256_max_ps(x, veps);
        const __m256 y = _mm256_div_ps(vone, _mm256_sqrt_ps(clamped));
        _mm256_storeu_ps(out + i, _mm256_blendv_ps(vzero, y, ok));
    }
    for (; i < n; i++) {
        float x = in[i];
        out[i] = (x > 1.0e-12f) ? (1.0f / sqrtf(x)) : 0.0f;
    }
}
#endif

static void rsqrt_scalar(const float *in, float *out, size_t n) {
    for (size_t i = 0; i < n; i++) {
        float x = in[i];
        if (!(x > 1.0e-12f)) {
            out[i] = 0.0f;
            continue;
        }
        float xhalf = 0.5f * x;
        uint32_t bits;
        memcpy(&bits, &x, sizeof(bits));
        bits = 0x5f3759dfu - (bits >> 1);
        float y;
        memcpy(&y, &bits, sizeof(y));
        y = y * (1.5f - xhalf * y * y);
        y = y * (1.5f - xhalf * y * y);
        out[i] = y;
    }
}

__attribute__((visibility("default")))
void hsn_c_rsqrt_f32(const float *in, float *out, size_t n) {
    if (!in || !out || n == 0) {
        return;
    }
#if defined(__x86_64__)
    if (cpu_avx2()) {
        rsqrt_avx2(in, out, n);
        return;
    }
#endif
    rsqrt_scalar(in, out, n);
}

__attribute__((visibility("default")))
int hsn_c_abi(void) {
    return 0x435f4142; /* 'C_AB' */
}
