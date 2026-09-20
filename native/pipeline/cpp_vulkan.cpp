#include <cstddef>
#include <cstdint>
#include <cstring>

#if defined(__linux__)
#include <sys/mman.h>
#include <unistd.h>
#endif

#if defined(__x86_64__)
#include <immintrin.h>
#endif

/* C++23 off-heap helpers. Named for the compute pipeline slot; no Vulkan
 * device is created (Sodium already owns the GPU queue). */

namespace {

constexpr std::size_t kCacheLine = 64;

[[nodiscard]] bool aligned_to(const void *p, std::size_t align) noexcept {
    if (p == nullptr || align == 0 || (align & (align - 1)) != 0) {
        return false;
    }
    return (reinterpret_cast<std::uintptr_t>(p) & (align - 1)) == 0;
}

} // namespace

extern "C" {

__attribute__((visibility("default")))
int hsn_cpp_cacheline_aligned(const void *p) {
    return aligned_to(p, kCacheLine) ? 1 : 0;
}

__attribute__((visibility("default")))
int hsn_cpp_aligned_to(const void *p, std::size_t align) {
    return aligned_to(p, align) ? 1 : 0;
}

__attribute__((visibility("default")))
void hsn_cpp_copy_bytes(void *dst, const void *src, std::size_t n) {
    if (dst == nullptr || src == nullptr || n == 0 || dst == src) {
        return;
    }
#if defined(__x86_64__)
    auto *d = static_cast<unsigned char *>(dst);
    const auto *s = static_cast<const unsigned char *>(src);
    if (aligned_to(d, 32) && aligned_to(s, 32) && n >= 32) {
        std::size_t i = 0;
        for (; i + 32 <= n; i += 32) {
            const __m256i v = _mm256_load_si256(reinterpret_cast<const __m256i *>(s + i));
            _mm256_store_si256(reinterpret_cast<__m256i *>(d + i), v);
        }
        if (i < n) {
            std::memcpy(d + i, s + i, n - i);
        }
        _mm256_zeroupper();
        return;
    }
#endif
    std::memmove(dst, src, n);
}

__attribute__((visibility("default")))
void hsn_cpp_prefetch_read(const void *p, std::size_t n) {
    if (p == nullptr || n == 0) {
        return;
    }
    const auto *b = static_cast<const unsigned char *>(p);
    for (std::size_t off = 0; off < n; off += kCacheLine) {
#if defined(__GNUC__)
        __builtin_prefetch(b + off, 0, 3);
#endif
    }
}

__attribute__((visibility("default")))
void *hsn_cpp_map_anon(std::size_t n) {
    if (n == 0) {
        return nullptr;
    }
#if defined(__linux__)
    void *p = mmap(nullptr, n, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (p == MAP_FAILED) {
        return nullptr;
    }
    return p;
#else
    return nullptr;
#endif
}

__attribute__((visibility("default")))
int hsn_cpp_unmap(void *p, std::size_t n) {
    if (p == nullptr || n == 0) {
        return 0;
    }
#if defined(__linux__)
    return munmap(p, n) == 0 ? 1 : 0;
#else
    (void)p;
    (void)n;
    return 0;
#endif
}

__attribute__((visibility("default")))
int hsn_cpp23_abi() {
    return 0x4332332B; /* 'C23+' */
}

} // extern "C"
