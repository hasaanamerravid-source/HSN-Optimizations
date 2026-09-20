# Changelog

## 4.1.9

Cloth Config is the main settings UI. Adaptive world scale engages at or below 45 FPS.

- Settings open order: Cloth Config → YACL → built-in vanilla screen.
- Cloth sliders drag and scroll on the 26.3 SDL mouse path.
- Adaptive world scale uses `adaptiveRenderScaleFps` (default 45). Above that, the world stays native.
- Always-on world scale stays off by default. The drop target is the World scale amount slider (70%).
- Mod icon is black "HSN-Optimizations" text on white.

## 4.1.8

Polish pass on the 4.1.7 Fabric 26.3 tree. No new gameplay features.

- Fabric API pin is `0.161.0+26.3`.
- Native GL context lookup tries SDL3 first, then the vanilla Window handle, then leftover GLFW.
- Window init gate comments match the SDL3 backend.
- GPU tier no longer treats a generic "iris" string as Intel Iris Xe.
- Mixin plugin skips circular-shape injects when another circular-rendering mod is loaded, and honors entity-culling / particle-core defer flags.
- CI uploads a Java-only storefront jar by default and a separate Linux-natives test jar.
- Duplicate TerraformersMC Maven entry removed.

## 4.1.7

Stability hotfix from the 4.1.6 inspection. No new features.

- `HSNConfig.get()` never returns null if JSON load fails.
- Config revision 4170 turns render-scale off and keeps at least 3 chunks around the player.
- YACL sliders use `HSNYaclSlider`: instant `StateManager`, `range/step/formatValue`, live HotPath rebuild.
- `BufferStorageMixin` and `BossOverlayMixin` only register when the target class exists (no silent-miss WARN on 26.3).
- Session GPU guard disables RenderScale (and circular terrain on Intel HD 2000–4000 / software GL).
- Terrain-mask buffers stop unbounded doubling; keep-ring floor is 3 chunks (48 blocks).
- Native miss logs OS/arch once and stays on the Java path.

## 4.1.6

World was blank after join on Intel HD 3000 because render-scale blit failed after the frame was drawn off-screen. Config menu looked locked because a dark panel was painted over the sliders.

- Render scale turns itself off for the session (and writes config off) when blit fails. World stays on the native framebuffer.
- Ultra Low / GPU auto-tune no longer enable render scale or circular terrain on 26.3.
- Existing configs migrate to revision 4160 with scale off and circular off.
- Built-in menu uses a plain "HSN" text title and no longer fills the slider area.

## 4.1.5

- Fixed the next 26.3 launch crash: `DebugScreenMixin.extractLines` is now `(GuiGraphicsExtractor, List, boolean, int)`. Old 3-arg overloads remain as separate descriptor-bound injects.
- F3 HSN status lines still append on the right column.

## 4.1.4

- `RenderShapeMixin` now has the exact 26.3 descriptor `prepareChunkRenders(Matrix4fc, boolean)` plus the older overloads as separate injects. This is the launch crash from `runClient`.
- `cullTerrain` inject no longer lists Camera/Frustum args, so a 26.3 signature tweak cannot fail apply.
- `ParticleManagerMixin` no longer binds the short name `createParticle` (same crash pattern).
- `EntityRendererMixin` uses full `shouldRender(Entity, Frustum, DDD)` descriptors instead of name-only matching.

## 4.1.3

- Fixed a 26.3 crash on launch: `RenderShapeMixin` expected `prepareChunkRenders(Matrix4fc)` but 26.3 is `prepareChunkRenders(Matrix4fc, boolean)`. Injects now use CallbackInfo-only handlers plus explicit descriptors so Mixin apply cannot fail.
- Same hardening on `SectionFrustumShapeMixin` (`addSectionsInFrustum` overloads). Circular / ellipse world shape is still applied.
- Missing `BufferStorage` / `BossBarHud` targets stay warnings only and do not crash.

## 4.1.2

More 26.3 fixes and extra performance work. No features removed.

- Adaptive culling now blends vanilla FPS with real frame time from `runTick`, so scale moves during the second instead of once per FPS counter update.
- Config sliders debounce disk writes (`saveSoon`) so dragging does not stall the render thread. Pending JSON still flushes on the client tick and on Done.
- Sound and particle distance checks use the per-frame camera snapshot when it is valid.
- Animated textures skip harder while the window is unfocused (still respects the existing texture-anim sliders).
- FPS overlay also prints frame time in milliseconds.
- Built-in and YACL sliders still apply instantly through HotPath.

## 4.1.1

Minecraft 26.3 compile and settings fix.

- Removed hard GLFW / `GlStateManager` / `FilterMode` compile references. 26.3 uses SDL3 and may run Vulkan, so those packages are resolved at runtime.
- Keybinds use the 3-argument `KeyMapping` constructor so missing `InputConstants.Type.KEYSYM` no longer fails the build.
- Sodium "All settings" buttons are typed as `OptionBuilder`.
- Render-scale blit picks a pipeline reflectively (`TRACY_BLIT` or a GUI blit fallback).
- YACL sliders now follow the working YACL 3.9 pattern: `IntegerSliderControllerBuilder.range/step`, instant apply, clamped binding, and a live `HotPath` rebuild while dragging.
- 26.3 OIT-aware particle budget: when adaptive scale drops, the hard particle cap tightens so order-independent transparency stays cheaper.
- Built-in settings screen now uses real vanilla sliders (drag + mouse wheel) instead of dead +/− buttons. Wheel events go to the hovered slider first.
- Client mixin `defaultRequire` is 0 so a renamed 26.3 method cannot abort the whole mixin set.
- Entity culling injects extra method aliases and is optional.
- Particle live-count only decrements when the hard cap is actually on.

## 4.1.0

Minecraft 26.3 Fabric port.

- World render scale is on by default at 75% with adaptive resolution. Balanced preset writes the same values so the GPU path actually runs.
- Existing configs that were forced to 1.00x by revision 3888 are migrated to 0.75x.
- Render scale no longer rewrites `Window.getWidth` / `getHeight` (that broke HUD and the 26.3 windowing backend). Only framebuffer queries are scaled during the world pass.
- Sky draws onto the scaled world target. Pausing the sky onto the native buffer used to wipe the dome on blit.
- Blit failure is logged instead of failing silently with no FPS change.
- Adaptive culling and performance mode are on in the default / Balanced profile.
- Sodium Video Settings now expose real tabs: General, Entities, Particles, Rendering, Audio, Server.
- Settings labels use professional names. Icon is a simpler mark.
- Targets Fabric API `0.160.7+26.3`, Sodium API `0.9.2+mc26.3`.

## 4.0.1

Audit pass against stock 4.0.0. Logic matches the UI.

- World shape is one control (Off / Circle / Hexagon / Front half). Turning Circular on no longer leaves `worldRenderShape=OFF`.
- Behind-camera decoration cull honors the Behind-camera toggle.
- Smart yield runs when the last frame missed ~10 ms, matching the tooltip.
- Skip-empty boss overlay is implemented (`BossOverlayMixin`).
- Performance mode eases distances toward 70% when FPS dips. Adaptive culling still owns the full 25–100% scaler.
- Laptop power save reads `/sys/class/power_supply/BAT*/status` when present.
- Config migrations no longer skip the 3888 render-scale reset. Load no longer rewrites JSON every boot.
- `isRemoteMultiplayer` fails closed (treat unknown sessions as remote). Generic dirt screens are not closed as join overlays.
- `ExactDistance.limitSq(0)` is +Infinity so a zero slider never culls the world.
- Particle `add` injector removed from the six-double createParticle mixin. Live counter only moves when the hard cap is on. Spawned particles no longer pop when you turn.
- Sounds are distance-culled only (no frustum). Burst counter increments after the sound is allowed.
- Client tick skip no longer freezes items / XP. Living anim no longer cancels `tickDeath` / `tickEffects`.
- Unfocused cap and camera capture run once per frame. Camera pose prefers `GameRenderer.getMainCamera()`.
- Intel Iris Xe detection no longer treats Iris shaders as an iGPU.
- Mixin plugin no longer loads/saves config during prepare.
- Settings: mouse wheel, shape control, keep-chunks, overlay X/Y, adaptive texture flags.

## 4.0.0

Final 4.0 line. Combines the 3.8.8 R feature set with the 4.0 cleanups.

### What this build actually does
- Distance culling for entities, decorations, particles, block entities, shadows, name tags, glow, beacons, and sounds.
- Real hard particle cap (`ParticleLive`) — spawn is refused when over budget, not just filtered later.
- Far / behind-camera decorations and particles are dropped.
- Weather and clouds skip when the camera cannot see the sky.
- Sky disc stays on the native framebuffer while world scale is on (`SkyNativeMixin` + real `RenderScale.pauseForSky`).
- Lightmap cache on every known 26.2 / Yarn class name, including the Yarn `LightmapTextureManager` variant that 4.0-draft dropped.
- Horizon-Y, circular section mask, progressive LOD, texture mip bias, texture-anim throttle.
- Integrated-server idle AI / pathfinding / item-XP tick throttles. Locate cache. Fast world-open drain.
- Optional Linux natives: C, C++, ASM, Rust only. Missing `.so` files fail open to Java.
- Settings menu is a vanilla Screen. Every `HSNConfig` field is on a tab. No YACL. No Cloth.

### Bugs fixed in this cut
- `HotPath.publishScale` early-return only skipped rebuilds when adaptive culling was on. Identical scales now skip the snapshot rebuild always.
- `AdaptiveCuller.lastReportedVanillaFps` is volatile so a non-render `tick()` cannot publish a stale FPS sample.
- `PathfindingStats.windowStart` and `rate` are volatile. The server thread writes them; the client F3 line reads them.

### What was removed on purpose
- Novelty-language frame-path sidecars (Go, Nim, D, Fortran, Swift, Haskell, Odin, OpenCL, ISPC, Zig, HSNL, WASM). Those never belonged on the render thread.
- YACL / Cloth hard depend.
- Empty `pauseForSky` / `resumeAfterSky` stubs.

### Storefront
- Default `./gradlew build` jar is Java-only.
- `-PbundleNatives=true` embeds the Linux x86_64 kernels for local testing.
