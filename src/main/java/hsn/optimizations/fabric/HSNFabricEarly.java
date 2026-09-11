package hsn.optimizations.fabric;

import hsn.optimizations.HSNEarly;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class HSNFabricEarly implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		HSNEarly.run();
	}
}
