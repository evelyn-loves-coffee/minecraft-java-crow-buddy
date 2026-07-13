package com.crowbuddy.client;

import com.crowbuddy.client.ModClientEntities;
import net.fabricmc.api.ClientModInitializer;

public class CrowBuddyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModClientEntities.register();
	}
}
