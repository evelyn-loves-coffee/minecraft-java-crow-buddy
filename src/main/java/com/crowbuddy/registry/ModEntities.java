package com.crowbuddy.registry;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
	public static final EntityType<CrowEntity> CROW = EntityType.Builder.<CrowEntity>of(CrowEntity::new, MobCategory.CREATURE)
			.sized(0.4f, 0.6f)
			.clientTrackingRange(64)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, CrowBuddy.id("crow")));

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Entities for " + CrowBuddy.MOD_ID);
		net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, CrowBuddy.id("crow"), CROW);
	}
}
