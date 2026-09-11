package hsn.optimizations.client.access;

/**
 * Duck interface implemented by {@code GameRendererTargetMixin}.
 * <p>
 * Must live outside {@code hsn.optimizations.client.mixin} — Mixin refuses to load
 * any class in a registered mixin package as a normal type
 * ({@code IllegalClassLoadError}).
 */
public interface HSNMainTarget {
	void hsn$setMainRenderTarget(Object target);
}
