package hsn.modod.client.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor for the sound event id. SoundInstance is an interface; this
 * Invoker is the supported way to call getLocation() without reflection.
 */
@Mixin(SoundInstance.class)
public interface SoundInstanceAccess {

	@Invoker("getIdentifier")
	Identifier hsn$getIdentifier();
}
