package hsn.modod.client.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Previously added a floating "HSN Optimizations…" button on Sodium's Video Settings.
 * Disabled because it overlapped the "Buy us a coffee!" button / top bar.
 * HSN remains fully available via the official Sodium Config API tab and Mod Menu.
 * All other features are unchanged.
 */
@Pseudo
@Mixin(targets = {
		"net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen",
		"net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI",
		"me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI",
		"net.caffeinemc.mods.sodium.client.gui.SodiumVideoOptionsScreen"
}, remap = false)
public abstract class SodiumOptionsButtonMixin extends Screen {

	protected SodiumOptionsButtonMixin(Component title) {
		super(title);
	}

	@Inject(method = {"init", "rebuildGui", "createButtons"}, at = @At("RETURN"), require = 0)
	private void hsn$addButton(CallbackInfo ci) {
		// no-op — floating button removed (was overlapping "Buy us a coffee!")
	}
}
