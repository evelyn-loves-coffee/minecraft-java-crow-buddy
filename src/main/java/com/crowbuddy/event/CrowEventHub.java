package com.crowbuddy.event;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.item.ModItems;
import com.crowbuddy.swarm.SwarmManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
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
        LootTableEvents.MODIFY.register((key, builder, source, accessor) -> {
            if (key.identifier().equals(Identifier.fromNamespaceAndPath("minecraft", "blocks/sunflower"))) {
                builder.withPool(LootPool.lootPool()
                    .add(LootItem.lootTableItem(ModItems.BLACK_OIL_SUNFLOWER_SEEDS)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2f, 6f)))
                    )
                );
            }
        });
    }

    static void onEntityDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount, float knockback, boolean isDirect) {
        if (!entity.level().isClientSide()) {
            if (entity instanceof CrowEntity crow) {
                handleCrowDamaged(crow, source, amount);
            } else {
                handleNonCrowDamaged(entity, source);
            }
        }
    }

    static InteractionResult onPlayerAttackEntity(Player player, Level level, net.minecraft.world.InteractionHand hand, Entity target, EntityHitResult hitResult) {
        if (!level.isClientSide()) {
            if (target instanceof CrowEntity crow) {
                handlePlayerAttackCrow(player, crow);
            } else {
                handlePlayerAttackTarget(player, target);
            }
        }
        return InteractionResult.PASS;
    }

    static void handleCrowDamaged(CrowEntity crow, net.minecraft.world.damagesource.DamageSource source, float amount) {
        SwarmManager.get(crow.level()).onCrowDamaged(crow, source, amount);
    }

    static void handleNonCrowDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        SwarmManager.get(entity.level()).onNonCrowDamaged(entity, source);
    }

    static void handlePlayerAttackCrow(Player player, CrowEntity crow) {
        SwarmManager.get(crow.level()).onPlayerAttackCrow(player, crow);
    }

    static void handlePlayerAttackTarget(Player player, Entity target) {
        SwarmManager.get(player.level()).onPlayerAttackTarget(player, target);
    }

    private static void log(String msg) {
        CrowBuddy.LOGGER.debug("[CrowEventHub] " + msg);
    }
}
