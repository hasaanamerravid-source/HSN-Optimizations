# HSN-Optimizations

**Version:** 3.8.8 R
**Minecraft:** 26.2
**Loader:** Fabric only (Architectury API and NeoForge port removed)
**License:** MIT

Client-side distance culling and a few cheap tick helpers. Meant to sit next to Sodium, not replace it.

## Run from the terminal (Gradle)

Need **JDK 25** on PATH. First run downloads Minecraft, Fabric API, YACL.
Uses Loom plugin id `net.fabricmc.fabric-loom` 1.17.20 (not the legacy `fabric-loom` id).

```bash
./gradlew runClient
# Windows PowerShell:
#   .\gradlew.bat runClient
# same thing:
./gradlew runClientFabric
```

Dedicated server:

```bash
./gradlew runServer
./gradlew runServerFabric
```

Game dir (saves, logs, `hsn-optimizations.json`): `run/`

Headless CI / SSH with no display will fail at GLFW window creation. You need a desktop session (or Xvfb). Stop with Ctrl+C in that terminal.

In-game check: F3 overlay / HSN line, **F6** kill switch, **F7** settings. Log line looks like `HSN 3.8.8 R ready on fabric`.

## Build jars

```bash
./gradlew build
# Windows PowerShell:
#   .\gradlew.bat build
```

- `build/libs/hsn-optimizations-3.8.8-R.jar`  (Java only — upload **this** to CurseForge)
- `build/libs/hsn-optimizations-3.8.8-R-sources.jar`  (optional, attach as source)

Do **not** upload the Gradle source zip. Do **not** pass `-PbundleNatives=true` for CurseForge.


YACL must be in the mods folder next to the jar. Fabric API is required. Architectury is **not** required.

## CurseForge upload

1. `./gradlew.bat build` (or `./gradlew build`)
2. Upload only `build/libs/hsn-optimizations-3.8.8-R.jar`
3. Mark loaders Fabric, game 26.2, Java 25
4. Environment: client-focused; server is safe no-op

The default jar contains **no** `.so` / `.dll` / `.wasm`. Those binaries are what CurseForge's scanner sends to manual review. All gameplay paths already have a Java fallback (`native=false` on Windows).

## Natives

Optional Linux x86_64 kernels. Missing `.so` files fail open to Java.

```bash
cd native && make && make rust && make verify
```

## Layout

```
src/main/java          Java sources (`hsn.optimizations`), mixins, Fabric entrypoints
src/main/resources     fabric.mod.json, mixin configs, icon, language file
native/prebuilt        optional Linux kernels (not in the CurseForge jar)
src/sodiumStubs        compile-only Sodium API stubs
native/                C / C++ / ASM / ISPC / HSNL sources
rust/                  hotpath + LOD crates
```

See `ARCHITECTURE.md` for the Panama / native pipeline.
