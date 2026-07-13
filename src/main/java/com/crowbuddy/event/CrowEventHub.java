package com.crowbuddy.event;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
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
        if (!crow.getCarriedItem().isEmpty()) {
            crow.dropCarriedItem();
        }
        log("Crow damaged: " + crow.getId() + " by " + source);
    }

    private static void handleNonCrowDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        // TODO: Delegate to SwarmManager for crow defense trigger
        log("Entity damaged: " + entity.getType().getDescription().getString());
    }

    private static void handlePlayerAttackCrow(Player player, CrowEntity crow) {
        // TODO: Delegate to SwarmManager for attack detection
        log("Player " + player.getName().getString() + " attacked crow " + crow.getId());
    }

    private static void handlePlayerAttackTarget(Player player, Entity target) {
        // TODO: Delegate to SwarmManager for defending player logic
        log("Player " + player.getName().getString() + " attacked target " + target.getType().getDescription().getString());
    }

    private static void log(String msg) {
        CrowBuddy.LOGGER.debug("[CrowEventHub] " + msg);
    }
}
