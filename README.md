# HSN-Optimizations 

**Version:** 3.8.3 
**Minecraft:** 26.2 (Fabric)  
**License:** MIT  

Lightweight **client-side** performance helpers aimed at weak / older GPUs.


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

- Java 25  
- Minecraft Fabric 26.x
- Fabric API  
- Cloth Config  
- Mod Menu 

## Build

```bash
./gradlew build --no-parallel
```

Output: `build/libs/hsn-optimizations-3.8.0-clean.jar` (after version is aligned in `gradle.properties`).



