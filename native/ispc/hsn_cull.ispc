// HSN ISPC kernels.
// ISPC is a performance language: one program describes a whole SIMD gang.
// `ispc -O3 --target=avx2-i32x8` emits AVX2; avx512skx-i32x16 emits AVX-512.
// Missing ispc is fail-open. Java never requires libhsn_ispc.so.

export uniform int hsn_ispc_abi() {
    return 0x49535043; // 'ISPC'
}

static inline uniform unsigned int pack_drop(varying float dist_sq,
                                             uniform float limit_sq) {
    return dist_sq > limit_sq ? 1u : 0u;
}

export void hsn_ispc_cull_f32(const uniform float in[],
                              uniform float limit_sq,
                              uniform unsigned int8 out[],
                              uniform int n) {
    foreach (i = 0 ... n) {
        out[i] = (unsigned int8) pack_drop(in[i], limit_sq);
    }
}

export void hsn_ispc_cull_f64(const uniform double in[],
                              uniform double limit_sq,
                              uniform unsigned int8 out[],
                              uniform int n) {
    foreach (i = 0 ... n) {
        out[i] = (unsigned int8)(in[i] > limit_sq ? 1 : 0);
    }
}

export void hsn_ispc_dist_sq_f32(const uniform float x[],
                                 const uniform float y[],
                                 const uniform float z[],
                                 uniform float ox,
                                 uniform float oy,
                                 uniform float oz,
                                 uniform float out[],
                                 uniform int n) {
    foreach (i = 0 ... n) {
        varying float dx = x[i] - ox;
        varying float dy = y[i] - oy;
        varying float dz = z[i] - oz;
        out[i] = dx * dx + dy * dy + dz * dz;
    }
}

export void hsn_ispc_cull_xyz_f32(const uniform float x[],
                                  const uniform float y[],
                                  const uniform float z[],
                                  uniform float ox,
                                  uniform float oy,
                                  uniform float oz,
                                  uniform float limit_sq,
                                  uniform unsigned int8 out[],
                                  uniform int n) {
    foreach (i = 0 ... n) {
        varying float dx = x[i] - ox;
        varying float dy = y[i] - oy;
        varying float dz = z[i] - oz;
        out[i] = (unsigned int8)(dx * dx + dy * dy + dz * dz > limit_sq ? 1 : 0);
    }
}

export void hsn_ispc_keep_u8(const uniform float scores[],
                             uniform float keep,
                             uniform unsigned int8 out[],
                             uniform int n) {
    foreach (i = 0 ... n) {
        out[i] = (unsigned int8)(scores[i] <= keep ? 1 : 0);
    }
}

export void hsn_ispc_lod_band_f32(const uniform float dist_sq[],
                                  uniform float t0,
                                  uniform float t1,
                                  uniform float t2,
                                  uniform unsigned int8 out[],
                                  uniform int n) {
    foreach (i = 0 ... n) {
        varying float d = dist_sq[i];
        varying unsigned int8 band = 3;
        if (d <= t0) {
            band = 0;
        } else if (d <= t1) {
            band = 1;
        } else if (d <= t2) {
            band = 2;
        }
        out[i] = band;
    }
}

export void hsn_ispc_rsqrt_f32(const uniform float in[],
                               uniform float out[],
                               uniform int n) {
    foreach (i = 0 ... n) {
        varying float x = in[i];
        if (x <= 0.0f) {
            out[i] = 0.0f;
        } else {
            varying float y = rsqrt(x);
            // One Newton step: y = y * (1.5 - 0.5 * x * y * y)
            y = y * (1.5f - 0.5f * x * y * y);
            out[i] = y;
        }
    }
}

export void hsn_ispc_frustum_aabb_f32(const uniform float planes24[],
                                      const uniform float aabb[],
                                      uniform unsigned int8 out[],
                                      uniform int n) {
    uniform float p[24];
    for (uniform int k = 0; k < 24; k++) {
        p[k] = planes24[k];
    }
    foreach (i = 0 ... n) {
        uniform int base = i * 6;
        varying float minx = aabb[base];
        varying float miny = aabb[base + 1];
        varying float minz = aabb[base + 2];
        varying float maxx = aabb[base + 3];
        varying float maxy = aabb[base + 4];
        varying float maxz = aabb[base + 5];
        varying int drop = 0;
        for (uniform int pl = 0; pl < 6; pl++) {
            uniform float a = p[pl * 4];
            uniform float b = p[pl * 4 + 1];
            uniform float c = p[pl * 4 + 2];
            uniform float d = p[pl * 4 + 3];
            varying float px = a >= 0.0f ? maxx : minx;
            varying float py = b >= 0.0f ? maxy : miny;
            varying float pz = c >= 0.0f ? maxz : minz;
            if (a * px + b * py + c * pz + d < 0.0f) {
                drop = 1;
            }
        }
        out[i] = (unsigned int8) drop;
    }
}

export void hsn_ispc_minmax_f32(const uniform float in[],
                                uniform float out_minmax[],
                                uniform int n) {
    if (n <= 0) {
        return;
    }
    varying float vmin = in[0];
    varying float vmax = in[0];
    foreach (i = 0 ... n) {
        vmin = min(vmin, in[i]);
        vmax = max(vmax, in[i]);
    }
    uniform float lo = reduce_min(vmin);
    uniform float hi = reduce_max(vmax);
    out_minmax[0] = lo;
    out_minmax[1] = hi;
}

export void hsn_ispc_horizon_y_f32(const uniform float max_y[], uniform float floor,
                                   uniform uint8 out[], uniform int64 n) {
    if (n <= 0) {
        return;
    }
    foreach (i = 0 ... n) {
        out[i] = max_y[i] < floor ? 1 : 0;
    }
}
