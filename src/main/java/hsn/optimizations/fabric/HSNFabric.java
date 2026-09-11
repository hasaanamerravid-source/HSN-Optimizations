package hsn.optimizations.fabric;

import hsn.optimizations.HSNOptimizations;
import net.fabricmc.api.ModInitializer;

public final class HSNFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		HSNOptimizations.init();
	}
}
