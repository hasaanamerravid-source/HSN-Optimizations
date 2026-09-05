# HSN 3.8.7 R — polyglot performance pipeline

Minecraft's hot path is Java mixins. Extra languages only win when they
process a **batch** that is already packed in off-heap or primitive arrays.
One FFI call per entity is a loss.

## What actually runs in-process

| Layer | Language | Artifact | When it runs |
|---|---|---|---|
| Mixin / config | Java 25 | Fabric mod | Every frame |
| Packed flags | Java `HotPath` | static volatiles | Every cull check |
| GPU preset | Kotlin | `GpuTier.pick` | Config / first GPU string |
| Keep mask | Scala | `KeepMask.apply` | Small batches, no FFI |
| Distance mask | x86-64 ASM AVX2 | `libhsn_asm.so` | `n >= 16` |
| Distance + rsqrt | C11 + AVX2 | `libhsn_c.so` | `n >= 16` |
| XYZ / cone | C++17 + AVX2 | `libhsn_cpp.so` | `n >= 16` |
| SIMD policy | Rust + C AVX-512 | `libhsn_hotpath.so` | `n >= 16`, CPUID gated |
| Keep mask (opt) | Go c-shared | `libhsn_go.so` | large keep batches only |
| WASM contract | WAT SIMD | `hsn_simd.wasm` | portable fallback |
| rsqrt + AABB | ASM + C | `libhsn_pipeline.so` | batch normalize / frustum |
| LOD bands | Rust | `libhsn_lod.so` | packed XYZ thresholds |
| Matrix / hash | Zig | `libhsn_zig.so` | optional, skipped if no zig |
| Distance mask | Nim | `libhsn_nim.so` | optional, skipped if no nim |
| Distance mask | D (`betterC`) | `libhsn_d.so` | optional, skipped if no ldc2/dmd |
| Distance mask | Fortran 2003 | `libhsn_fortran.so` | optional, skipped if no gfortran |
| Off-heap copy | C++23 | same pipeline .so | cacheline + mmap |

Java talks to natives **only** through Panama FFM (`MemorySegment.ofArray` +
`Linker.Option.critical(true)`). There is no classic JNI stub, no `Unsafe`
sun.misc path.

## What does *not* belong on the frame path

Fortran, Julia, Mojo, Haskell, OCaml, Crystal, Nim, D, and a second Go
process are useful as **offline / sidecar** tools:

* Haskell / OCaml — mixin rule checks at CI time
* Julia / Mojo — fit adaptive frame-pacing curves from telemetry dumps
* Fortran — pack huge particle debug dumps, not 200 particles/frame
* CUDA / OpenCL / WGSL — Sodium already owns the GPU; a second compute
  queue fighting the driver is a 1% low factory
* Wasm user scripts — sandbox for custom LOD rules, never per-entity

Shipping 20 runtimes inside one Fabric jar guarantees:

1. 20 ABIs to break on a JVM update
2. 20 extract-to-tmpdir races
3. Go/Haskell RTS threads fighting Minecraft's tick thread
4. Worse 1% lows than the Java scalar loop you already have

## Build

```
cd native && make && make rust && make zig && make verify
```

Linux x86_64 only. Missing `.so` files fail open to Java.

## Safety

* Every native entry returns immediately on null / `n == 0`
* AVX-512 object is linked only if `gcc -mavx512f` works; runtime still
  checks `avx512f` before calling it
* AVX2 C/C++ kernels use `__attribute__((target("avx2")))` + CPUID
* Mixins read `HotPath` primitives, never the config object
* Master switch (`modEnabled` / F6) clears every HotPath flag
