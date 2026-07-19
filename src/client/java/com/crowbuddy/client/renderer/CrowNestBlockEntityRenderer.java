package com.crowbuddy.client.renderer;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.block.entity.CrowNestBlockEntity;
import com.crowbuddy.block.entity.CrowNestStateMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class CrowNestBlockEntityRenderer
        implements BlockEntityRenderer<CrowNestBlockEntity, CrowNestBlockEntityRenderer.CrowNestRenderState> {

    private static final Identifier CROW_TEXTURE = CrowBuddy.id("textures/entity/crow");
    private static final RenderType EGG_RENDER_TYPE = RenderTypes.entitySolid(CROW_TEXTURE);

    public static class CrowNestRenderState extends BlockEntityRenderState {
        int stage = CrowNestStateMachine.STAGE_IDLE;
    }

    public CrowNestBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static CrowNestBlockEntityRenderer create(BlockEntityRendererProvider.Context context) {
        return new CrowNestBlockEntityRenderer(context);
    }

    @Override
    public CrowNestRenderState createRenderState() {
        return new CrowNestRenderState();
    }

    @Override
    public void extractRenderState(CrowNestBlockEntity blockEntity, CrowNestRenderState state,
                                   float partialTick, net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.stage = blockEntity.getStage();
    }

    @Override
    public void submit(CrowNestRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        switch (state.stage) {
            case CrowNestStateMachine.STAGE_EGGS -> submitEggs(collector, poseStack, state, false);
            case CrowNestStateMachine.STAGE_HATCHING -> submitEggs(collector, poseStack, state, true);
            case CrowNestStateMachine.STAGE_FLEDGLING -> submitChick(collector, poseStack, state);
            case CrowNestStateMachine.STAGE_BABY_FLYING -> submitRemnants(collector, poseStack);
            default -> {}
        }

        poseStack.popPose();
    }

    private void submitEggs(SubmitNodeCollector collector, PoseStack poseStack,
                            CrowNestRenderState state, boolean shaking) {
        float[][] positions = {
            {-0.2f, 0.0f, -0.15f},
            {0.2f, 0.0f, -0.15f},
            {0.0f, 0.0f, 0.2f}
        };

        for (float[] pos : positions) {
            poseStack.pushPose();
            poseStack.translate(pos[0], pos[1], pos[2]);

            if (shaking) {
                float angle = (float) Math.sin(state.blockPos.getY() * 0.1) * 5.0f;
                poseStack.mulPose(new Quaternionf().set(new AxisAngle4f(1.0f, 0.0f, 0.0f, angle * 0.01745f)));
            }

            float eggW = 0.12f;
            float eggH = 0.18f;
            collector.submitCustomGeometry(poseStack, EGG_RENDER_TYPE,
                (pose, consumer) -> renderCube(consumer, pose, eggW, eggH, 0.9f, 0.9f, 0.9f, 1.0f));

            poseStack.popPose();
        }
    }

    private void submitChick(SubmitNodeCollector collector, PoseStack poseStack,
                             CrowNestRenderState state) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.05f, 0.0f);
        float pulse = (float) Math.sin(state.blockPos.getX() * 0.1) * 0.02f;
        float scale = 0.15f + pulse;
        poseStack.scale(scale, scale, scale);

        collector.submitCustomGeometry(poseStack, EGG_RENDER_TYPE,
            (pose, consumer) -> renderCube(consumer, pose, 1.0f, 1.0f, 0.3f, 0.3f, 0.3f, 1.0f));

        poseStack.popPose();
    }

    private void submitRemnants(SubmitNodeCollector collector, PoseStack poseStack) {
        float[] positions = {-0.25f, 0.1f, 0.2f};
        for (float x : positions) {
            poseStack.pushPose();
            poseStack.translate(x, -0.05f, 0.1f);

            collector.submitCustomGeometry(poseStack, EGG_RENDER_TYPE,
                (pose, consumer) -> renderCube(consumer, pose, 0.08f, 0.02f, 0.15f, 0.15f, 0.15f, 0.8f));

            poseStack.popPose();
        }
    }

    private void renderCube(VertexConsumer consumer, PoseStack.Pose pose,
                            float w, float h, float r, float g, float b, float a) {
        float halfW = w * 0.5f;
        float halfH = h * 0.5f;

        renderFace(consumer, pose, halfW, halfH, r, g, b, a,
            -halfW, halfH, -halfW,
             halfH, halfH, -halfW,
             halfW, halfH,  halfW,
            -halfW, halfH,  halfW);

        renderFace(consumer, pose, halfW, halfH, r, g, b, a,
            -halfW, -halfH,  halfW,
             halfW, -halfH,  halfW,
             halfW, -halfH, -halfW,
            -halfW, -halfH, -halfW);

        renderFace(consumer, pose, halfW, halfH, r, g, b, a,
            -halfW,  halfH, -halfW,
             halfW,  halfH, -halfW,
             halfW, -halfH, -halfW,
            -halfW, -halfH, -halfW);

        renderFace(consumer, pose, halfW, halfH, r, g, b, a,
             halfW,  halfH,  halfW,
            -halfW,  halfH,  halfW,
            -halfW, -halfH,  halfW,
             halfW, -halfH,  halfW);

        renderFace(consumer, pose, halfW, halfH, r, g, b, a,
             halfW,  halfH, -halfW,
             halfW,  halfH,  halfW,
             halfW, -halfH,  halfW,
             halfW, -halfH, -halfW);

        renderFace(consumer, pose, halfW, halfH, r, g, b, a,
            -halfW,  halfH,  halfW,
            -halfW,  halfH, -halfW,
            -halfW, -halfH, -halfW,
            -halfW, -halfH,  halfW);
    }

    private void renderFace(VertexConsumer consumer, PoseStack.Pose pose,
                            float w, float h, float r, float g, float b, float a,
                            float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3) {
        consumer.addVertex(pose, x0, y0, z0).setColor(r, g, b, a);
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
