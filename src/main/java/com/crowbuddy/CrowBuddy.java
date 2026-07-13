package com.crowbuddy;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrowBuddy implements ModInitializer {
    public static final String MOD_ID = "crowbuddy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final EntityType<CrowEntity> CROW = EntityType.Builder.<CrowEntity>of(CrowEntity::new, MobCategory.CREATURE)
        .sized(0.4f, 0.6f)
        .clientTrackingRange(64)
        .updateInterval(1)
        .build(ResourceKey.create(Registries.ENTITY_TYPE, id("crow")));

    public static final Item BLACK_OIL_SUNFLOWER_SEEDS = new Item(new Item.Properties());

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Crow Buddy world!");
        net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, id("crow"), CROW);
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, id("black_oil_sunflower_seeds"), BLACK_OIL_SUNFLOWER_SEEDS);
        ModItems.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
