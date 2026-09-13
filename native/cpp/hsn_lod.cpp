#include <cstddef>
#include <cstdint>
#include <cmath>
#include <algorithm>

// LOD banding, quality curves, and keep-rate helpers in C++.
// Same ABI family as hsn_batch.cpp; linked into libhsn_cpp.so.

namespace {

inline float clampf(float v, float lo, float hi) {
    return v < lo ? lo : (v > hi ? hi : v);
}

inline float smoothstep(float t) {
    t = clampf(t, 0.f, 1.f);
    return t * t * (3.f - 2.f * t);
}

} // namespace

extern "C" {

__attribute__((visibility("default")))
void hsn_cpp_quality_f32(const float* dist_sq, float max_dist, float start_factor,
                         float min_q, float* out, size_t n) {
    if (!dist_sq || !out || n == 0 || !(max_dist > 0.f)) {
        return;
    }
    start_factor = clampf(start_factor, 0.15f, 0.95f);
    min_q = clampf(min_q, 0.05f, 1.f);
    const float start = max_dist * start_factor;
    const float start_sq = start * start;
    const float max_sq = max_dist * max_dist;
    const float span = max_sq - start_sq;
    for (size_t i = 0; i < n; ++i) {
        const float d = dist_sq[i];
        if (d <= start_sq) {
            out[i] = 1.f;
            continue;
        }
        if (d >= max_sq || span <= 1e-6f) {
            out[i] = min_q;
            continue;
        }
        const float t = smoothstep((d - start_sq) / span);
        out[i] = clampf(1.f - t * (1.f - min_q), min_q, 1.f);
    }
}

__attribute__((visibility("default")))
void hsn_cpp_keep_rate_u8(const uint8_t* rnd, uint8_t thresh, uint8_t* out, size_t n) {
    if (!rnd || !out || n == 0) {
        return;
    }
    for (size_t i = 0; i < n; ++i) {
        out[i] = rnd[i] < thresh ? 1 : 0;
    }
}

__attribute__((visibility("default")))
void hsn_cpp_priority_keep(const uint8_t* priority, const uint8_t* rnd,
                           uint8_t high_thresh, uint8_t low_thresh,
                           uint8_t* out, size_t n) {
    if (!priority || !rnd || !out || n == 0) {
        return;
    }
    for (size_t i = 0; i < n; ++i) {
        const uint8_t t = priority[i] != 0 ? high_thresh : low_thresh;
        out[i] = rnd[i] < t ? 1 : 0;
    }
}

__attribute__((visibility("default")))
void hsn_cpp_scale_distances(const float* base, float scale, float floor_scale,
                             float* out, size_t n) {
    if (!base || !out || n == 0) {
        return;
    }
    scale = clampf(scale, floor_scale > 0.f ? floor_scale : 0.05f, 1.f);
    for (size_t i = 0; i < n; ++i) {
        out[i] = base[i] * scale;
    }
}

__attribute__((visibility("default")))
float hsn_cpp_mean_f32(const float* in, size_t n) {
    if (!in || n == 0) {
        return 0.f;
    }
    double acc = 0.0;
    for (size_t i = 0; i < n; ++i) {
        acc += static_cast<double>(in[i]);
    }
    return static_cast<float>(acc / static_cast<double>(n));
}

__attribute__((visibility("default")))
void hsn_cpp_and_or_mask(const uint8_t* a, const uint8_t* b, uint8_t* out_and,
                         uint8_t* out_or, size_t n) {
    if (!a || !b || n == 0) {
        return;
    }
    for (size_t i = 0; i < n; ++i) {
        if (out_and) out_and[i] = static_cast<uint8_t>(a[i] & b[i]);
        if (out_or) out_or[i] = static_cast<uint8_t>(a[i] | b[i]);
    }
}

} // extern "C"
