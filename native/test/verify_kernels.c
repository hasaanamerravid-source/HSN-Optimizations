#define _GNU_SOURCE
#include <dlfcn.h>
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef void (*cull_fn)(const double *, double, uint8_t *, size_t);
typedef void (*xyz_fn)(const double *, const double *, const double *,
                       double, double, double, double, int8_t *, size_t);
typedef void (*cone_fn)(const double *, const double *,
                        double, double, double, double, int8_t *, size_t);
typedef void (*keep_fn)(const double *, double, uint8_t *, size_t);
typedef void (*quality_fn)(const double *, double, double, double, double *, size_t);
typedef void (*rsqrt_fn)(const float *, float *, size_t);

static int fails;

static void expect_eq_u8(const char *name, const uint8_t *a, const uint8_t *b, size_t n) {
    for (size_t i = 0; i < n; i++) {
        if (a[i] != b[i]) {
            fprintf(stderr, "FAIL %s idx %zu: %u vs %u\n", name, i, a[i], b[i]);
            fails++;
            return;
        }
    }
    printf("ok   %s (%zu)\n", name, n);
}

static void *must_sym(void *h, const char *name) {
    dlerror();
    void *s = dlsym(h, name);
    const char *err = dlerror();
    if (err || !s) {
        fprintf(stderr, "missing symbol %s (%s)\n", name, err ? err : "null");
        fails++;
        return NULL;
    }
    return s;
}

int main(int argc, char **argv) {
    const char *dir = argc > 1 ? argv[1] : "src/main/resources/natives/linux-x86_64";
    char path[512];

    const size_t N = 67;
    double dist[67];
    double x[67], y[67], z[67];
    uint8_t ref[67], got[67];
    for (size_t i = 0; i < N; i++) {
        dist[i] = (double)i * 1.7;
        x[i] = (double)i - 10.0;
        y[i] = (double)(i % 5) * 0.5;
        z[i] = 40.0 - (double)i;
    }
    const double limit = 40.0;

    for (size_t i = 0; i < N; i++) {
        ref[i] = (uint8_t)(dist[i] > limit);
    }

    snprintf(path, sizeof(path), "%s/libhsn_c.so", dir);
    void *hc = dlopen(path, RTLD_NOW);
    if (!hc) {
        fprintf(stderr, "dlopen C failed: %s (%s)\n", path, dlerror());
        return 2;
    }
    cull_fn c_cull = (cull_fn)must_sym(hc, "hsn_c_cull_f64");
    rsqrt_fn rsqrt = (rsqrt_fn)must_sym(hc, "hsn_c_rsqrt_f32");
    if (c_cull) {
        memset(got, 0x5a, N);
        c_cull(dist, limit, got, N);
        expect_eq_u8("c_cull", ref, got, N);
    }
    if (rsqrt) {
        float in[8] = {1.f, 4.f, 16.f, 0.25f, 9.f, 100.f, 0.f, -3.f};
        float out[8];
        rsqrt(in, out, 8);
        float expect[6] = {1.f, 0.5f, 0.25f, 2.f, 1.f / 3.f, 0.1f};
        for (int i = 0; i < 6; i++) {
            float err = fabsf(out[i] - expect[i]);
            if (err > 0.002f) {
                fprintf(stderr, "FAIL rsqrt[%d] %f vs %f\n", i, out[i], expect[i]);
                fails++;
            }
        }
        if (out[6] != 0.f || out[7] != 0.f) {
            fprintf(stderr, "FAIL rsqrt non-positive\n");
            fails++;
        }
        if (fails == 0) {
            printf("ok   c_rsqrt\n");
        }
    }

    snprintf(path, sizeof(path), "%s/libhsn_asm.so", dir);
    void *ha = dlopen(path, RTLD_NOW);
    if (!ha) {
        fprintf(stderr, "dlopen ASM failed: %s (%s)\n", path, dlerror());
        fails++;
    } else {
        cull_fn fn = (cull_fn)must_sym(ha, "hsn_asm_cull_f64");
        if (fn) {
            memset(got, 0x5a, N);
            fn(dist, limit, got, N);
            expect_eq_u8("asm_cull", ref, got, N);
        }
    }

    snprintf(path, sizeof(path), "%s/libhsn_cpp.so", dir);
    void *hp = dlopen(path, RTLD_NOW);
    if (!hp) {
        fprintf(stderr, "dlopen C++ failed: %s (%s)\n", path, dlerror());
        fails++;
    } else {
        xyz_fn xyz = (xyz_fn)must_sym(hp, "hsn_cpp_cull_xyz");
        cone_fn cone = (cone_fn)must_sym(hp, "hsn_cpp_cone_mask");
        if (xyz) {
            uint8_t xref[67], xgot[67];
            for (size_t i = 0; i < N; i++) {
                double dx = x[i] - 1.0, dy = y[i] - 2.0, dz = z[i] - 3.0;
                xref[i] = (uint8_t)((dx * dx + dy * dy + dz * dz) > 250.0);
            }
            memset(xgot, 0x5a, N);
            xyz(x, y, z, 1.0, 2.0, 3.0, 250.0, (int8_t *)xgot, N);
            expect_eq_u8("cpp_xyz", xref, xgot, N);
        }
        if (cone) {
            uint8_t cref[67], cgot[67];
            for (size_t i = 0; i < N; i++) {
                cref[i] = (uint8_t)(((x[i] - 0.0) * 1.0 + (z[i] - 0.0) * 0.0) < 0.0);
            }
            memset(cgot, 0x5a, N);
            cone(x, z, 0.0, 0.0, 1.0, 0.0, (int8_t *)cgot, N);
            expect_eq_u8("cpp_cone", cref, cgot, N);
        }
    }

    snprintf(path, sizeof(path), "%s/libhsn_hotpath.so", dir);
    void *hr = dlopen(path, RTLD_NOW);
    if (!hr) {
        fprintf(stderr, "dlopen rust failed: %s (%s)\n", path, dlerror());
        fails++;
    } else {
        cull_fn cull = (cull_fn)must_sym(hr, "hsn_cull_mask");
        xyz_fn xyz = (xyz_fn)must_sym(hr, "hsn_cull_xyz");
        quality_fn quality = (quality_fn)must_sym(hr, "hsn_quality_batch");
        if (cull) {
            memset(got, 0x5a, N);
            cull(dist, limit, got, N);
            expect_eq_u8("rust_cull", ref, got, N);
        }
        if (xyz) {
            uint8_t xref[67], xgot[67];
            for (size_t i = 0; i < N; i++) {
                double dx = x[i] - 1.0, dy = y[i] - 2.0, dz = z[i] - 3.0;
                xref[i] = (uint8_t)((dx * dx + dy * dy + dz * dz) > 250.0);
            }
            memset(xgot, 0x5a, N);
            xyz(x, y, z, 1.0, 2.0, 3.0, 250.0, (int8_t *)xgot, N);
            expect_eq_u8("rust_xyz", xref, xgot, N);
        }
        if (quality) {
            double q[67];
            quality(dist, 32.0, 0.5, 0.15, q, N);
            int bad = 0;
            for (size_t i = 0; i < N; i++) {
                if (!(q[i] >= 0.15 && q[i] <= 1.0)) {
                    bad = 1;
                }
            }
            if (bad) {
                fprintf(stderr, "FAIL rust_quality range\n");
                fails++;
            } else {
                printf("ok   rust_quality (%zu)\n", N);
            }
        }
    }

    if (fails) {
        fprintf(stderr, "%d failure(s)\n", fails);
        return 1;
    }
    printf("all kernels verified\n");
    return 0;
}
