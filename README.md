# HSN-Optimizations

**Version:** 3.8.5 (release wrap-up)  
**Minecraft:** 26.2 (Fabric)  
**License:** MIT

Client-side distance culling and a few cheap tick helpers for weak GPUs.  
Meant to sit next to Sodium, not replace it.

## What it does

* Particle distance + per-tick spawn budget + keep-rates (combat particles preferred)
* Particle tick cull for already-spawned particles that leave the distance cap
* Entity / item / XP / decoration render distance limits
* Optional entity LOD stage for far low-priority entities
* Block texture LOD (mip bias while the world is drawn)
* Shadow + name-tag distance culling
* Block-entity view-distance cap
* Sound distance culling
* Adaptive FPS-based distance scale + Performance Mode
* Optional native batch path with configurable AVX-512 / AVX2 / scalar fallback
* Optional distant item/XP tick throttle (off by default)
* Distant mob path rebuild throttle (deferred when Lithium is loaded)
* `/locate` result cache
* Optional extra chunk-task drain on world load
* Animated texture throttle, beacon / glow caps, item spin freeze

## What it does not do

* No JNI (optional Panama downcall into `libhsn_hotpath` for batch AVX-512 / AVX2 tests; scalar fallback if the CPU or config forbids them)
* No custom pathfinder replacement
* No custom far-chunk meshes (Distant Horizons / Sodium terrain)
* Does not replace Sodium, Lithium, FerriteCore, or Entity Culling

## Config

Open via **Mod Menu → HSN Optimizations**, or Video Settings when Sodium is installed.

* **General** — profile and master toggles
* **Culling** — particle, entity, block-entity, overlay, and sound distances
* **Rendering** — LOD, textures, terrain mask, fog
* **Performance** — adaptive scaling, SIMD mode, and frame-pacing

The menu uses YACL with colored on/off controllers. F3 lines are color-coded (green / yellow / red FPS).
* **Advanced** — keep-rates, interface, simulation extras

Hover any row for a short tip. Saved to `config/hsn-optimizations.json`.

## Requirements

* Java 25
* Minecraft Fabric 26.x
* Fabric API
* YetAnotherConfigLib (YACL)
* Mod Menu (recommended)
* Sodium (optional)

## Build

Needs **Java 25**. Minecraft 26.2 Loom does not provide `modImplementation`; this project uses `implementation`.

```
./gradlew build --no-parallel
```

Output: `build/libs/hsn-optimizations-3.8.5.jar`

## Hotkeys (defaults)

| Key | Action                  |
|-----|-------------------------|
| F6  | Toggle Performance Mode |
| F7  | Toggle FPS overlay      |
| F8  | Apply ULTRA_LOW preset  |
| F9  | Apply SAFE preset       |
