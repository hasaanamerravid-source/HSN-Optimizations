# HSN Optimizations 4.1.9

Fabric 26.3. MIT. Java 25.

Client-side distance culling, particle budgets, world render scale, and a few cheap tick helpers.
Sits next to Sodium. Does not replace Sodium, Lithium, or Iris.

```bash
./gradlew build
./gradlew runClient
```

Upload only `build/libs/hsn-optimizations-4.1.9.jar`.

## Settings

Install Cloth Config for the main settings screen (drag-able sliders).
YACL is a fallback. Built-in vanilla screen is last.

Mod Menu, the in-game settings key, or Sodium Video Settings tabs:

General · Entities · Particles · Rendering · Audio · Server · Advanced

Presets: Ultra Low, Safe, Balanced, Quality, Competitive.

Master switch / F6 turns every HSN pass into a no-op.

World render scale stays off by default on 26.3 (Intel HD blit is unsafe). The HUD stays native.

## Natives

Optional Linux x86_64 kernels in `native/prebuilt/linux-x86_64/`.
The storefront jar does **not** include them. Build with
`-PbundleNatives=true` only for local testing.

```bash
cd native && make && make rust && make verify
```

Missing `.so` files fail open to the Java path.

## Requirements

- JDK 25
- Fabric Loader 0.19.5+
- Fabric API
- Minecraft 26.3
- YACL is **not** required
