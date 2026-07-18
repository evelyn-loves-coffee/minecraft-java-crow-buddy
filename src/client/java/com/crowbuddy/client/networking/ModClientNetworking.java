package com.crowbuddy.client.networking;

import com.crowbuddy.networking.DistressPayload;
import com.crowbuddy.networking.ScavengePayload;
import com.crowbuddy.sound.ModSounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class ModClientNetworking {

    private static final int DISTRESS_PARTICLE_COUNT = 8;
    private static final float DISTRESS_SOUND_VOLUME = 0.8f;
    private static final int SCAVENGE_PARTICLE_COUNT = 4;

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(DistressPayload.TYPE, ModClientNetworking::handleDistress);
        ClientPlayNetworking.registerGlobalReceiver(ScavengePayload.TYPE, ModClientNetworking::handleScavenge);
    }

    private static void handleDistress(DistressPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity source = mc.level.getEntity(payload.sourceId());
            if (source != null && source.isAlive()) {
                double x = source.getX();
                double y = source.getEyeY();
                double z = source.getZ();

                mc.level.playLocalSound(
                    x, y, z,
                    ModSounds.CROW_DISTRESS,
                    SoundSource.HOSTILE,
                    DISTRESS_SOUND_VOLUME,
                    0.9f + mc.level.getRandom().nextFloat() * 0.2f,
                    false
                );

                for (int i = 0; i < DISTRESS_PARTICLE_COUNT; i++) {
                    mc.level.addParticle(
                        ParticleTypes.ANGRY_VILLAGER,
                        x + mc.level.getRandom().nextDouble() * 0.6 - 0.3,
                        y + mc.level.getRandom().nextDouble() * 0.5,
                        z + mc.level.getRandom().nextDouble() * 0.6 - 0.3,
                        0.0, 0.1, 0.0
                    );
                }
            }

            Entity target = mc.level.getEntity(payload.entityId());
            if (target != null && target.isAlive()) {
                for (int i = 0; i < 3; i++) {
                    mc.level.addParticle(
                        ParticleTypes.CRIT,
                        target.getX() + mc.level.getRandom().nextDouble() * 0.4 - 0.2,
                        target.getEyeY() + mc.level.getRandom().nextDouble() * 0.3,
                        target.getZ() + mc.level.getRandom().nextDouble() * 0.4 - 0.2,
                        0.0, 0.05, 0.0
                    );
                }
            }
        });
    }

    private static void handleScavenge(ScavengePayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity crow = mc.level.getEntity(payload.crowId());
            if (crow != null && crow.isAlive()) {
                double x = crow.getX();
                double y = crow.getEyeY();
                double z = crow.getZ();

                for (int i = 0; i < SCAVENGE_PARTICLE_COUNT; i++) {
                    mc.level.addParticle(
                        ParticleTypes.POOF,
                        x + mc.level.getRandom().nextDouble() * 0.4 - 0.2,
                        y + mc.level.getRandom().nextDouble() * 0.3,
                        z + mc.level.getRandom().nextDouble() * 0.4 - 0.2,
                        0.0, 0.05, 0.0
                    );
                }

                mc.level.playLocalSound(
                    x, y, z,
                    net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
                    SoundSource.NEUTRAL,
                    0.4f,
                    1.2f,
                    false
                );
            }
        });
    }
}
