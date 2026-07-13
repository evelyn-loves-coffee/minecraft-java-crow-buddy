package com.crowbuddy.client;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.client.renderer.CrowRenderer;
import com.crowbuddy.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ModClientEntities {
    public static void register() {
        CrowBuddy.LOGGER.info("Registering Client Entities for " + CrowBuddy.MOD_ID);
        try {
            Class<?> registry = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry");
            EntityRendererProvider<com.crowbuddy.entity.CrowEntity> provider = context -> new CrowRenderer(context);
            registry.getMethod("register", net.minecraft.world.entity.EntityType.class, EntityRendererProvider.class)
                    .invoke(null, (Object) ModEntities.CROW, (Object) provider);
        } catch (Exception e) {
            CrowBuddy.LOGGER.error("Failed to register Crow renderer", e);
        }
    }
}
