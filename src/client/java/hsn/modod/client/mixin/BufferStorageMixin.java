package hsn.modod.client.mixin;

import hsn.modod.client.optimize.GpuPacing;
import org.lwjgl.opengl.GLCapabilities;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.mojang.blaze3d.opengl.BufferStorage")
public class BufferStorageMixin {

	@Redirect(
			method = "create",
			at = @At(
					value = "FIELD",
					opcode = Opcodes.GETFIELD,
					target = "Lorg/lwjgl/opengl/GLCapabilities;GL_ARB_buffer_storage:Z"
			),
			require = 0
	)
	private static boolean hsn$mutableWhenPacingNeedsIt(GLCapabilities capabilities) {
		if (GpuPacing.shouldUseMutableBuffers()) {
			return false;
		}
		return capabilities.GL_ARB_buffer_storage;
	}
}
