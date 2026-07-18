package com.crowbuddy.worldgen;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.worldgen.feature.CrowNestFeature;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ModFeatures {

    public static final Feature<NoneFeatureConfiguration> CROW_NEST = new CrowNestFeature(
            NoneFeatureConfiguration.CODEC
    );

    public static void register() {
        CrowBuddy.LOGGER.info("Registering Features for " + CrowBuddy.MOD_ID);
        net.minecraft.core.Registry.register(BuiltInRegistries.FEATURE, CrowBuddy.id("crow_nest"), CROW_NEST);
    }
}
