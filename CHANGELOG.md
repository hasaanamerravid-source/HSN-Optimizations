# Changelog

## 3.8.8 R — shaders + sky + simple menu

- **Shader Safe Mode** (on by default): while an Iris/Oculus pack is bound, HSN will not resize the world framebuffer, freeze the lightmap, scale fog, cut the horizon, or skip clouds/sky. That was the BSL "day at night / shadows in the wrong place" path.
- **Sky at non-100% scale:** sky draws on the native buffer; the scaled world is blended back. Minecraft's main target is swapped with GameRenderer's.
- Settings open on a **Simple** tab (profile, kill switch, two distances, world scale, shader safe). Search still has every slider.


## 3.8.8 R — Ambient Block Tick cull

- New **Ambient Block Ticks** pass. Vanilla `ClientLevel.animateTick` scans a ~16-block cube every client tick for torch flame, drip, spores and furnace crackle.
- HSN runs that walk on an interval (default every 2 ticks) and caps range at 12 blocks. Performance Mode tightens both.
- Combat, redstone and real block updates are unchanged. Quality preset leaves the walk vanilla.


## 3.8.8 R — render scale keeps sky + settings cleanup

- World scale other than 100% drew terrain into `GameRenderer.mainRenderTarget` and sky into `Minecraft.getMainRenderTarget()`. The upscale blit then replaced the sky with the scaled target's clear color (flat blue).
- Scale now retargets both buffers for the world pass and restores both before blit.
- Settings tabs renamed to Start here / Distance / Picture / Speed / Extras / Tweaks / Other mods, with a short play-first intro.
- Source zip no longer ships BUILD-OK.txt or the generated JVM-hint text file.


## 3.8.8 R — multiplayer "Joining world…" overlay

- Admit-when-ready cancelled `Gui.setScreen(ReceivingLevelScreen)` as soon as the local player existed.
- On remote servers that handshake screen stays until chunks arrive. Cancelling it left the blur overlay up with the world already drawing behind it.
- Kill switch looked like the fix because `modEnabled=false` disabled the same hook.
- Instant World Open / Admit Play When Ready now run on integrated / LAN hosts only. Multiplayer uses vanilla dismiss.


## 3.8.8 R — Code and package cleanup

- Renamed Java package `hsn.modod` → `hsn.optimizations` to match the mod id and display name **HSN-Optimizations**.
- Maven group is now `hsn.optimizations`. Fabric entrypoints and mixin configs updated.
- Renamed duck interface `HsnMainTarget` → `HSNMainTarget`.
- Tightened imports (no leftover fully-qualified internal types), marked config / mixin plugin final, removed dead mixin-plugin probe code.
- Shared clamp / interval helpers in `HotPath` and `ThrottleUtil`. Behavior is unchanged.


## 3.8.8 R — RenderScale GpuSampler compile fix

- `RenderScale.blitVanilla` no longer passes an `Object` into `RenderPass.bindTexture(..., GpuSampler)`.
- Sampler lookup stays reflective; the 3-arg bind is invoked through `Method` so the 26.2 `GpuSampler` signature compiles.



## 3.8.8 R — RenderScale-style world framebuffer

- World scale now swaps `GameRenderer.mainRenderTarget` to a scaled `MainTarget` for `renderLevel`, then blits back so the HUD stays native. Lying about `Window.getWidth` alone never resized the FBO.
- Ultra Low writes 0.50x scale on. Safe writes 0.75x. Re-apply the preset (or flip Enable 3D Scale) after updating.

## 3.8.8 R — DebugScreenOverlay CallbackInfo

- `extractLines` / `collectLines` / `drawLines` are void on 26.2. The CIR inject crashed apply with `Expected CallbackInfo but found CallbackInfoReturnable`.
- One `@Inject` with `CallbackInfo` covers all three names again.

## 3.8.8 R — BufferStorage CallbackInfoReturnable + project flatten

- `BufferStorage.create` returns a value. The mixin used `CallbackInfo` and Fabric crashed at GL init with `InvalidInjectionException: CallbackInfoReturnable is required`.
- Handler now takes `CallbackInfoReturnable<?>` and still only `@Inject`s at HEAD (MixinExtras 0.5.4 still cannot `@Redirect` this method).
- Debug overlay line injects: void `drawLines` keeps `CallbackInfo`; `extractLines` / `collectLines` use `CallbackInfoReturnable`.
- Path navigation `tick` inject is `require = 0` so a renamed 26.2 method disables one hook instead of crashing.
- Mixin configs use `defaultRequire: 0` as previously documented.
- Dropped the leftover Architectury-style `common/` + `fabric/` split. One `src/` tree.

## 3.8.8 R — MixinExtras BufferStorage crash fix

- Replaced `BufferStorageMixin` `@Redirect` with `@Inject` at `create` HEAD.
- MixinExtras 0.5.4 on Fabric Loader 0.19.3 wraps `@Redirect` and crashes:
  `ClassCastException: ArrayList cannot be cast to AnnotationNode`
  in `FactoryRedirectWrapperMixinTransformer` while transforming
  `com.mojang.blaze3d.opengl.BufferStorage`.
- Frame-pacing still flips `GLCapabilities.GL_ARB_buffer_storage` off on
  Intel HD 2000–4000 so mutable buffers are used.



## 3.8.8 R — working RenderScale path + horizon plane + shipped native

- World scale follows RenderScale: lie about window size only during `renderLevel`, Linear/Nearest on the world target, resize hook, no HUD scale. Stays off when Resolution Control / RenderScale is loaded.
- Horizon Y uses the real FOV for frustum planes and a distance-dependent floor. Section lists run a packed AABB + bottom-plane test through `libhsn_horizon.so` (shipped) with Java fallback.
- RenderShapeCuller reuses buffers. VoxelSniper matches item ids (`arrow`, `gunpowder`, `wooden_axe`), not substrings. Keys stay unbound until set in Controls.

---

## 3.8.8 R — 26.2 compile fixes + Horizon Y batch

- Compile on 26.2: keybinds no longer import Fabric KeyBindingHelper (reflection), LOD bias uses a float texParameter lookup, display resize uses renamed Minecraft methods.
- Horizon Y section lists run as a packed batch: C AVX2 / Rust LOD / ISPC / ASM when rebuilt, WASM-style 2-wide loop, then Java 8-wide scalar. Missing `.so` symbols fail open.
- Rebuild natives with `cd native && make && make rust_lod` to pick up `hsn_horizon_y_f32`.

---

## 3.8.8 R — VoxelSniper + full settings + usable F3

- VoxelSniper / WorldEdit integration: while an arrow, gunpowder or wand is held, Horizon Y and the terrain mask can pause so the aimed block stays drawn. Idle when those mods are absent.
- Remaining sliders are in Mod Menu (Compat tab) and Sodium Video Settings (HSN Extra / High-end / Compat pages).
- F3 right column is two short lines by default (fps, cull scale, render scale, drop counts, Y-floor, VS state). Compact / Details toggles in Interface.

---

## 3.8.8 R — horizon Y cull + render-scale pass

- Horizon Y cull: each frame compute the lowest world Y the camera can see from pitch, FOV and view distance. Sections, entities and particles entirely below that floor are not drawn. Looking down keeps caves; a keep-band under the camera stops the floor at your feet from popping.
- Vertical Range Limit now actually clips sections above/below the camera (the slider was previously unused).
- Render scale: 5% quantization, 8-px framebuffer alignment, cached width/height, resize debounce. Optional Adaptive World Scale follows Adaptive Culling without rebuilding the FBO every tick.

---

## 3.8.8 R — scale, Vector API, AVX-512, L3-aware batches

- Version bump to **3.8.8 R** (`mod_version=3.8.8-R`, native extract key `3.8.8-R`).
- 3D render scale (Resolution Control-style): world framebuffer 25–200%, HUD stays native, Linear/Nearest filter.
- If Resolution Control / ResolutionControl++ / RenderScale is loaded **or** a matching jar sits in `mods/`, HSN scale turns itself off and the config row is struck through + locked.
- Java Vector API path (`VectorCullIncubator`) with a portable FMA fallback when `jdk.incubator.vector` is missing.
- L3 cache size from sysfs drives native batch floor and worker count.
- Makefile: fail-open AVX-512 `.so`, isolated build temp dir, `.DELETE_ON_ERROR`.
- HSNL compiler emits 8-wide then 4-wide C loops.

---

## 3.8.7 R — Minecraft 26.2 Fabric load / HUD / screen fixes

- Gradle plugin id is now `net.fabricmc.fabric-loom` (required for unobfuscated 26.1+). The legacy `fabric-loom` id is remap-only and broke this workspace.
- Dropped the dummy identity mappings jar from `dependencies`. New Loom compiles against the official 26.2 names.
- Bumped Fabric Loader to 0.19.5 and Fabric API to `0.159.0+26.2`.
- World-open “admit when ready” now injects `Gui.setScreen`. `Minecraft.setScreen` was moved in 26.2, so the old mixin never ran.
- FPS overlay registers through `HudElementRegistry`. `HudRenderCallback` is gone in Fabric API 26.2.
- Texture-atlas LOD no longer hard-shadows `location()` / `require = 1` on `upload` (compile/apply fail on renamed methods).
- Block-texture LOD bias is skipped unless a GL context is current (Vulkan / no-context safe).
- Mixin injectors now defaultRequire 0 so a renamed 26.2 method disables one hook instead of crashing the game.
- Entity / particle / shadow / sound / toast / item-throttle injects set require = 0.
- Idle AI remembers whether a player is in range instead of cancelling nearby mobs between scans.
- Item throttle leaves the first 40 ticks, fluids, fire, and airborne items vanilla.
- Locate cache no longer returns null when structures are enabled; cells are 32 blocks.
- Loading-overlay skip matches exact screen class names only.
- Cloud LOD only skips when looking sharply down; weather LOD only under a ceiling.
- LevelRenderer LOD hooks `renderLevel` only (not the main `render` pass).
- Buffer-storage pacing is Intel HD 2000–4000 only.
- Native extract versions each `.so` and no longer deletes the whole native folder.
- Removed unused `hsn-polyglot.jar` dependency.
- Sodium Extra detected under `sodium-extra` / `sodium_extra` / `sodiumextra`.

- Replaced leftover `modImplementation` / `modCompileOnly` / `include` with `implementation` / `compileOnly`. Non-remap Loom 1.17 does not create those configurations.

---

## 3.8.7 R — Fabric-only (Architectury API + NeoForge port removed)

Reversed the multi-loader split. The workspace is a single Fabric Loom project again.

| Before | Now |
| --- | --- |
| Architectury workspace: `common` + `fabric` + `neoforge` | One Fabric Loom project; `common/` and `fabric/` are source dirs only |
| Runtime dep on Architectury API 21.0.7 | No Architectury API, plugin, or `@ExpectPlatform` |
| NeoForge jar + `HSNNeoForge` + NeoForge platform impl | `neoforge/` deleted |
| Architectury lifecycle / tick / keybind / HUD events | Fabric API `ServerLifecycleEvents`, `ServerTickEvents`, `ClientTickEvents`, `KeyBindingHelper`, `HudRenderCallback` |
| `HSNPlatform.modPresent` tried Architectury then Fabric then NeoForge via exceptions | Direct cached `FabricLoader.isModLoaded` |
| `./gradlew :fabric:build :neoforge:build` | `./gradlew build` → `build/libs/hsn-optimizations-3.8.7-R.jar` |

Other code-path tidy-ups in the same pass:

- One client tick listener handles window gate, scheduler arm, cull stats, and F6/F7 keys.
- Mixin plugin caches Sodium / Sodium Extra / Framepace presence once at `onLoad`.
- GC collector name is cached after the first MXBean scan.
- FPS overlay reuses a single `StringBuilder` instead of allocating every frame.

Minecraft 26.2, Java 25, mixin set, culling / LOD / natives, and YACL settings are unchanged.

---

## 3.8.7 R — this chat vs the starting zip (`HSN-Optimizations-final`)

Version **name stayed 3.8.7 R**. What changed is the tree and the stutter path.

### Loaders

| Before (start of chat) | Now |
| --- | --- |
| Fabric-only Loom project | Architectury workspace: `common` + `fabric` + `neoforge` |
| `fabric.mod.json` only | Fabric jar **and** NeoForge jar from the same common code |
| `FabricLoader` / Fabric events in shared classes | `HSNPlatform` + `@ExpectPlatform` + Architectury lifecycle / tick / keybinds |
| Mod Menu entry in the shared client sources | Mod Menu stays Fabric-only; NeoForge gets a config-screen factory |

Runtime still needs Architectury API **21.0.7** and YACL on both loaders. Fabric still needs Fabric API.

### Stutter / frame-pacing (the part that did not work)

Before:

- `HSNScheduler.apply()` called `Thread.getAllStackTraces()` from the client tick (stack-dumps every live thread; that *is* a hitch).
- That scan ran every tick for the first 40 client ticks.
- Render thread was set to `Thread.MAX_PRIORITY`.
- Frame time was sampled from `Minecraft.tick` / `runTick`, not the frame.
- Smart yield ran when disabled, and still yielded on an already-late frame.
- "Frame-pacing workaround" only disabled `GL_ARB_buffer_storage` for Intel HD 2000–4000, so it was a no-op on modern GPUs.
- The toggle defaulted **off**.

Now:

- Threads are listed with `ThreadGroup.enumerate` (no stacks).
- Priorities applied **once** per session (`applyOnce`), never `MAX_PRIORITY`.
- Frame EWMA + jitter come from `GameRenderer.render`.
- Yield only when the last frame had headroom under ~60 FPS and jitter is low.
- Mutable buffers also trigger on integrated GPUs with high jitter, not only ancient Intel HD.
- Frame-pacing **on** by default. Quality preset still turns it off.

### Build / run

Before: `./gradlew build` on a single Fabric project.

Now:

```
./gradlew runClient              # Fabric client
./gradlew runClientFabric        # same
./gradlew runClientNeoForge      # NeoForge client
./gradlew :fabric:build :neoforge:build
```

Jars: `fabric/build/libs/hsn-optimizations-3.8.7-R.jar` and `neoforge/build/libs/hsn-optimizations-3.8.7-R.jar`.

Needs JDK **25**. Game dirs: `fabric/run/` and `neoforge/run/`.

### Left alone on purpose

- Minecraft 26.2 target, Java 25, mixin set, culling / LOD / natives.
- Polyglot sidecars (C, C++, ASM, Rust, ISPC, Go, …) and Linux `.so` fail-open path.
- Windows still uses the Java hot path (no shipped Windows natives).
- YACL settings screen, F6 / F7, Sodium optional hooks.

### Not done in this pass

- Did not launch `runClient` here (this environment is JDK 21; 26.2 refuses that).
- Did not rewrite every sidecar language kernel.
- Did not add a second independent Forge (old Forge) loader — NeoForge only, plus Fabric.

---

## 3.8.7 R (scheduler + world-open + ZGC)

- World-open prefetch path: C++ thread pool → Rust scoped threads → C → Java.
  ASM `hsn_asm_touch_pages` walks 64-byte lines with AVX loads.

- Replaced cloned thread/load hacks with HSN's own scheduler and
  world-open path. No availableProcessors() lie, no view-distance rewrite.
- Scheduler reads visible cores and cgroup quota, boosts the render
  thread, and only deprioritizes extra chunk workers on 8+ core machines.
- Smart yield: the client loop yields only when the last frame missed 10ms.
- World-open: C region prefetch, time-boxed chunk drain, admit the first
  frame after the local player exists.
- ZGC: Loom run configs pass -XX:+UseZGC. A running game cannot switch
  collectors; hsn-recommended-jvm.txt is written for launcher paste.
  preLaunch sizes ForkJoin parallelism from CPU quota.


## 3.8.7 R (exact block distances)

- Entity / item / XP / decoration / particle sliders are exact blocks.
  Distance is camera-to-AABB closest point, plus a half-block pad.
- Performance Mode no longer multiplies sliders by minAdaptiveScale
  (that was why "8 blocks" looked like 1–2 blocks).
- Adaptive Culling is the only remaining scale, and only while enabled.
- Existing configs bump to revision 3880 and turn off stacked multipliers.
- Restored empty CullStats (mod would not compile).
- HotPath publishes one immutable snapshot instead of 30 volatiles.
- Java GpuTier / KeepMask so the optional polyglot jar is not required.
- Native libs extract to `.hsn-natives` with owner-only permissions.
- Default cull path is ASM/C/Rust then Java; sidecar languages stay loaded
  but off the frame path.
- Idle AI no longer calls getNearestPlayer every tick.
- Fireworks are not client-tick-skipped.
- Optional behind-camera cull (off by default).
- F3 prints slider vs effective distance (`entity 8=8b`).


## 3.8.7 R (DX12 fallback + Vulkan pipeline cache)

- Fallback chain: DirectX 12 (if the game ever exposes a D3D12 device) →
  Vulkan → OpenGL → CPU. No second D3D12/Vulkan device is created.
- Vulkan pipeline cache file + optional hook on `GpuDevice` if Minecraft
  exposes one. Live device detection always beats the disk cache.
- GL hooks stay off on both Vulkan and DX12.

## 3.8.7 R (OpenGL + Vulkan backends)

- `GraphicsBackend` picks OpenGL or Vulkan from `RenderSystem.getDevice()`
  and LWJGL. Texture LOD and `glGet*` run only on OpenGL.
- Vulkan keeps CPU culling and skips GL hooks (no more native abort).
- C AVX2 cull is 16-wide; rsqrt uses `rsqrtps` + one Newton step.

## 3.8.7 R (release — Vulkan probe + OpenCL fallback)

- GPU probe reads Minecraft 26.2 `RenderSystem.getDevice()` so Vulkan
  names like `Quadro P600` work without `glGetString`.
- OpenCL ICD is detected through LWJGL when present. Compute stays off
  the Vulkan render thread (driver fight). Missing ICD = Java/C path.
- C kernels take `restrict` pointers. Sidecar languages stay off small
  batches. Ready log prints backend, GPU name, and OpenCL status.

## 3.8.7 R (more crash / efficiency fixes)

- Map throttle no longer cancels `render` (maps stayed blank / flickered).
- Sky extras no longer target `LevelRenderer` (26.2 world-pass risk).
- Name tags and particles read `HotPath` instead of the fat config object.
- Extra languages (Nim/D/Fortran/Go/Wasm) only run on batches of 64+;
  ASM/C/Rust stay first. In-process Scala keep-mask beats a Go FFI call.
- HSNL compiler emits 4-wide loops. GPU probe can read the Vulkan device
  name so it never calls `glGetString` on that backend.

## 3.8.7 R (Vulkan / no-GL crash)

- Minecraft 26.2 can use the Vulkan backend. Block texture LOD called
  `GlStateManager._getInteger` during atlas upload. LWJGL aborts the JVM
  when no OpenGL context is current (`FATAL ERROR in native method`).
  All GL reads now go through `GlGuard.glUsable()` (GLFW current context
  + backend name). On Vulkan the texture-LOD hook is a no-op.

## 3.8.7 R (real presets)

- Profile is no longer a partial overlay. Ultra Low / Safe / Balanced /
  Quality / Competitive each write the full gameplay field set. Quality
  is near-vanilla; Ultra Low turns every saver on and shortens every distance.

## 3.8.7 R (search + live sliders + polyglot kernels)

- Config Search tab is a real text field. Apply search rebuilds the tab
  with live sliders and toggles for every match. Values apply while you
  drag (`instant(true)`), so sliders no longer snap back until Done.
- Sliders clamp to their own min/max so an out-of-range JSON value can
  no longer freeze the YACL widget.
- Early-load mixin probe stays on `ClassLoader.getResource` so Sodium
  `preLaunch` cannot hit `MixinTargetAlreadyLoadedException`.
- Entity hot path reads one packed flag word. Shape-mask and occupancy
  tests are skipped when those bits are off. Paintings and Display
  entities use the decoration distance.
- Extra first-party kernels for the GitHub language chart: more AVX2
  Assembly, C++ SoA / LOD / frustum helpers, Rust frustum / SoA / bits
  modules, plus Swift and Haskell sidecars (fail-open if the toolchain
  is missing).
- Added ISPC (AVX2/AVX-512 SPMD) and OpenCL batch kernels for large
  packed arrays, plus HSNL — a tiny home-grown data-parallel language
  compiled to C by `native/hsnl/hsnlc.py`. All three fail open.

## 3.8.7 R (MixinTargetAlreadyLoadedException fix)

- `HSNMixinPlugin` probed `MapRenderer` / light-texture class names with
  `Class.forName(..., false, cl)`. That *defines* the target in Knot before
  Sponge Mixin can prepare the matching mixin, so the game crashed at
  Sodium's `preLaunch` entrypoint:
  `MixinTargetAlreadyLoadedException: MapRendererRendererMixin target
  net.minecraft.client.renderer.MapRenderer was loaded too early`.
- Probe is now a `ClassLoader.getResource("…/MapRenderer.class")` lookup
  (bytecode present, class not defined). Already-defined targets are
  skipped instead of crashing.

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
