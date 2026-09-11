#include <cstddef>
#include <cstdint>
#include <cmath>

#if defined(__x86_64__)
#include <immintrin.h>
#endif

// Extra frustum helpers. Java already has a scalar fallback.
// planes24 = 6 * (a, b, c, d). AABB = n * (minx,miny,minz,maxx,maxy,maxz).

namespace {

bool outside_plane(float a, float b, float c, float d,
                   float minx, float miny, float minz,
                   float maxx, float maxy, float maxz) {
    const float px = a >= 0.f ? maxx : minx;
    const float py = b >= 0.f ? maxy : miny;
    const float pz = c >= 0.f ? maxz : minz;
    return a * px + b * py + c * pz + d < 0.f;
}

}  // namespace

extern "C" {

__attribute__((visibility("default")))
void hsn_cpp_cull_aabb_f32(const float* planes24, const float* aabb,
                           int8_t* out, uint64_t n) {
    if (!planes24 || !aabb || !out || n == 0) {
        return;
    }
    float pl[6][4];
    for (int p = 0; p < 6; ++p) {
        pl[p][0] = planes24[p * 4];
        pl[p][1] = planes24[p * 4 + 1];
        pl[p][2] = planes24[p * 4 + 2];
        pl[p][3] = planes24[p * 4 + 3];
    }
    for (uint64_t i = 0; i < n; ++i) {
        const float* box = aabb + i * 6;
        out[i] = static_cast<int8_t>(
            outside_plane(pl[0][0], pl[0][1], pl[0][2], pl[0][3], box[0], box[1], box[2], box[3], box[4], box[5])
            || outside_plane(pl[1][0], pl[1][1], pl[1][2], pl[1][3], box[0], box[1], box[2], box[3], box[4], box[5])
            || outside_plane(pl[2][0], pl[2][1], pl[2][2], pl[2][3], box[0], box[1], box[2], box[3], box[4], box[5])
            || outside_plane(pl[3][0], pl[3][1], pl[3][2], pl[3][3], box[0], box[1], box[2], box[3], box[4], box[5])
            || outside_plane(pl[4][0], pl[4][1], pl[4][2], pl[4][3], box[0], box[1], box[2], box[3], box[4], box[5])
            || outside_plane(pl[5][0], pl[5][1], pl[5][2], pl[5][3], box[0], box[1], box[2], box[3], box[4], box[5]));
    }
}

__attribute__((visibility("default")))
void hsn_cpp_cull_sphere_f32(const float* planes24, const float* xyzr,
                             int8_t* out, uint64_t n) {
    if (!planes24 || !xyzr || !out || n == 0) {
        return;
    }
    for (uint64_t i = 0; i < n; ++i) {
        const float x = xyzr[i * 4];
        const float y = xyzr[i * 4 + 1];
        const float z = xyzr[i * 4 + 2];
        const float r = xyzr[i * 4 + 3];
        int8_t drop = 0;
        for (int p = 0; p < 6; ++p) {
            const float* pl = planes24 + p * 4;
            const float dist = pl[0] * x + pl[1] * y + pl[2] * z + pl[3];
            if (dist < -r) {
                drop = 1;
                break;
            }
        }
        out[i] = drop;
    }
}

__attribute__((visibility("default")))
void hsn_cpp_extract_planes(const float* viewproj16, float* planes24) {
    if (!viewproj16 || !planes24) {
        return;
    }
    // Column-major view-projection. Planes: left, right, bottom, top, near, far.
    const float* m = viewproj16;
    auto store = [&](int idx, float a, float b, float c, float d) {
        const float inv = 1.f / std::sqrt(std::max(1e-12f, a * a + b * b + c * c));
        planes24[idx * 4]     = a * inv;
        planes24[idx * 4 + 1] = b * inv;
        planes24[idx * 4 + 2] = c * inv;
        planes24[idx * 4 + 3] = d * inv;
    };
    store(0, m[3] + m[0], m[7] + m[4], m[11] + m[8],  m[15] + m[12]);
    store(1, m[3] - m[0], m[7] - m[4], m[11] - m[8],  m[15] - m[12]);
    store(2, m[3] + m[1], m[7] + m[5], m[11] + m[9],  m[15] + m[13]);
    store(3, m[3] - m[1], m[7] - m[5], m[11] - m[9],  m[15] - m[13]);
    store(4, m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14]);
    store(5, m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14]);
}

__attribute__((visibility("default")))
int hsn_cpp_frustum_abi() {
    return 0x46525354;  // "FRST"
}

__attribute__((visibility("default")))
void hsn_cpp_cull_point_planes(const float* planes24, const float* xyz,
                               int8_t* out, uint64_t n) {
    if (!planes24 || !xyz || !out || n == 0) {
        return;
    }
    for (uint64_t i = 0; i < n; ++i) {
        const float x = xyz[i * 3];
        const float y = xyz[i * 3 + 1];
        const float z = xyz[i * 3 + 2];
        int8_t drop = 0;
        for (int p = 0; p < 6; ++p) {
            const float* pl = planes24 + p * 4;
            if (pl[0] * x + pl[1] * y + pl[2] * z + pl[3] < 0.f) {
                drop = 1;
                break;
            }
        }
        out[i] = drop;
    }
}

__attribute__((visibility("default")))
void hsn_cpp_aabb_from_sphere(const float* xyzr, float* aabb6, uint64_t n) {
    if (!xyzr || !aabb6 || n == 0) {
        return;
    }
    for (uint64_t i = 0; i < n; ++i) {
        const float x = xyzr[i * 4];
        const float y = xyzr[i * 4 + 1];
        const float z = xyzr[i * 4 + 2];
        const float r = xyzr[i * 4 + 3];
        float* o = aabb6 + i * 6;
        o[0] = x - r; o[1] = y - r; o[2] = z - r;
        o[3] = x + r; o[4] = y + r; o[5] = z + r;
    }
}

__attribute__((visibility("default")))
int hsn_cpp_any_visible_aabb(const float* planes24, const float* aabb, uint64_t n) {
    if (!planes24 || !aabb || n == 0) {
        return 0;
    }
    for (uint64_t i = 0; i < n; ++i) {
        const float* box = aabb + i * 6;
        bool drop = false;
        for (int p = 0; p < 6; ++p) {
            const float* pl = planes24 + p * 4;
            if (outside_plane(pl[0], pl[1], pl[2], pl[3],
                              box[0], box[1], box[2], box[3], box[4], box[5])) {
                drop = true;
                break;
            }
        }
        if (!drop) {
            return 1;
        }
    }
    return 0;
}

}  // extern "C"
