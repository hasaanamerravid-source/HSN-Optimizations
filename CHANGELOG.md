# Changelog

## 3.8.5 wrap-up

Nothing was removed. Rust `libhsn_hotpath`, the Linux `.so`, Panama `NativeBridge`, `HotPath`, `SimdMode`, YACL, and Architectury stay in the tree.

### Improvements
- `HSNTickState` now rebuilds `HotPath` and publishes the adaptive scale every tick so mixins see live distances.
- `/locate` cache drops expired hits and is cleared when the server stops.
- Item-entity tick throttle skips the client side and dead entities (same rules as XP orbs).
- Native library extract uses a versioned temp file so the `.so` is not rewritten every launch.
- FPS overlay no longer double-counts cull stats.

### Still included
- rust/hsn_hotpath (AVX-512 C kernel + AVX2 / scalar fallback)
- src/main/resources/natives/linux-x86_64/libhsn_hotpath.so
- NativeBridge, HotPath, SimdMode
- All previous mixins, presets, and config fields
