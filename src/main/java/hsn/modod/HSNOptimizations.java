package hsn.modod;

import hsn.modod.config.HSNConfig;
import hsn.modod.optimize.ItemEntityOptimizer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HSNOptimizations implements ModInitializer {

	public static final String MOD_ID = "hsn-optimizations";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		HSNConfig.load();
		ItemEntityOptimizer.init();
		LOGGER.info("HSN-Optimizations {} loaded", HSNConfig.modVersionLabel);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
