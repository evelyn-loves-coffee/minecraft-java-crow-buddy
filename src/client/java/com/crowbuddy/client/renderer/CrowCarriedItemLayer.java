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
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** Renders the scavenged item in the beak and a floating seed feeding prompt. */
final class CrowCarriedItemLayer
        extends BlockAndItemGeoLayer<CrowEntity, Void, LivingEntityRenderState> {
    // The lower-beak pivot lies at the seam between the upper and lower beak
    // for both models, making it a stable attachment point for held items.
    private static final String MOUTH_BONE = "lower_beak";

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
        poseStack.translate(0.0, 0.0, -0.38);
        // Item ground models stand vertically in this bone space by default.
        // Rotate their vertical axis onto the beak's forward axis so the item
        // projects out of the mouth instead of rising above it.
        poseStack.mulPose(new Quaternionf().rotationX((float) (Math.PI * 0.5)));
        poseStack.scale(0.7f, 0.7f, 0.7f);
        super.submitItemStackRender(
            poseStack, bone, itemState, displayContext, renderState, collector, packedLight);
        poseStack.popPose();
    }

}
