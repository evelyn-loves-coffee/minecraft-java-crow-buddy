package com.crowbuddy.client.networking;

import com.crowbuddy.networking.DistressPayload;
import com.crowbuddy.networking.ScavengePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class ModClientNetworking {

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(DistressPayload.TYPE, ModClientNetworking::handleDistress);
        ClientPlayNetworking.registerGlobalReceiver(ScavengePayload.TYPE, ModClientNetworking::handleScavenge);
    }

    private static void handleDistress(DistressPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity source = mc.level.getEntity(payload.sourceId());
            if (source != null && source.isAlive()) {
                // Client-side reaction: visual/audio cue for distress source
            }
        });
    }

    private static void handleScavenge(ScavengePayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity crow = mc.level.getEntity(payload.crowId());
            if (crow != null && crow.isAlive()) {
                // Client-side reaction: update carried item visualization
            }
        });
    }
}
