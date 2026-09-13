# Storefront checklist — HSN-Optimizations 4.0

## Do this on the project page

- Tick Modrinth **Contains AI-generated content**. A large part of this
  repo was written with an assistant. Rule 6.1 requires the disclosure.
  Do not tick it and then claim the opposite in the description.
- Do **not** use an AI image for the icon, gallery, or description.
  The jar icon is a flat HSN tile drawn in code, not a generated gem.
- Tags that fit: Optimization, Utility.  
  Tags that do **not** fit: World Generation, Management, Game Mechanics
  (unless you only mean cull rules). Locate-cache is not world gen.
- Environment: client-focused. Dedicated servers load a few no-op-safe
  mixins. Do not advertise “world generation” or “server optimization pack”.
- Description must match the jar: distance culling, particle budgets,
  optional Linux natives that fall back to Java. No FPS guarantees.

## Upload

- File: `hsn-optimizations-4.0.0.jar` only.
- Not the source zip. Not `-PbundleNatives=true`.
- Sources jar may be an additional file.
- License on the page: MIT (same as `LICENSE`).

## Already clean

- No telemetry, webhooks, or hidden downloads.
- No cheat features (x-ray, aim, fly, PvP assist).
- No bundled `.so` / `.dll` / `.wasm` in the default jar.
- No YACL / Cloth hard depend.
- Common mixins fail open when the method name moved.

## Mojang usage guidelines

- Distribute the mod, never a modded client jar.
- Keep it free if you follow the Minecraft EULA “don’t sell mods” line.
- Do not use the official Minecraft logo on the page.
