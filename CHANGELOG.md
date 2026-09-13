# Changelog

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
