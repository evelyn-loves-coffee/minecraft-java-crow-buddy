package com.crowbuddy.client.renderer;

import com.crowbuddy.client.model.CrowGeoModel;
import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.entity.CrowBehaviorPolicy;
import com.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

public class CrowRenderer extends GeoEntityRenderer<CrowEntity, LivingEntityRenderState> {
    private static final DataTicket<ItemStackRenderState> SEED_PROMPT =
        DataTicket.create("crow_seed_prompt", ItemStackRenderState.class);

    public CrowRenderer(EntityRendererProvider.Context context) {
        super(context, new CrowGeoModel());
        this.withRenderLayer(new CrowCarriedItemLayer(context, this));
    }

    @Override
    public void extractRenderState(CrowEntity crow, LivingEntityRenderState renderState,
                                   float partialTick) {
        super.extractRenderState(crow, renderState, partialTick);
        if (!crow.getCarriedItem().isEmpty()
                || crow.getSatiation() < CrowBehaviorPolicy.MIN_SCAVENGE_SATIATION) {
            ((GeoRenderState) renderState).addGeckolibData(
                SEED_PROMPT,
                RenderUtil.createRenderStateForItem(
                    Items.WHEAT_SEEDS.getDefaultInstance(), this.itemModelResolver,
                    ItemDisplayContext.GUI, crow));
        }
    }

    @Override
    public void submit(LivingEntityRenderState renderState, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(renderState, poseStack, collector, cameraState);
        ItemStackRenderState seed = ((GeoRenderState) renderState)
            .getOrDefaultGeckolibData(SEED_PROMPT, (ItemStackRenderState) null);
        if (seed == null) return;

        poseStack.pushPose();
        poseStack.translate(0.0, renderState.boundingBoxHeight + 0.65, 0.0);
        poseStack.mulPose(cameraState.orientation);
        float closeScale = (float) Math.max(0.55, Math.min(1.0,
            Math.sqrt(renderState.distanceToCameraSq) / 3.0));
        poseStack.scale(0.45f * closeScale, 0.45f * closeScale, 0.45f * closeScale);
        submitSpeechBubble(poseStack, collector);
        // The bubble occupies Z 0.01-0.02; place the icon closer to the camera
        // and submit it later so translucent bubble geometry cannot wash it out.
        poseStack.translate(0.0, 0.0, 0.06);
        submitSeedIcon(poseStack, collector, seed, renderState.lightCoords);
        poseStack.popPose();
    }

    private static void submitSeedIcon(PoseStack poseStack, SubmitNodeCollector collector,
                                       ItemStackRenderState seed, int packedLight) {
        var material = seed.pickParticleMaterial(RandomSource.create(0L));
        TextureAtlasSprite sprite = material.sprite();
        collector.order(1).submitCustomGeometry(
            poseStack, RenderTypes.entityTranslucent(sprite.atlasLocation()), (pose, vertices) -> {
                iconVertex(vertices, pose, -0.56f, -0.56f, 0.0f,
                    sprite.getU0(), sprite.getV1(), packedLight);
                iconVertex(vertices, pose, 0.56f, -0.56f, 0.0f,
                    sprite.getU1(), sprite.getV1(), packedLight);
                iconVertex(vertices, pose, 0.56f, 0.56f, 0.0f,
                    sprite.getU1(), sprite.getV0(), packedLight);
                iconVertex(vertices, pose, -0.56f, 0.56f, 0.0f,
                    sprite.getU0(), sprite.getV0(), packedLight);
            });
    }

    private static void iconVertex(com.mojang.blaze3d.vertex.VertexConsumer vertices,
                                   PoseStack.Pose pose, float x, float y, float z,
                                   float u, float v, int packedLight) {
        vertices.addVertex(pose, x, y, z)
            .setColor(255, 255, 255, 204)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0.0f, 0.0f, 1.0f);
    }

    private static void submitSpeechBubble(PoseStack poseStack, SubmitNodeCollector collector) {
        collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, vertices) -> {
            quad(vertices, pose, -0.72f, -0.60f, 0.72f, 0.60f, 0.02f, 0x99202020);
            quad(vertices, pose, -0.63f, -0.51f, 0.63f, 0.51f, 0.01f, 0x99F7F7F2);
            triangle(vertices, pose, -0.24f, -0.51f, 0.24f, -0.51f,
                0.0f, -0.85f, 0.015f, 0x99202020);
            triangle(vertices, pose, -0.15f, -0.49f, 0.15f, -0.49f,
                0.0f, -0.73f, 0.005f, 0x99F7F7F2);
        });
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vertices,
                             PoseStack.Pose pose, float left, float bottom,
                             float right, float top, float z, int color) {
        vertices.addVertex(pose, left, bottom, z).setColor(color);
        vertices.addVertex(pose, right, bottom, z).setColor(color);
        vertices.addVertex(pose, right, top, z).setColor(color);
        vertices.addVertex(pose, left, top, z).setColor(color);
    }

    private static void triangle(com.mojang.blaze3d.vertex.VertexConsumer vertices,
                                 PoseStack.Pose pose, float x1, float y1,
                                 float x2, float y2, float x3, float y3,
                                 float z, int color) {
        vertices.addVertex(pose, x1, y1, z).setColor(color);
        vertices.addVertex(pose, x2, y2, z).setColor(color);
        vertices.addVertex(pose, x3, y3, z).setColor(color);
        vertices.addVertex(pose, x3, y3, z).setColor(color);
    }
}
