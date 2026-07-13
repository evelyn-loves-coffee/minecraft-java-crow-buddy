package com.crowbuddy.event;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.swarm.SwarmManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public class CrowEventHub {

    public static void registerEvents() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CrowEventHub::onEntityDamaged);
        AttackEntityCallback.EVENT.register(CrowEventHub::onPlayerAttackEntity);
    }

    private static void onEntityDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount, float knockback, boolean isDirect) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof CrowEntity crow) {
                handleCrowDamaged(crow, source, amount);
            } else {
                handleNonCrowDamaged(entity, source);
            }
        }
    }

    private static InteractionResult onPlayerAttackEntity(Player player, Level level, net.minecraft.world.InteractionHand hand, Entity target, EntityHitResult hitResult) {
        if (!level.isClientSide()) {
            if (target instanceof CrowEntity crow) {
                handlePlayerAttackCrow(player, crow);
            } else {
                handlePlayerAttackTarget(player, target);
            }
        }
        return InteractionResult.PASS;
    }

    private static void handleCrowDamaged(CrowEntity crow, net.minecraft.world.damagesource.DamageSource source, float amount) {
        SwarmManager.INSTANCE.onCrowDamaged(crow, source, amount);
    }

    private static void handleNonCrowDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        SwarmManager.INSTANCE.onNonCrowDamaged(entity, source);
    }

    private static void handlePlayerAttackCrow(Player player, CrowEntity crow) {
        SwarmManager.INSTANCE.onPlayerAttackCrow(player, crow);
    }

    private static void handlePlayerAttackTarget(Player player, Entity target) {
        SwarmManager.INSTANCE.onPlayerAttackTarget(player, target);
    }

    private static void log(String msg) {
        CrowBuddy.LOGGER.debug("[CrowEventHub] " + msg);
    }
}
