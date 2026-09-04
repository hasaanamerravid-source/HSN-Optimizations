# Access pattern

HSN does not use reflection to read `SoundInstance` identifiers.

- Preferred: call the public API `SoundInstance.getIdentifier()`.
- Fallback: Mixin `@Invoker` in `SoundInstanceAccess` (`@Invoker("getIdentifier")`).
- Do not use `Class.getMethod(...)` / `Method.invoke(...)` for this.

If a mapping rename breaks `getIdentifier()`, update the Invoker name. Do not add reflection.
