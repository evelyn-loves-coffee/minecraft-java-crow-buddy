package com.crowbuddy.client.model;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public final class CrowGeoModel extends GeoModel<CrowEntity> {
    private static final Identifier MODEL = CrowBuddy.id("entity/crow");
    private static final Identifier MODEL_BABY = CrowBuddy.id("entity/crow_baby");
    private static final Identifier ANIMATIONS = CrowBuddy.id("entity/crow");
    private static final Identifier TEXTURE_ADULT = CrowBuddy.id("textures/entity/crow");
    private static final Identifier TEXTURE_BABY = CrowBuddy.id("textures/entity/crow_baby");
    private static final DataTicket<Boolean> IS_BABY = DataTicket.create("is_baby", Boolean.class);

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        Boolean isBaby = renderState.getOrDefaultGeckolibData(IS_BABY, false);
        return isBaby ? MODEL_BABY : MODEL;
    }

    @Override
    public Identifier getAnimationResource(CrowEntity animatable) {
        return ANIMATIONS;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        Boolean isBaby = renderState.getOrDefaultGeckolibData(IS_BABY, false);
        return isBaby ? TEXTURE_BABY : TEXTURE_ADULT;
    }

    @Override
    public void addAdditionalStateData(CrowEntity animatable, Object handler, GeoRenderState renderState) {
        super.addAdditionalStateData(animatable, handler, renderState);
        renderState.addGeckolibData(IS_BABY, animatable.isBaby());
    }
}
