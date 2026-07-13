package com.crowbuddy.networking;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public record ScavengePayload(int crowId, ItemStack carriedItem) implements CustomPacketPayload {
    public static final Type<ScavengePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("crowbuddy", "scavenge")
    );
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ScavengePayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.crowId);
            ItemStack.STREAM_CODEC.encode(buf, payload.carriedItem);
        },
        buf -> new ScavengePayload(
            buf.readVarInt(),
            ItemStack.STREAM_CODEC.decode(buf)
        )
    );

    @NotNull
    @Override
    public Type<ScavengePayload> type() {
        return TYPE;
    }
}
