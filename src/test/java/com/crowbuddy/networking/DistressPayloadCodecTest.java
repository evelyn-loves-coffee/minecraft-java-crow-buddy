package com.crowbuddy.networking;

import com.crowbuddy.test.BootstrapTest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DistressPayloadCodecTest extends BootstrapTest {

    private RegistryFriendlyByteBuf createBuffer(ByteBuf byteBuf) {
        return new RegistryFriendlyByteBuf(byteBuf, RegistryAccess.EMPTY);
    }

    @Test
    void testTypeId() {
        assertEquals(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("crowbuddy", "distress"),
            DistressPayload.TYPE.id()
        );
    }

    @Test
    void testCodecRoundTrip() {
        net.minecraft.core.BlockPos originalPos = new net.minecraft.core.BlockPos(100, 64, -200);
        DistressPayload original = new DistressPayload(42, originalPos, 7);

        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf encodeBuf = createBuffer(buf);
        DistressPayload.CODEC.encode(encodeBuf, original);

        ByteBuf decodeByteBuf = Unpooled.copiedBuffer(buf);
        RegistryFriendlyByteBuf decodeBuf = createBuffer(decodeByteBuf);
        DistressPayload decoded = DistressPayload.CODEC.decode(decodeBuf);

        assertEquals(original.entityId(), decoded.entityId());
        assertEquals(original.targetPos(), decoded.targetPos());
        assertEquals(original.sourceId(), decoded.sourceId());
    }

    @Test
    void testCodecRoundTripZeroValues() {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, 0, 0);
        DistressPayload original = new DistressPayload(0, pos, 0);

        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf encodeBuf = createBuffer(buf);
        DistressPayload.CODEC.encode(encodeBuf, original);

        ByteBuf decodeByteBuf = Unpooled.copiedBuffer(buf);
        RegistryFriendlyByteBuf decodeBuf = createBuffer(decodeByteBuf);
        DistressPayload decoded = DistressPayload.CODEC.decode(decodeBuf);

        assertEquals(0, decoded.entityId());
        assertEquals(new net.minecraft.core.BlockPos(0, 0, 0), decoded.targetPos());
        assertEquals(0, decoded.sourceId());
    }

    @Test
    void testCodecRoundTripNegativeCoordinates() {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(-1000, -64, -1000);
        DistressPayload original = new DistressPayload(9999, pos, 1);

        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf encodeBuf = createBuffer(buf);
        DistressPayload.CODEC.encode(encodeBuf, original);

        ByteBuf decodeByteBuf = Unpooled.copiedBuffer(buf);
        RegistryFriendlyByteBuf decodeBuf = createBuffer(decodeByteBuf);
        DistressPayload decoded = DistressPayload.CODEC.decode(decodeBuf);

        assertEquals(9999, decoded.entityId());
        assertEquals(-1000, decoded.targetPos().getX());
        assertEquals(-64, decoded.targetPos().getY());
        assertEquals(-1000, decoded.targetPos().getZ());
        assertEquals(1, decoded.sourceId());
    }

    @Test
    void testCodecRoundTripBoundaryCoordinates() {
        // BlockPos uses 26 bits for X/Z (max ~33M) and 12 bits for Y (max ~2K)
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(100000, 2000, 100000);
        DistressPayload original = new DistressPayload(Integer.MAX_VALUE, pos, Integer.MAX_VALUE);

        ByteBuf buf = Unpooled.buffer();
        RegistryFriendlyByteBuf encodeBuf = createBuffer(buf);
        DistressPayload.CODEC.encode(encodeBuf, original);

        ByteBuf decodeByteBuf = Unpooled.copiedBuffer(buf);
        RegistryFriendlyByteBuf decodeBuf = createBuffer(decodeByteBuf);
        DistressPayload decoded = DistressPayload.CODEC.decode(decodeBuf);

        assertEquals(Integer.MAX_VALUE, decoded.entityId());
        assertEquals(100000, decoded.targetPos().getX());
        assertEquals(2000, decoded.targetPos().getY());
        assertEquals(100000, decoded.targetPos().getZ());
        assertEquals(Integer.MAX_VALUE, decoded.sourceId());
    }
}
