package com.crowbuddy.client;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.client.renderer.CrowRenderer;
import com.crowbuddy.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class ModClientEntities {
    public static void register() {
        CrowBuddy.LOGGER.info("Registering Client Entities for " + CrowBuddy.MOD_ID);
        EntityRenderers.register(ModEntities.CROW, CrowRenderer::new);
    }
}
