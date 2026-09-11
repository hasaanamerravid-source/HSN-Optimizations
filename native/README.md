# Native kernels

Batch work that is cheaper off the JVM. Java still owns the mixin glue.
Missing libraries fall back to the Java loops in `NativeBridge`.

| File | Language | Export | Used for |
|---|---|---|---|
| `rust/hsn_hotpath` | Rust + C | `hsn_cull_mask`, `hsn_cull_xyz`, AVX-512 object | existing hotpath |
| `native/asm/hsn_dist.S` | x86-64 assembly (AVX2) | `hsn_asm_cull_f64` | squared-distance mask |
| `native/c/hsn_c.c` | C11 | `hsn_c_cull_f64`, `hsn_c_rsqrt_f32` | mask + rsqrt |
| `native/cpp/hsn_batch.cpp` | C++17 | `hsn_cpp_cull_xyz`, `hsn_cpp_cone_mask` | section / entity XYZ + cone |
| `native/go/hsn_keep.go` | Go | `hsn_go_keep_mask` | particle/sound keep masks |
| `native/wasm/hsn_simd.wat` | WebAssembly SIMD | `cull_f64` contract | portable fallback |
| `src/main/kotlin/.../GpuTier.kt` | Kotlin | `GpuTier.pick` | hardware preset |
| `src/main/scala/.../KeepMask.scala` | Scala | `KeepMask.apply` | keep/drop masks |
| `native/pipeline/simd_math.s` | x86-64 AVX2 | `hsn_simd_rsqrt_f32` | packed invsqrt |
| `native/pipeline/native_engine.c` | C11 | `hsn_engine_cull_aabb` | frustum / AABB |
| `native/pipeline/cpp_vulkan.cpp` | C++23 | `hsn_cpp_cacheline_aligned` | mmap + align |
| `rust/rust_lod` | Rust | `hsn_lod_thresholds` | LOD bands |
| `native/zig_culler/culler.zig` | Zig | `hsn_zig_mul_mat4` | matrix / hash |
| `native/nim/hsn_nim.nim` | Nim | `hsn_nim_cull_f64` | squared-distance mask |
| `native/d/hsn_d.d` | D (`betterC`) | `hsn_d_cull_f64` | squared-distance mask |
| `native/fortran/hsn_fortran.f90` | Fortran 2003 | `hsn_fortran_cull_f64` | squared-distance mask |

```
cd native && make && make rust && make zig && make verify
```

Outputs land in `src/main/resources/natives/linux-x86_64/`. Linux x86_64 only.
