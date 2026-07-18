package com.crowbuddy.worldgen;

import com.crowbuddy.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class CrowSpawning {

    public static final ResourceKey<PlacedFeature> CROW_NEST_PLACED = ResourceKey.create(
            net.minecraft.core.registries.Registries.PLACED_FEATURE,
            com.crowbuddy.CrowBuddy.id("crow_nest")
    );

    public static void initialize() {
        BiomeModifications.addSpawn(
                BiomeSelectors.foundInOverworld(),
                MobCategory.CREATURE,
                ModEntities.CROW,
                2,
                2,
                4
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.VEGETAL_DECORATION,
                CROW_NEST_PLACED
        );
    }
}
