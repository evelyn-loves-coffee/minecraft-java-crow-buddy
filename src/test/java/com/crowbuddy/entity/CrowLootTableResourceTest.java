package com.crowbuddy.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowLootTableResourceTest {
    private static final String LOOT_TABLE =
        "/data/crowbuddy/loot_table/entities/crow.json";
    private static final String LEGACY_LOOT_TABLE =
        "/data/crowbuddy/loot_tables/entities/crow.json";

    @Test
    void packagesCrowLootTableAtCurrentRegistryPath() {
        assertNotNull(getClass().getResource(LOOT_TABLE));
        assertNull(getClass().getResource(LEGACY_LOOT_TABLE));
    }

    @Test
    void usesMinecraft26LootFunctionIdentifiers() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(LOOT_TABLE)) {
            assertNotNull(stream);
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(json.contains("minecraft:enchanted_count_increase"));
            assertTrue(json.contains("minecraft:random_chance_with_enchanted_bonus"));
            assertFalse(json.contains("minecraft:looting_enchant"));
            assertFalse(json.contains("minecraft:luck_or_random"));
        }
    }
}
