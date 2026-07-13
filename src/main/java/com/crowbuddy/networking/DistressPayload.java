package com.crowbuddy.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record DistressPayload(int entityId, BlockPos targetPos, int sourceId) implements CustomPacketPayload {
    public static final Type<DistressPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("crowbuddy", "distress")
    );
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DistressPayload> CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.entityId);
            buf.writeLong(payload.targetPos.asLong());
            buf.writeVarInt(payload.sourceId);
        },
        buf -> new DistressPayload(
            buf.readVarInt(),
            BlockPos.of(buf.readLong()),
            buf.readVarInt()
        )
    );

    @NotNull
    @Override
    public Type<DistressPayload> type() {
        return TYPE;
    }
}
