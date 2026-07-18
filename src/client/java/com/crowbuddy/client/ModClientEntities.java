package com.crowbuddy.client;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.client.renderer.CrowRenderer;
import com.crowbuddy.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ModClientEntities {
    public static void register() {
        CrowBuddy.LOGGER.info("Registering Client Entities for " + CrowBuddy.MOD_ID);
        EntityRendererRegistry.register(ModEntities.CROW, CrowRenderer::new);
    }
}
