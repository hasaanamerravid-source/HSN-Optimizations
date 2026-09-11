Optional Linux kernels. They are **not** copied into the CurseForge jar.

`./gradlew build` is Java-only (Windows, macOS, and CurseForge-safe).
To embed them for a Linux sideload: `./gradlew build -PbundleNatives=true`
