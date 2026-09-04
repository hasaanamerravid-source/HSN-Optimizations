# Changelog

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
