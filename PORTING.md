# Loader support

Architectury API **21.0.7** exists for **Fabric 26.2** and **NeoForge 26.2**.

| Loader | Status |
|---|---|
| Fabric | Current build |
| Quilt | Load the Fabric jar with Quilted Fabric API. No second source set. |
| NeoForge | Same Architectury lifecycle events already used. Mixin targets and mods.toml still need a NeoForge source set. |
| LexForge | No 26.2 Architectury artifact. Not started. |

Server start/stop/tick hooks go through `dev.architectury.event.events.common` so they do not have to be rewritten for a NeoForge module later.

Client mixins, Sodium, Mod Menu, and YACL stay Fabric-facing until that module exists. An empty `neoforge/` tree is not included.
