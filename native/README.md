# Native kernels

Batch work off the JVM. Java owns mixin glue. Missing libraries fall back
to the loops in `NativeBridge`.

| File | Language | Export |
|---|---|---|
| `rust/hsn_hotpath` | Rust + C | `hsn_cull_mask`, `hsn_cull_xyz` |
| `native/asm/` | x86-64 AVX2 | `hsn_asm_cull_f64`, AABB / sphere |
| `native/c/hsn_c.c` | C11 | `hsn_c_cull_f64`, `hsn_c_rsqrt_f32` |
| `native/c/hsn_horizon.c` | C11 | `hsn_horizon_y_f32` |
| `native/cpp/` | C++17 | `hsn_cpp_cull_xyz`, cone mask |
| `native/pipeline/` | ASM + C + C++ | rsqrt, AABB, cacheline |
| `rust/rust_lod` | Rust | LOD thresholds |

```
cd native && make && make rust && make rust_lod && make verify
```

Linux x86_64 only.
