/* AVX-512F kernels. Built only when gcc accepts -mavx512f.
 * Called from Rust after CPUID says the host has AVX-512F.
 */

#include <immintrin.h>
#include <stddef.h>
#include <stdint.h>

static void write_mask8(uint8_t *out, size_t i, uint8_t mask)
{
    out[i] = mask & 1u;
    out[i + 1] = (mask >> 1) & 1u;
    out[i + 2] = (mask >> 2) & 1u;
    out[i + 3] = (mask >> 3) & 1u;
    out[i + 4] = (mask >> 4) & 1u;
    out[i + 5] = (mask >> 5) & 1u;
    out[i + 6] = (mask >> 6) & 1u;
    out[i + 7] = (mask >> 7) & 1u;
}

void hsn_cull_avx512_impl(const double *input, double limit_sq, uint8_t *out, size_t n)
{
    const __m512d vlimit = _mm512_set1_pd(limit_sq);
    size_t i = 0;
    for (; i + 16 <= n; i += 16) {
        const __m512d a = _mm512_loadu_pd(input + i);
        const __m512d b = _mm512_loadu_pd(input + i + 8);
        write_mask8(out, i, (uint8_t)_mm512_cmp_pd_mask(a, vlimit, _CMP_GT_OQ));
        write_mask8(out, i + 8, (uint8_t)_mm512_cmp_pd_mask(b, vlimit, _CMP_GT_OQ));
    }
    for (; i + 8 <= n; i += 8) {
        const __m512d a = _mm512_loadu_pd(input + i);
        write_mask8(out, i, (uint8_t)_mm512_cmp_pd_mask(a, vlimit, _CMP_GT_OQ));
    }
    for (; i < n; i++) {
        out[i] = (uint8_t)(input[i] > limit_sq);
    }
}

void hsn_quality_avx512_impl(const double *input, double max_dist, double start_factor,
                             double min_q, double *out, size_t n)
{
    size_t i = 0;
    if (!(max_dist > 0.0)) {
        for (; i < n; i++) {
            out[i] = 1.0;
        }
        return;
    }
    if (start_factor < 0.15) start_factor = 0.15;
    else if (start_factor > 0.95) start_factor = 0.95;
    if (min_q < 0.05) min_q = 0.05;
    else if (min_q > 1.0) min_q = 1.0;

    const double start_sq = (max_dist * start_factor) * (max_dist * start_factor);
    const double max_sq = max_dist * max_dist;
    const double span = max_sq - start_sq;
    if (!(span > 0.0001)) {
        for (; i < n; i++) {
            out[i] = input[i] <= start_sq ? 1.0 : min_q;
        }
        return;
    }

    const __m512d vstart = _mm512_set1_pd(start_sq);
    const __m512d vmax = _mm512_set1_pd(max_sq);
    const __m512d vminq = _mm512_set1_pd(min_q);
    const __m512d vone = _mm512_set1_pd(1.0);
    const __m512d vzero = _mm512_set1_pd(0.0);
    const __m512d vspan = _mm512_set1_pd(span);
    const __m512d vthree = _mm512_set1_pd(3.0);
    const __m512d vtwo = _mm512_set1_pd(2.0);

    for (; i + 8 <= n; i += 8) {
        const __m512d d = _mm512_loadu_pd(input + i);
        __m512d t = _mm512_div_pd(_mm512_sub_pd(d, vstart), vspan);
        t = _mm512_min_pd(vone, _mm512_max_pd(vzero, t));
        const __m512d s = _mm512_mul_pd(_mm512_mul_pd(t, t),
                                        _mm512_sub_pd(vthree, _mm512_mul_pd(vtwo, t)));
        __m512d q = _mm512_sub_pd(vone, _mm512_mul_pd(s, _mm512_sub_pd(vone, vminq)));
        q = _mm512_min_pd(vone, _mm512_max_pd(vminq, q));
        const __mmask8 far = _mm512_cmp_pd_mask(d, vmax, _CMP_GE_OQ);
        const __mmask8 near = _mm512_cmp_pd_mask(d, vstart, _CMP_LE_OQ);
        q = _mm512_mask_blend_pd(far, q, vminq);
        q = _mm512_mask_blend_pd(near, q, vone);
        _mm512_storeu_pd(out + i, q);
    }
    for (; i < n; i++) {
        const double d = input[i];
        if (!(d > 0.0) || d <= start_sq) {
            out[i] = 1.0;
            continue;
        }
        if (d >= max_sq) {
            out[i] = min_q;
            continue;
        }
        double t = (d - start_sq) / span;
        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;
        t = t * t * (3.0 - 2.0 * t);
        double q = 1.0 - t * (1.0 - min_q);
        if (q < min_q) q = min_q;
        else if (q > 1.0) q = 1.0;
        out[i] = q;
    }
}


void hsn_cull_xyz_avx512_impl(const double *x, const double *y, const double *z,
                              double ox, double oy, double oz, double limit_sq,
                              uint8_t *out, size_t n)
{
    const __m512d vx0 = _mm512_set1_pd(ox);
    const __m512d vy0 = _mm512_set1_pd(oy);
    const __m512d vz0 = _mm512_set1_pd(oz);
    const __m512d vlim = _mm512_set1_pd(limit_sq);
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m512d dx = _mm512_sub_pd(_mm512_loadu_pd(x + i), vx0);
        const __m512d dy = _mm512_sub_pd(_mm512_loadu_pd(y + i), vy0);
        const __m512d dz = _mm512_sub_pd(_mm512_loadu_pd(z + i), vz0);
        const __m512d d2 = _mm512_fmadd_pd(dz, dz, _mm512_fmadd_pd(dy, dy, _mm512_mul_pd(dx, dx)));
        write_mask8(out, i, (uint8_t)_mm512_cmp_pd_mask(d2, vlim, _CMP_GT_OQ));
    }
    for (; i < n; i++) {
        const double dx = x[i] - ox;
        const double dy = y[i] - oy;
        const double dz = z[i] - oz;
        out[i] = (uint8_t)(dx * dx + dy * dy + dz * dz > limit_sq);
    }
}
