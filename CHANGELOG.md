# Changelog

## 3.8.7 R (mixin target warnings fixed)

- `LightTextureMixin` and `MapRendererThrottleMixin` used to guess two
  possible target class names each (e.g. `LightTexture` /
  `LightmapTextureManager`). Sponge Mixin logged a `WARN` for every guess
  that didn't match the running version, even though the mod kept working
  fine (`require = 0`).
- Split each guess into its own single-target mixin
  (`LightTextureMixin`, `LightmapTextureManagerMixin`,
  `LightmapTextureManagerYarnMixin`, `MapRendererThrottleMixin`,
  `MapRendererRendererMixin`) and moved them out of the static mixin list.
  `HSNMixinPlugin` now probes each candidate class with a plain
  `Class.forName` check on load and only hands Sponge Mixin the ones that
  actually exist, so a mismatched name is skipped silently instead of
  warning. Whichever candidate matches the running version still gets the
  lightmap-cache / map-throttle optimization applied.
- If none of the known names match, HSN now logs one clear `INFO` line
  explaining that those two optimizations are inactive, instead of three
  raw Mixin warnings.

## 3.8.7 R (world render fix)

- Cloud / weather / world-border LOD no longer target `LevelRenderer`.
  Their cancellable `render` inject was hitting the 26.2 world pass, so
  chunks never drew and the title-screen panorama stayed under
  "Loading terrain…".
- Sodium circular mixin no longer replaces `testDistance`. It only
  applies the optional shape mask after Sodium's own range test.

## 3.8.7 R (26.2 compile + extra kernels)

- Minecraft 26.2: `Painting` lives in `net.minecraft.world.entity.decoration.painting`.
- Minecraft 26.2: settings key uses `ClientScreens` (`Minecraft.gui.setScreen` / `gui.screen()`).
- Minecraft 26.2: `ResourceKey.identifier()` replaces `location()`.
- Added batch distance-mask kernels in Nim (`libhsn_nim.so`), D (`libhsn_d.so`) and Fortran (`libhsn_fortran.so`). Missing compilers / `.so` files fail open to the existing C/ASM/Java path.


## 3.8.7 R (FFM lifecycle)

- Thread-local confined `Arena` scratch (`FfmSegments`) with 64-byte slices for
  plane / payload / mask when heap `MemorySegment.ofArray` is rejected.
- Rust LOD AABB (`hsn_lod_cull_aabb_f32`) stays zero-copy: raw slices only.
- Assembly frustum: `hsn_asm_cull_aabb_f32` / `hsn_asm_cull_sphere_f32`.
- Toggle **Native Frustum Culling** drives section batches plus particle /
  sound / beacon tests. Off leaves vanilla + distance culling alone.

## 3.8.7 R (FFM pipeline)

- Added confined-arena Panama driver `NativePipeline` plus extra native libs:
  AVX2 `vsqrtps`/`vdivps` rsqrt (`simd_math.s`), frustum/AABB engine
  (`native_engine.c`), C++23 cacheline/mmap helpers (`cpp_vulkan.cpp`),
  zero-alloc Rust LOD (`rust_lod`), Zig matrix/hash (`zig_culler`).
- `libhsn_c.so` rsqrt uses AVX2 sqrt+div when CPUID says so; Newton fallback stays.
- Native extract now drops every `libhsn_*.so` when the version marker changes
  so a new jar cannot keep a stale sibling library.

## 3.8.7 R (hotpath rebuild)

- Rebuilt `libhsn_hotpath.so` from current Rust so `hsn_cull_xyz` is exported.
- C++ XYZ/cone kernels now dispatch AVX2 at runtime (scalar fallback).
- New `libhsn_c.so`: AVX2 distance mask + two-Newton `rsqrt` batch.
- Native extract writes `hsn-native.version` so stale `/tmp` copies are replaced.
- `native/test/verify_kernels.c` checks ASM/C/C++/Rust against a scalar oracle.
- Documented why extra languages stay off the frame path (`ARCHITECTURE.md`).



## 3.8.7 R (polyglot)

- Panama Foreign Function API drives every native downcall and the WASM linear memory.
- WebAssembly SIMD module (`natives/wasm/hsn_simd.wasm`, `f64x2.splat` / two-wide compare).
- Kotlin `GpuTier.pick` selects the hardware preset.
- Scala `KeepMask.apply` fills keep/drop masks.
- Assembly AVX2 mask still sits in front of the distance batch.

## 3.8.7 R (native languages)

- Dropped PORTING.md and ACCESSORS.md.
- Added three extra native kernels used on real batch paths:
  - x86-64 assembly (`hsn_asm_cull_f64`, AVX2)
  - C++ (`hsn_cpp_cull_xyz`, `hsn_cpp_cone_mask`)
  - Go (`hsn_go_keep_mask`)
- Java `NativeBridge` loads all four `.so` files and falls back to Java if one is missing.

## 3.8.7 R (slider + kill-switch fix)

- Entity / item / XP / decoration / particle / block-entity sliders now mean
  exactly that many blocks. Priority weights, LOD shrink and a second square
  were stacking on top of the slider (32 looked like ~3, 6 looked like ~16).
- Adaptive culling no longer touches distances unless you turn it on.
- Sodium section occupancy defaults off and only runs past 24 blocks.
- Master kill switch (`modEnabled`, F6): every HSN mixin becomes a no-op.
- F7 opens the settings screen. F8 / F9 preset binds that only printed chat
  are gone.
- Sliders apply immediately (HotPath rebuilds on change, not only on save).



## 3.8.7 R

- High-end CPU pass aimed at 240–500+ FPS clients, not low-end iGPUs.
- Lightmap cache: skip `LightTexture` rebuilds while gamma / dimension / player
  state are unchanged; force a rebuild every 20 frames.
- Skip `lerpTo` / old-position copies on far non-combat entities.
- Distant client ticks for items, XP, armor stands, frames, paintings, displays.
- Living-entity walk-animation and effect-particle throttle (does not cancel tick).
- Unfocused window FPS cap (default 30).
- Cloud / weather / sky extras / world-border LOD when the pass is invisible.
- Map renderer rebuild interval.
- Firework particle budget + drip/falling fluid throttle.
- Hard particle cap reported through `ParticleEngine.countParticles`.
- Idle AI throttle for far, non-combat mobs on the integrated server.
- New **Competitive** preset (long distances, CPU cuts on, target 360 FPS).
- Flagship GPUs (RTX 4090/50-series, RX 7900/9070 class) auto-tier to Competitive.
- Target FPS slider now goes to 1000. Entity/particle clamps raised.
- F3 adds a hi-end line: lightmap / interp / tick / anim skips per second.

## 3.8.6 (patched)


## 3.8.6 (patched)

- **Fix entity-culling distances for items / XP orbs / decorations**: the mixin
  applied the generic mob priority weight (0.55 for loot, 0.60 for armor stands
  and item frames) *and* the extra LOD shrink on top of the dedicated
  "Item Distance", "XP Orb Distance" and "Decoration Distance" sliders. A slider
  of 20 blocks was actually culling around 9–11 blocks. Those entity types now
  honor the configured block distance (adaptive scale still applies).
- **YACL descriptions**: every Mod Menu setting now has a full tooltip —
  what the control does, when to use it, and a professional frame-rate note
  that states whether lower/higher values (or on/off) raise or lower FPS.

- **Fix crash on world join**: `SodiumCircularMixin`'s `@Redirect` on `OcclusionCuller.testDistance` had a stale
  handler signature (`(Object, float, float, float)`) left over from an older Sodium build. Sodium 0.9.1+mc26.2
  changed `testDistance` to `(float, float, float, WriteQueue, RenderSection, int, boolean, boolean, boolean)`,
  so mixin application failed with `InvalidInjectionException` and took the whole game down as soon as a world
  loaded. The handler now matches Sodium's real signature and reads the section straight from the redirected
  call instead of relying only on the earlier `@Inject`-captured field.
- **Fix missing "HSN Optimizations" tab in Sodium's Video Settings**: the `SimdMode` enum option never had a
  `setElementNameProvider` set, which the Sodium Config API requires for every enum option. That made the whole
  `registerConfigLate` call throw, so the entire HSN page silently failed to register (logged only as a WARN).
- **New**: one-time hardware-tier auto-detection. The first time a *brand-new* config sees a real GPU string,
  HSN now picks a sensible starting preset automatically — `ULTRA_LOW` for very old / software-rendered GPUs
  (old Intel HD 2000-4000, llvmpipe/softpipe), `SAFE` for other integrated GPUs, and `BALANCED` for confidently
  dedicated GPUs (GeForce RTX/GTX, Radeon RX, Arc). Existing configs are marked as already-tiered on load so
  upgrading never overwrites a preset you picked yourself; change it anytime from the presets screen.
- **Perf**: particle spawn culling no longer calls `Math.sqrt()` per particle — the un-squared distance limit is
  now cached in `HotPath` and only recomputed when the config or adaptive scale actually changes.

- Version stays 3.8.6. Fabric lifecycle events only; Architectury is not required.
- Sodium `visitNode` inject uses typed `WriteQueue` + `RenderSection` at compile time.
- Sodium Extra detection. Fog, toasts, beacons, and texture animation can be deferred to Extra (defaults on except particles).
- Section occupancy cull: entities in sections Sodium did not visit this frame are skipped. Empty set fails open.
- Optional Panama `hsn_cull_xyz` batch kernel (AVX-512 / AVX2 / scalar). Missing symbol falls back to Java.
- `BatchDistance` is used by the terrain-mask section filter, not left unused.
- Sodium Options integration logs Config API presence and opens the YACL screen as a fallback.
- Native extract file is versioned `libhsn_hotpath-3.8.6.so` and is replaced when the bundled size changes.
- YACL is the settings UI (not Cloth Config).
