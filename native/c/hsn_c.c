#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif
#include <math.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
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

#if defined(__BMI2__)
static inline void store4(uint8_t *out, int mask) {
    uint32_t bits = (uint32_t)_pdep_u32((unsigned)mask, 0x01010101u);
    memcpy(out, &bits, 4);
}
#else
__attribute__((target("avx2")))
static inline void store4(uint8_t *out, int mask) {
    out[0] = (uint8_t)(mask & 1);
    out[1] = (uint8_t)((mask >> 1) & 1);
    out[2] = (uint8_t)((mask >> 2) & 1);
    out[3] = (uint8_t)((mask >> 3) & 1);
}
#endif

__attribute__((target("avx2")))
static void cull_avx2(const double *in, double limit_sq, uint8_t *out, size_t n) {
    const __m256d vlim = _mm256_set1_pd(limit_sq);
    size_t i = 0;
    for (; i + 16 <= n; i += 16) {
        store4(out + i,      _mm256_movemask_pd(_mm256_cmp_pd(_mm256_loadu_pd(in + i),      vlim, _CMP_GT_OQ)));
        store4(out + i + 4,  _mm256_movemask_pd(_mm256_cmp_pd(_mm256_loadu_pd(in + i + 4),  vlim, _CMP_GT_OQ)));
        store4(out + i + 8,  _mm256_movemask_pd(_mm256_cmp_pd(_mm256_loadu_pd(in + i + 8),  vlim, _CMP_GT_OQ)));
        store4(out + i + 12, _mm256_movemask_pd(_mm256_cmp_pd(_mm256_loadu_pd(in + i + 12), vlim, _CMP_GT_OQ)));
    }
    for (; i + 8 <= n; i += 8) {
        store4(out + i,     _mm256_movemask_pd(_mm256_cmp_pd(_mm256_loadu_pd(in + i),     vlim, _CMP_GT_OQ)));
        store4(out + i + 4, _mm256_movemask_pd(_mm256_cmp_pd(_mm256_loadu_pd(in + i + 4), vlim, _CMP_GT_OQ)));
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
void hsn_c_cull_f64(const double *restrict in, double limit_sq, uint8_t *restrict out, size_t n) {
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
__attribute__((target("avx2,fma")))
static void rsqrt_avx2(const float *in, float *out, size_t n) {
    const __m256 veps = _mm256_set1_ps(1.0e-12f);
    const __m256 vhalf = _mm256_set1_ps(0.5f);
    const __m256 vthreehalves = _mm256_set1_ps(1.5f);
    const __m256 vzero = _mm256_setzero_ps();
    size_t i = 0;
    for (; i + 8 <= n; i += 8) {
        const __m256 x = _mm256_loadu_ps(in + i);
        const __m256 ok = _mm256_cmp_ps(x, veps, _CMP_GT_OQ);
        const __m256 clamped = _mm256_max_ps(x, veps);
        const __m256 est = _mm256_rsqrt_ps(clamped);
        const __m256 y = _mm256_mul_ps(est, _mm256_sub_ps(vthreehalves,
                _mm256_mul_ps(vhalf, _mm256_mul_ps(clamped, _mm256_mul_ps(est, est)))));
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
void hsn_c_rsqrt_f32(const float *restrict in, float *restrict out, size_t n) {
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

#if defined(__linux__)
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <sys/stat.h>
#include <unistd.h>

static void prefetch_file(const char *path) {
	int fd = open(path, O_RDONLY | O_CLOEXEC);
	if (fd < 0) {
		return;
	}
#if defined(POSIX_FADV_WILLNEED)
	posix_fadvise(fd, 0, 1 << 20, POSIX_FADV_WILLNEED);
#endif
	char buf[8192];
	(void)read(fd, buf, sizeof(buf));
	close(fd);
}

static int prefetch_tree(const char *root, int depth) {
	if (!root || depth > 4) {
		return 0;
	}
	DIR *dir = opendir(root);
	if (!dir) {
		return 0;
	}
	int n = 0;
	char child[1024];
	struct dirent *ent;
	while ((ent = readdir(dir)) != NULL) {
		if (ent->d_name[0] == '.') {
			continue;
		}
		int written = snprintf(child, sizeof(child), "%s/%s", root, ent->d_name);
		if (written < 0 || written >= (int)sizeof(child)) {
			continue;
		}
		struct stat st;
		if (stat(child, &st) != 0) {
			continue;
		}
		if (S_ISDIR(st.st_mode)) {
			n += prefetch_tree(child, depth + 1);
		} else if (S_ISREG(st.st_mode)) {
			prefetch_file(child);
			n++;
			if (n >= 4096) {
				break;
			}
		}
	}
	closedir(dir);
	return n;
}

__attribute__((visibility("default")))
int hsn_boot_prefetch_path(const char *path) {
	if (!path || !path[0]) {
		return 0;
	}
	return prefetch_tree(path, 0);
}

__attribute__((visibility("default")))
int hsn_boot_set_current_priority(int java_prio) {
	if (java_prio < 1) java_prio = 1;
	if (java_prio > 10) java_prio = 10;
	int nicev = (10 - java_prio) * 2; /* 10 -> 0, 1 -> 18 */
	errno = 0;
	int rc = nice(nicev);
	(void)rc;
	pthread_t self = pthread_self();
	struct sched_param sp;
	memset(&sp, 0, sizeof(sp));
	sp.sched_priority = 0;
	pthread_setschedparam(self, SCHED_OTHER, &sp);
	return 1;
}
#else
__attribute__((visibility("default")))
int hsn_boot_prefetch_path(const char *path) {
	(void)path;
	return 0;
}

__attribute__((visibility("default")))
int hsn_boot_set_current_priority(int java_prio) {
	(void)java_prio;
	return 0;
}
#endif

/* Packed Horizon Y: out[i]=1 when maxY[i] < floor. 8-wide AVX2 when present. */
__attribute__((visibility("default")))
void hsn_horizon_y_f32(const float *max_y, float floor, uint8_t *out, size_t n) {
	if (!max_y || !out || n == 0) return;
	size_t i = 0;
#if defined(__x86_64__)
	if (cpu_avx2() && n >= 8) {
		__m256 lim = _mm256_set1_ps(floor);
		for (; i + 8 <= n; i += 8) {
			__m256 v = _mm256_loadu_ps(max_y + i);
			__m256 cmp = _mm256_cmp_ps(v, lim, _CMP_LT_OQ);
			int mask = _mm256_movemask_ps(cmp);
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
