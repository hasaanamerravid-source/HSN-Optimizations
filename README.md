# HSN-Optimizations 4.0

Fabric 26.2. MIT. Java 25.

Client-side distance culling, particle budgets, and a few cheap tick helpers.
Sits next to Sodium. Does not replace Sodium, Lithium, or Iris.

```bash
./gradlew build
./gradlew runClient
```

Upload only `build/libs/hsn-optimizations-4.0.1.jar`.

## Settings

Mod Menu, or the in-game settings key. Seven tabs:

General · Entities · Particles · Rendering · Audio · Server · Extra

Presets: Ultra Low, Safe, Balanced, Quality, Competitive.

Master switch / F6 turns every HSN pass into a no-op.

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
- Minecraft 26.2
- YACL is **not** required
