package com.crowbuddy.registry;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

public class ModEntities {
	public static final EntityType<CrowEntity> CROW = EntityType.Builder.<CrowEntity>of(CrowEntity::new, MobCategory.CREATURE)
			.sized(0.4f, 0.6f)
			.clientTrackingRange(64)
			.updateInterval(1)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, CrowBuddy.id("crow")));

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Entities for " + CrowBuddy.MOD_ID);
		net.minecraft.core.Registry.register(BuiltInRegistries.ENTITY_TYPE, CrowBuddy.id("crow"), CROW);
		// Supported Fabric API (fabric-object-builder-api-v1): registers the default
		// attribute supplier without touching private DefaultAttributes fields.
		FabricDefaultAttributeRegistry.register(CROW, CrowEntity.createAttributes().build());
		// Fail fast: if the supplier is missing, every crow construction NPEs inside
		// the LivingEntity constructor. Better to crash mod init with a clear message.
		if (!DefaultAttributes.hasSupplier(CROW)) {
			throw new IllegalStateException("Default attributes were not registered for "
					+ CrowBuddy.id("crow") + "; the entity cannot be constructed.");
		}
	}
}
