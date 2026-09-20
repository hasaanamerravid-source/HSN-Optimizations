package hsn.optimizations.client.mixin;

/**
 * Not registered as a mixin. MixinExtras 0.5.4 crashes on startup when a
 * {@code @Redirect} on {@code Minecraft} uses a method-name array:
 * {@code ClassCastException: ArrayList cannot be cast to AnnotationNode}
 * in {@code FactoryRedirectWrapperMixinTransformer}.
 * <p>
 * Smart yield is an {@code @Inject} on {@code runTick} in
 * {@link MinecraftUnfocusedMixin}.
 */
public final class RenderPacingMixin {
	private RenderPacingMixin() {
	}
}
