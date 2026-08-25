# HSN-Optimizations

**Version:** 3.8.4  
**Minecraft:** 26.2 (Fabric)  
**License:** MIT

Lightweight **client-side** performance helper. 
Designed to **complement** Sodium (and similar) rather than fight them.

## What it does

### Core culling
* Particle distance + soft count budget + **priority system** (combat kept, decorative culled first)
* Entity / item / XP / decoration render distance limits
* Shadow + name-tag distance culling
* Block-entity distance culling + **LOD**
* Sound distance + weather thinning + optional burst limiter
* Optional mild fog scaling
* Adaptive FPS-based culling + Performance Mode (F6)
* Item entity merge + optional distant item/XP tick throttling

### Unique helpers (things Sodium usually leaves alone)
* **Animated texture throttling** – slows water/lava/portals/fire under load
* **Cloud distance & density**
* **Beacon beam distance**
* **Glow outline distance**
* **Distant item spin throttle**
* **Entity animation relief**
* **Weak-GPU auto layer** – extra aggression when FPS stays low
* **Progressive LOD** – quality falls off near the edge of max distance (no render-distance change)
* **Entity LOD stages** – staged animation / detail reduction by distance
* **Particle quality curve** – keep-chance drops smoothly with distance

### Quality-of-life
* Presets: ULTRA_LOW / SAFE / BALANCED / QUALITY
* **Cloth Config** UI with default Minecraft panorama background
* **Sodium Config API tab** – a dedicated **HSN Optimizations** page in Video Settings (also appears in Reese's Sodium Options automatically)
* Fallback **HSN Optimizations…** button on the Sodium screen
* Mod Menu integration
* F7 FPS overlay, F8 / F9 preset hotkeys, rich F3 status block

## Requirements

* Java 25
* Minecraft Fabric 26.x
* Fabric API
* Cloth Config
* Mod Menu (recommended)
* Sodium (optional, complementary)

## Build

```
./gradlew build --no-parallel
```

Output: `build/libs/hsn-optimizations-3.8.4.jar`

## Config

Open via **Mod Menu → HSN Optimizations**, **Video Settings → HSN Optimizations** (when Sodium is installed), or the **HSN Optimizations…** button inside Sodium's Video Settings.

Settings save to `config/hsn-optimizations.json`.

## Hotkeys

| Key | Action                |
|-----|-----------------------|
| F6  | Toggle Performance Mode |
| F7  | Toggle FPS overlay    |
| F8  | Apply ULTRA_LOW preset |
| F9  | Apply SAFE preset     |
