package com.crowbuddy.client.model;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public final class CrowGeoModel extends GeoModel<CrowEntity> {
    private static final Identifier MODEL_ADULT = CrowBuddy.id("geckolib/models/entity/adult_crow");
    private static final Identifier MODEL_BABY = CrowBuddy.id("geckolib/models/entity/crow_baby");
    private static final Identifier ANIMATIONS_ADULT = CrowBuddy.id("geckolib/animations/entity/adult_crow");
    private static final Identifier ANIMATIONS_BABY = CrowBuddy.id("geckolib/animations/entity/crow");
    private static final Identifier TEXTURE_ADULT = CrowBuddy.id("textures/entity/adult_crow");
    private static final Identifier TEXTURE_BABY = CrowBuddy.id("textures/entity/crow");
    private static final DataTicket<Boolean> IS_BABY = DataTicket.create("is_baby", Boolean.class);

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        Boolean isBaby = renderState.getOrDefaultGeckolibData(IS_BABY, false);
        return isBaby ? MODEL_BABY : MODEL_ADULT;
    }

    @Override
    public Identifier getAnimationResource(CrowEntity animatable) {
        return animatable.isBaby() ? ANIMATIONS_BABY : ANIMATIONS_ADULT;
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
