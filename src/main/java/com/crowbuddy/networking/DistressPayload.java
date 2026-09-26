package com.crowbuddy.networking;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Server-to-client distress notification. Carries only the crow's entity ID:
 * the distress sound itself is played server-side (positional), so the client
 * only renders particles on the crow.
 */
public record DistressPayload(int sourceId) implements CustomPacketPayload {
    public static final Type<DistressPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath("crowbuddy", "distress")
    );
    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DistressPayload> CODEC = StreamCodec.of(
        (buf, payload) -> buf.writeVarInt(payload.sourceId),
        buf -> new DistressPayload(buf.readVarInt())
    );

    @NotNull
    @Override
    public Type<DistressPayload> type() {
        return TYPE;
    }
}
