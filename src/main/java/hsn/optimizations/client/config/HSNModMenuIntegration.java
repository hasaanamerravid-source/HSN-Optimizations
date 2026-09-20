package hsn.optimizations.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import hsn.optimizations.client.compat.HSNConfigEntry;

public class HSNModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return HSNConfigEntry::createScreen;
	}
}
