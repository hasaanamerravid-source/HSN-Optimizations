# Origin audit — HSN-Optimizations 4.0

No third-party license headers were found in `src/`. Nothing is a paste of
Sodium, Lithium, Iris, EntityCulling, Dynamic FPS, or Resolution Control
source.

## Compile stubs (not shipped behavior)

`src/sodiumStubs/` is two tiny compile-only facades (`RenderSection`,
`WriteQueue`) so this mod can mention Sodium types without bundling Sodium.
That is normal Fabric practice. Those classes are not copied Sodium engines.

## Same problem, own mixins

These features exist in other mods. HSN implements them with its own mixins
and config fields. That is overlap of *idea*, not a copied file.

| HSN piece | Other mod that does a similar job |
|---|---|
| Entity / particle distance cull | EntityCulling, MoreCulling |
| Unfocused FPS cap | Dynamic FPS |
| World framebuffer scale | Resolution Control, RenderScale |
| Fog / toast / beacon deferrals | Sodium Extra (HSN *backs off* when that mod is present) |
| Pathfinding interval | Lithium (HSN *backs off* when Lithium is present) |
| Horizon / section skip | Sodium’s own visibility graph, various cull mods |

`ResolutionControlCompat` only detects those mods and turns HSN scale off.
It does not include their code.

## Removed in 4.0 because it was theater or a storefront problem

- Novelty languages (Go / Fortran / Haskell / …)
- Prebuilt `.so` blobs
- YACL + Cloth Config UI
- Unused DX12 / OpenCL stubs

## Settings

The in-game menu is a vanilla `Screen` (`HSNConfigScreen`). No YACL. No Cloth.
Mod Menu and a Sodium “Open HSN settings” button only open that screen.
