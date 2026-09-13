# Binaries — Modrinth / CurseForge

This project does **not** ship native binaries in the storefront jar.

## What gets uploaded

| File | Contents |
|---|---|
| `hsn-optimizations-4.0.0.jar` | Java bytecode + mixin JSON + icon. **No** `.so`, `.dll`, `.wasm`, `.exe` |
| `hsn-optimizations-4.0.0-sources.jar` | Java sources only |

`./gradlew build` fails if a `.so` / `.dll` / `.wasm` leaks into the default jar.

Optional Linux kernels are **off** unless you pass `-PbundleNatives=true`. That flag is for local testing, not for Modrinth.

## Native source (all first-party, MIT)

| Path | Language | Produces |
|---|---|---|
| `native/c/` | C11 | `libhsn_c.so`, `libhsn_horizon.so` |
| `native/cpp/` | C++17 | `libhsn_cpp.so` |
| `native/asm/` | x86-64 GAS | `libhsn_asm.so` |
| `native/pipeline/` | C + C++ + ASM | `libhsn_pipeline.so` |
| `rust/hsn_hotpath/` | Rust + C | `libhsn_hotpath.so` |
| `rust/rust_lod/` | Rust | `libhsn_lod.so` |

Build:

```bash
cd native
make            # C / C++ / ASM
make rust       # needs rustc + cargo
make rust_lod
make verify     # optional smoke test
```

Objects land in `native/prebuilt/linux-x86_64/`. That folder is gitignored.

## Automation

`.github/workflows/ci.yml`:

1. Builds the Java jar on JDK 25 (`./gradlew build`).
2. Confirms the jar contains no ELF / WASM / PE.
3. On `ubuntu-24.04`, compiles every native library from the sources above.
4. Uploads those `.so` files as a **CI artifact**, not as a storefront file.

Anyone can fork the repo, press “Run workflow”, and get the same objects.

## Runtime

`NativeBridge` loads `/natives/linux-x86_64/*.so` from the jar only if they were bundled.
The default jar has none, so every path uses the Java fallback. Missing libs never crash the game.

## Other binaries in the tree

- `gradle/wrapper/gradle-wrapper.jar` — official Gradle wrapper. Not game code.
- `src/main/resources/assets/hsn-optimizations/icon.png` — mod icon.

No other `.so` / `.dll` / `.jar` libraries are vendored.
