# Architecture — HSN-Optimizations 4.0

## Frame path

Mixins read `HotPath` primitives, never the live `HSNConfig` object.
`HotPath.rebuild` / `publishScale` swap an immutable snapshot.

`AdaptiveCuller` publishes a 0.25–1.0 scale. `publishScale` skips the
rebuild when the new scale is within 0.005 of the current one.

## Particles

`ParticleManagerMixin` filters by distance, facing, horizon, and keep-chance.
`ParticleEngineCapMixin` calls `ParticleLive.acceptSpawn()` and refuses the
add when the live count is at the budget.
`ParticleTickMixin` drops particles that have walked out of range and
releases the live slot on `remove()`.

## Sky + world scale

`RenderScale` swaps a scaled `MainTarget` for the world pass and blits back.
`SkyNativeMixin` pauses that swap so `SkyRenderer` writes the dome onto the
native target. Empty pause methods delete the sky on blit — those stubs
are gone.

## Natives

Java talks to C / C++ / ASM / Rust through Panama FFM only.
No JNI. No novelty-language RTS on the frame loop.

`BufferStorageMixin` still uses `sun.misc.Unsafe` to poke a final LWJGL
field when Framepace is absent. That path is isolated and can fail open
on JDK 25. It is the only Unsafe site.

## Threads

Integrated-server mixins write `PathfindingStats`. The client F3 overlay
reads it. Those fields are volatile.
