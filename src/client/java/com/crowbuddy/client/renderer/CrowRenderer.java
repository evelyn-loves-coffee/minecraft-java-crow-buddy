package com.crowbuddy.client.renderer;

import com.crowbuddy.client.model.CrowGeoModel;
import com.crowbuddy.entity.CrowEntity;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CrowRenderer extends GeoEntityRenderer<CrowEntity, LivingEntityRenderState> {
    public CrowRenderer(EntityRendererProvider.Context context) {
        super(context, new CrowGeoModel());
    }
}
