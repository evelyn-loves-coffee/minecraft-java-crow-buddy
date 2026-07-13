package com.crowbuddy.registry;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import net.minecraft.world.entity.EntityType;

public class ModEntities {
	public static final EntityType<CrowEntity> CROW = CrowBuddy.CROW;

	public static void register() {
		CrowBuddy.LOGGER.info("Registering Entities for " + CrowBuddy.MOD_ID);
	}
}
