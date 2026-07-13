package com.crowbuddy.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class ModNetworking {

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(DistressPayload.TYPE, DistressPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ScavengePayload.TYPE, ScavengePayload.CODEC);
    }

    public static void sendDistress(ServerPlayer player, int entityId, net.minecraft.core.BlockPos targetPos, int sourceId) {
        if (ServerPlayNetworking.canSend(player, DistressPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DistressPayload(entityId, targetPos, sourceId));
        }
    }

    public static void sendScavenge(ServerPlayer player, int crowId, net.minecraft.world.item.ItemStack carriedItem) {
        if (ServerPlayNetworking.canSend(player, ScavengePayload.TYPE)) {
            ServerPlayNetworking.send(player, new ScavengePayload(crowId, carriedItem));
        }
    }
}
