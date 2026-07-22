package com.crowbuddy.worldgen;

import com.crowbuddy.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.entity.MobCategory;

public class CrowSpawning {

    public static void initialize() {
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                ModEntities.CROW,
                2,
                2,
                4
        );
    }
}
