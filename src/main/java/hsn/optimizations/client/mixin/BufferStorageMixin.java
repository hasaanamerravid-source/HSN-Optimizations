package hsn.optimizations.client.mixin;

import hsn.optimizations.client.optimize.GpuPacing;
import org.lwjgl.opengl.GLCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Force mutable GL buffers on Intel HD 2000–4000 when frame-pacing is on.
 * <p>
 * {@code GLCapabilities.GL_ARB_buffer_storage} is a {@code final} LWJGL field,
 * so it cannot be assigned from Java. {@code BufferStorage.create} also consults
 * {@code GlDevice.USE_GL_ARB_buffer_storage} (a non-final static), which is the
 * flag this mixin flips.
 * <p>
 * Must stay {@code @Inject}-only. MixinExtras 0.5.4 (Fabric Loader 0.19.3)
 * wraps every {@code @Redirect} in {@code FactoryRedirectWrapperMixinTransformer}
 * and crashes with {@code ClassCastException: ArrayList cannot be cast to
 * AnnotationNode} because {@code @Redirect.at} is an array.
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.BufferStorage")
public class BufferStorageMixin {

	// create(...) returns BufferStorage. Mixin requires CallbackInfoReturnable
	// for non-void targets; CallbackInfo crashes apply with InvalidInjectionException.
	@Inject(method = "create", at = @At("HEAD"), require = 0)
	private static void hsn$mutableWhenPacingNeedsIt(GLCapabilities capabilities, Set<String> enabledExtensions, CallbackInfoReturnable<?> cir) {
		if (GpuPacing.shouldUseMutableBuffers()) {
			hsn$disableArbBufferStorage(capabilities);
		}
	}

	@Unique
	private static void hsn$disableArbBufferStorage(GLCapabilities capabilities) {
		// Minecraft 1.21.6+/26.x gate: if (caps.GL_ARB_buffer_storage && GlDevice.USE_GL_ARB_buffer_storage)
		hsn$setStaticBoolean("com.mojang.blaze3d.opengl.GlDevice", "USE_GL_ARB_buffer_storage", false);
		if (capabilities != null) {
			hsn$pokeBoolean(capabilities, "GL_ARB_buffer_storage", false);
		}
	}

	@Unique
	private static void hsn$setStaticBoolean(String className, String fieldName, boolean value) {
		try {
			Field field = Class.forName(className).getDeclaredField(fieldName);
			field.setAccessible(true);
			if (Modifier.isStatic(field.getModifiers())) {
				field.setBoolean(null, value);
			}
		} catch (Throwable ignored) {
			// Flag name / visibility may differ on older snapshots; Unsafe path below still runs.
		}
	}

	@Unique
	private static void hsn$pokeBoolean(Object instance, String fieldName, boolean value) {
		try {
			Field field = instance.getClass().getField(fieldName);
			field.setAccessible(true);
			try {
				field.setBoolean(instance, value);
				return;
			} catch (IllegalAccessException ignored) {
				// final instance field: fall through to Unsafe
			}
			hsn$unsafePutBoolean(instance, field, value);
		} catch (Throwable ignored) {
		}
	}

	@Unique
	private static void hsn$unsafePutBoolean(Object instance, Field field, boolean value) {
		try {
			Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			Object unsafe = unsafeField.get(null);
			long offset = (Long) unsafe.getClass()
					.getMethod("objectFieldOffset", Field.class)
					.invoke(unsafe, field);
			unsafe.getClass()
					.getMethod("putBoolean", Object.class, long.class, boolean.class)
					.invoke(unsafe, instance, offset, value);
		} catch (Throwable ignored) {
		}
	}
}
