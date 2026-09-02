# libhsn_hotpath

Batch distSq tests. Java talks to it through Panama (NativeBridge), not JNI.

- `hsn_cull_mask` / `hsn_quality_batch`: AVX-512 (8-wide f64), AVX2 (4/8-wide), or scalar
- `hsn_cpu_flags`: bit0=loaded, bit1=AVX, bit2=AVX2, bit3=FMA, bit4=AVX-512F
- `hsn_set_simd_mode`: 0 auto, 1 scalar, 2 AVX2, 3 AVX-512
- `hsn_active_simd`: kernel that will actually run after policy + CPUID

AVX-512 kernels live in `src/avx512.c` and are compiled by gcc (`-mavx512f`) from `build.rs`.
Stable Rust 1.75 cannot emit AVX-512 from `std::arch`. The C object is only called after CPUID reports AVX-512F.

A requested mode that the CPU does not support falls back (AVX-512 → AVX2 → scalar).
The whole native path can be turned off in the Performance tab.

Do not call this once per particle. HotPath stays on that path.

    cargo test --manifest-path rust/hsn_hotpath/Cargo.toml
    cargo build --release --manifest-path rust/hsn_hotpath/Cargo.toml

Linux x86_64 binary: src/main/resources/natives/linux-x86_64/libhsn_hotpath.so
Java 25 may need --enable-native-access=ALL-UNNAMED. Missing library is ignored.
