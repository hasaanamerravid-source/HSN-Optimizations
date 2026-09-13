#include <stdint.h>
#include <stddef.h>
#if defined(__x86_64__)
#include <immintrin.h>
#include <cpuid.h>
#endif

#if defined(__x86_64__)
static int cpu_avx2(void) {
	static int cached = -1;
	if (cached < 0) {
		unsigned eax = 0, ebx = 0, ecx = 0, edx = 0;
		cached = (__get_cpuid_count(7, 0, &eax, &ebx, &ecx, &edx) && (ebx & (1u << 5))) ? 1 : 0;
	}
	return cached;
}
#endif

__attribute__((visibility("default")))
void hsn_horizon_y_f32(const float *max_y, float floor, uint8_t *out, size_t n) {
	if (!max_y || !out || n == 0) return;
	size_t i = 0;
#if defined(__x86_64__)
	if (cpu_avx2() && n >= 8) {
		__m256 lim = _mm256_set1_ps(floor);
		for (; i + 8 <= n; i += 8) {
			__m256 v = _mm256_loadu_ps(max_y + i);
			int mask = _mm256_movemask_ps(_mm256_cmp_ps(v, lim, _CMP_LT_OQ));
			out[i]     = (uint8_t)(mask & 1);
			out[i + 1] = (uint8_t)((mask >> 1) & 1);
			out[i + 2] = (uint8_t)((mask >> 2) & 1);
			out[i + 3] = (uint8_t)((mask >> 3) & 1);
			out[i + 4] = (uint8_t)((mask >> 4) & 1);
			out[i + 5] = (uint8_t)((mask >> 5) & 1);
			out[i + 6] = (uint8_t)((mask >> 6) & 1);
			out[i + 7] = (uint8_t)((mask >> 7) & 1);
		}
	}
#endif
	for (; i < n; ++i) out[i] = max_y[i] < floor ? 1u : 0u;
}

static int plane_out(const float *p, int o,
		float minx, float miny, float minz, float maxx, float maxy, float maxz) {
	float px = p[o] >= 0.f ? maxx : minx;
	float py = p[o + 1] >= 0.f ? maxy : miny;
	float pz = p[o + 2] >= 0.f ? maxz : minz;
	return p[o] * px + p[o + 1] * py + p[o + 2] * pz + p[o + 3] < 0.f;
}

/* out[i]=1 when AABB is fully under the bottom frustum plane and below cam_y-keep. */
__attribute__((visibility("default")))
void hsn_horizon_aabb_f32(const float *planes24, const float *aabb,
		float cam_y, float keep, uint8_t *out, size_t n) {
	if (!aabb || !out || n == 0) return;
	float band = cam_y - keep;
	for (size_t i = 0; i < n; ++i) {
		const float *b = aabb + i * 6;
		float maxy = b[4];
		if (maxy >= band) {
			out[i] = 0;
			continue;
		}
		if (planes24 && plane_out(planes24, 16, b[0], b[1], b[2], b[3], b[4], b[5])) {
			out[i] = 1;
		} else if (!planes24 && maxy < band) {
			out[i] = 1;
		} else {
			out[i] = maxy < band ? 1u : 0u;
		}
	}
}

__attribute__((visibility("default")))
int hsn_horizon_abi(void) {
	return 0x48595A31; /* HYZ1 */
}
