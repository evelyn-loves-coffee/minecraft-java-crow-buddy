package com.crowbuddy.client.renderer;

import com.crowbuddy.entity.CrowEntity;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.builtin.BlockAndItemGeoLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Renders the scavenged item in the beak and a floating seed feeding prompt. */
final class CrowCarriedItemLayer
        extends BlockAndItemGeoLayer<CrowEntity, Void, LivingEntityRenderState> {
    private static final String MOUTH_BONE = "upper_beak";

    CrowCarriedItemLayer(EntityRendererProvider.Context context,
                         GeoRenderer<CrowEntity, Void, LivingEntityRenderState> renderer) {
        super(context, renderer);
    }

    @Override
    protected List<RenderData> getRelevantBones(CrowEntity crow, Void relatedObject,
                                                 LivingEntityRenderState renderState,
                                                 float partialTick) {
        ItemStack carried = crow.getCarriedItem();
        if (carried.isEmpty()) return List.of();

        List<RenderData> contents = new ArrayList<>(1);
        contents.add(itemData(MOUTH_BONE, carried, crow));
        return contents;
    }

    private RenderData itemData(String bone, ItemStack stack, CrowEntity crow) {
        ItemDisplayContext context = ItemDisplayContext.GROUND;
        return RenderData.item(
            bone, context,
            RenderUtil.createRenderStateForItem(stack, this.itemModelResolver, context, crow));
    }

    @Override
    public void addRenderData(CrowEntity crow, Void relatedObject,
                              LivingEntityRenderState renderState, float partialTick) {
        List<RenderData> contents = getRelevantBones(crow, relatedObject, renderState, partialTick);
        if (!contents.isEmpty()) renderState.addGeckolibData(CONTENTS, contents);
    }

    @Override
    protected void submitItemStackRender(PoseStack poseStack, GeoBone bone,
                                         ItemStackRenderState itemState,
                                         ItemDisplayContext displayContext,
                                         LivingEntityRenderState renderState,
                                         SubmitNodeCollector collector, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.05, -0.12);
        poseStack.scale(0.35f, 0.35f, 0.35f);
        super.submitItemStackRender(
            poseStack, bone, itemState, displayContext, renderState, collector, packedLight);
        poseStack.popPose();
    }

}
