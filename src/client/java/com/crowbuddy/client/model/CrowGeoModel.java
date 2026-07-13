package com.crowbuddy.client.model;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public final class CrowGeoModel extends GeoModel<CrowEntity> {
    private static final Identifier MODEL = CrowBuddy.id("entity/crow");
    private static final Identifier ANIMATIONS = CrowBuddy.id("entity/crow");
    private static final Identifier TEXTURE = CrowBuddy.id("textures/entity/crow");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getAnimationResource(CrowEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }
}
