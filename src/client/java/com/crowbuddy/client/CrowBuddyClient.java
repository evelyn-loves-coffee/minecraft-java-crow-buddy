package com.crowbuddy.client;

import com.crowbuddy.client.ModClientEntities;
import com.crowbuddy.client.networking.ModClientNetworking;
import net.fabricmc.api.ClientModInitializer;

public class CrowBuddyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModClientEntities.register();
		ModClientNetworking.registerReceivers();
	}
}
