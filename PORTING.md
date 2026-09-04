# Loader support

Architectury is no longer a dependency. Server hooks use Fabric API
`ServerLifecycleEvents` and `ServerTickEvents`.

| Loader | Status |
|---|---|
| Fabric | Current build |
| Quilt | Load the Fabric jar with Quilted Fabric API. No second source set. |
| NeoForge | Not started. Mixin targets and a NeoForge source set would still be required. |
| LexForge | Not started. |

Client mixins, Sodium, Mod Menu, and YACL stay Fabric-facing.
