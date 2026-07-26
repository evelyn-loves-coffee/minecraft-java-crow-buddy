package com.crowbuddy.worldgen;

import com.crowbuddy.registry.ModEntities;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Predicate;

public class CrowSpawning {

    public static void initialize() {
        var landBiomes = BiomeSelectors.foundInOverworld()
                .and(Predicate.not(BiomeSelectors.tag(BiomeTags.IS_OCEAN)))
                .and(Predicate.not(BiomeSelectors.tag(BiomeTags.IS_RIVER)));

        BiomeModifications.addSpawn(
                landBiomes,
                MobCategory.CREATURE,
                ModEntities.CROW,
                1,
                1,
                2
        );
    }
}
