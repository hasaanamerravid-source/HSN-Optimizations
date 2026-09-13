package hsn.optimizations.fabric;

import hsn.optimizations.client.HSNOptimizationsClient;
import net.fabricmc.api.ClientModInitializer;

public final class HSNFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		HSNOptimizationsClient.init();
	}
}
