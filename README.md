# HSN-Optimizations (cleaned)

**Version:** 3.8.0-clean  
**Minecraft:** 26.2 (Fabric)  
**License:** MIT  

Lightweight **client-side** performance helpers aimed at weak / older GPUs.

This is a cleaned source tree. The original package contained a large amount of experimental, non-functional, and over-engineered code (off-heap particle systems, fabricated “rendering engine” improvements, aggressive adaptive systems, JVM GC forcing from inside the game, etc.). Those have been removed.

## What remains (real, focused features)

- Particle distance + soft count culling  
- Entity / item / XP / decoration render distance limits  
- Shadow distance culling  
- Name-tag distance culling  
- Block-entity distance culling  
- Sound distance + weather sound thinning  
- Optional mild fog scaling  
- Simple performance presets (ULTRA_LOW / SAFE / BALANCED / QUALITY)  
- F7 FPS overlay, F8 / F9 preset hotkeys  
- F3 status lines  
- Cloth Config + Mod Menu integration  
- Basic item-entity merge on the server side  

## Requirements

- Java 25 (as targeted by the original Loom setup)  
- Fabric Loader for Minecraft **26.2**  
- Fabric API matching 26.2  
- Cloth Config  
- Mod Menu (optional)

## Recommended stack

1. A modern terrain renderer (Sodium or equivalent for 26.2)  
2. This mod for particle / entity / sound limits  
3. Lithium / FerriteCore if available for the same version  

## Build

```bash
./gradlew build --no-parallel
```

Output: `build/libs/hsn-optimizations-3.8.0-clean.jar` (after version is aligned in `gradle.properties`).

## Notes on multi-loader / Architectury

The original project is pure Fabric with client mixins. Converting it to a full Architectury multi-loader project (Fabric + Forge/NeoForge + Quilt) is a larger structural change (common/fabric/forge modules, `@ExpectPlatform`, shared mixins strategy, etc.). This cleaned zip keeps the Fabric layout so it remains buildable. A proper Architectury port can be done on top of this cleaned base.

## What was removed

- Entire `exp/` package (HardcoreExp, OffHeapParticleSoA, FrustumPovCull, VectorCull, LazyBudget)  
- Fabricated rendering “engine” stubs (LOD / batch / occlusion / shader / VBO claims)  
- Adaptive FPS ladder, tick budget, frame cache, hysteresis, render-snap systems  
- JVM GC forcing / idle memory trim / OpenGL preference helpers  
- Input latency / mouse hacks  
- Dozens of over-eager or mapping-fragile mixins  
- AI-generated documentation claiming features that did not exist  

The remaining code is intentionally conservative and easier to maintain or port.
