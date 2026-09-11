package hsn.optimizations.client;

import com.mojang.blaze3d.platform.InputConstants;
import hsn.optimizations.HSNOptimizations;
import hsn.optimizations.client.compat.ClientScreens;
import hsn.optimizations.client.config.HSNConfigScreen;
import hsn.optimizations.config.HSNConfig;
import hsn.optimizations.optimize.HotPath;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Optional keybinds. Both start <b>unbound</b>. Bind them in
 * Options → Controls → HSN Optimizations (or the Fabric/Sodium controls
 * page). Nothing is reserved until the player picks a key.
 */
public final class HSNKeybinds {

	private static KeyMapping killSwitchKey;
	private static KeyMapping settingsKey;

	private HSNKeybinds() {
	}

	public static void register() {
		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(HSNOptimizations.MOD_ID, "keys"));

		killSwitchKey = registerMapping(unbound(
				"key.hsn-optimizations.killswitch", category));
		settingsKey = registerMapping(unbound(
				"key.hsn-optimizations.settings", category));

		HSNOptimizations.LOGGER.info(
				"HSN keys registered unbound. Bind them in Controls → HSN Optimizations.");
	}

	private static KeyMapping unbound(String translation, KeyMapping.Category category) {
		int unknown = unknownCode();
		try {
			return new KeyMapping(translation, InputConstants.Type.KEYSYM, unknown, category);
		} catch (Throwable first) {
			try {
				return KeyMapping.class
						.getConstructor(String.class, int.class, KeyMapping.Category.class)
						.newInstance(translation, unknown, category);
			} catch (Throwable ignored) {
				return new KeyMapping(translation, InputConstants.Type.KEYSYM, -1, category);
			}
		}
	}

	private static int unknownCode() {
		try {
			Object key = InputConstants.class.getField("UNKNOWN").get(null);
			if (key instanceof InputConstants.Key input) {
				return input.getValue();
			}
		} catch (Throwable ignored) {
		}
		return -1;
	}

	private static KeyMapping registerMapping(KeyMapping mapping) {
		KeyMapping registered = tryFabricRegister(mapping);
		if (registered == null) {
			registered = mapping;
		}
		tryVanillaList(registered);
		return registered;
	}

	private static KeyMapping tryFabricRegister(KeyMapping mapping) {
		String[] types = {
				"net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper",
				"net.fabricmc.fabric.api.client.keybinding.KeyBindingHelper",
				"net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents"
		};
		for (String name : types) {
			if (!name.endsWith("KeyBindingHelper")) {
				continue;
			}
			try {
				Class<?> type = Class.forName(name);
				Method method = type.getMethod("registerKeyBinding", KeyMapping.class);
				Object out = method.invoke(null, mapping);
				if (out instanceof KeyMapping key) {
					return key;
				}
				return mapping;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	/** So the row still appears in vanilla Controls when Fabric helper is missing. */
	private static void tryVanillaList(KeyMapping mapping) {
		try {
			Minecraft client = Minecraft.getInstance();
			if (client == null || client.options == null) {
				return;
			}
			Object options = client.options;
			for (Field field : options.getClass().getFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Object value = field.get(options);
				if (value instanceof KeyMapping[] array) {
					for (KeyMapping existing : array) {
						if (existing == mapping) {
							return;
						}
					}
					KeyMapping[] next = new KeyMapping[array.length + 1];
					System.arraycopy(array, 0, next, 0, array.length);
					next[array.length] = mapping;
					field.set(options, next);
					return;
				}
				if (value instanceof List<?> list) {
					if (list.isEmpty() || list.get(0) instanceof KeyMapping) {
						@SuppressWarnings("unchecked")
						List<Object> raw = (List<Object>) list;
						if (!raw.contains(mapping)) {
							raw.add(mapping);
						}
						return;
					}
				}
			}
		} catch (Throwable ignored) {
		}
	}

	static void tick(Minecraft client) {
		if (client == null) {
			return;
		}
		if (bound(killSwitchKey)) {
			while (consume(killSwitchKey)) {
				HSNConfig cfg = HSNConfig.get();
				cfg.modEnabled = !cfg.modEnabled;
				cfg.save();
				HotPath.rebuild(cfg);
				String state = cfg.modEnabled ? "ON" : "OFF (vanilla rendering)";
				HSNOptimizations.LOGGER.info("HSN master switch {}", state);
				if (client.player != null) {
					client.player.sendSystemMessage(Component.literal("HSN Optimizations: " + state));
				}
			}
		}
		if (bound(settingsKey)) {
			while (consume(settingsKey)) {
				if (ClientScreens.current() == null) {
					ClientScreens.open(HSNConfigScreen.create(null));
				}
			}
		}
	}

	private static boolean bound(KeyMapping key) {
		if (key == null) {
			return false;
		}
		try {
			Method unbound = key.getClass().getMethod("isUnbound");
			Object value = unbound.invoke(key);
			if (value instanceof Boolean flag) {
				return !flag;
			}
		} catch (Throwable ignored) {
		}
		try {
			Method saved = key.getClass().getMethod("getKey");
			Object keyObj = saved.invoke(key);
			if (keyObj instanceof InputConstants.Key input) {
				return input.getValue() != unknownCode() && input.getValue() != -1;
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	private static boolean consume(KeyMapping key) {
		try {
			return key.consumeClick();
		} catch (Throwable ignored) {
			return false;
		}
	}
}
