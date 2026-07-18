package com.crowbuddy.networking;

import com.crowbuddy.test.BootstrapTest;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ScavengePayloadCodecTest extends BootstrapTest {

    @Test
    void testTypeId() {
        assertEquals(
            Identifier.fromNamespaceAndPath("crowbuddy", "scavenge"),
            ScavengePayload.TYPE.id()
        );
    }

    @Test
    void testTypeIdNamespaceAndPath() {
        assertEquals("crowbuddy", ScavengePayload.TYPE.id().getNamespace());
        assertEquals("scavenge", ScavengePayload.TYPE.id().getPath());
    }

    @Test
    void testCodecExists() {
        assertNotNull(ScavengePayload.CODEC);
    }
}
