package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.entity.CrowState;
import com.crowbuddy.networking.ModNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ScavengeGoal extends Goal {
    private static final TagKey<Item> BEACON_PAYMENT = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "beacon_payment_items")
    );
    private static final TagKey<Item> PIGLIN_LOVED = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "piglin_loved")
    );
    private static final TagKey<Item> TRIM_MATERIALS = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "trim_materials")
    );

    private static final double SEARCH_RADIUS = 1.0;
    private static final double SEARCH_RADIUS_SQ = SEARCH_RADIUS * SEARCH_RADIUS;

    private final CrowEntity crow;
    private ItemEntity targetItem;
    private int cooldownTimer;

    public ScavengeGoal(CrowEntity crow) {
        this.crow = crow;
        this.cooldownTimer = 0;
    }

    @Override
    public boolean canUse() {
        if (this.crow.isInSittingPose() || this.crow.isPerched()) {
            return false;
        }
        if (!this.crow.getCarriedItem().isEmpty()) {
            return false;
        }
        if (this.crow.getState() != CrowState.IDLE) {
            return false;
        }
        if (this.cooldownTimer > 0) {
            return false;
        }
        float satiation = this.crow.getSatiation();
        if (satiation < 0.3f) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        ServerLevel level = getServerLevel(this.crow);
        if (level == null) {
            return;
        }
        this.targetItem = findBestItem(level);
        if (this.targetItem == null) {
            return;
        }
        ItemStack itemStack = this.targetItem.getItem().copy();
        this.crow.setCarriedItem(itemStack);
        this.crow.setState(CrowState.CARRYING);
        this.targetItem.setItem(ItemStack.EMPTY);
        this.targetItem.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        this.temporaryCooldown();
        this.crow.level().playSound(
            null,
            this.crow.blockPosition(),
            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
            this.crow.getSoundSource(),
            0.2f,
            1.0f
        );
        this.broadcastScavenge(itemStack);
    }

    @Override
    public void stop() {
        this.targetItem = null;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }

    private ItemEntity findBestItem(ServerLevel level) {
        java.util.List<ItemEntity> candidates = level.getEntitiesOfClass(
            ItemEntity.class,
            this.crow.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS)
        );
        if (candidates.isEmpty()) {
            return null;
        }
        ItemEntity bestBeacon = null, bestPiglin = null, bestTrim = null;
        double bestBeaconDist = Double.MAX_VALUE, bestPiglinDist = Double.MAX_VALUE, bestTrimDist = Double.MAX_VALUE;

        for (ItemEntity itemEntity : candidates) {
            if (!itemEntity.isAlive()) {
                continue;
            }
            double distSq = this.crow.distanceToSqr(itemEntity);
            if (distSq > SEARCH_RADIUS_SQ) {
                continue;
            }
            Item item = itemEntity.getItem().getItem();
            if (isInTag(item, BEACON_PAYMENT) && distSq < bestBeaconDist) {
                bestBeacon = itemEntity;
                bestBeaconDist = distSq;
            }
            if (isInTag(item, PIGLIN_LOVED) && distSq < bestPiglinDist) {
                bestPiglin = itemEntity;
                bestPiglinDist = distSq;
            }
            if (isInTag(item, TRIM_MATERIALS) && distSq < bestTrimDist) {
                bestTrim = itemEntity;
                bestTrimDist = distSq;
            }
        }
        if (bestBeacon != null) {
            return bestBeacon;
        }
        if (bestPiglin != null) {
            return bestPiglin;
        }
        if (bestTrim != null) {
            return bestTrim;
        }
        ItemEntity nearest = null;
        double nearestDist = SEARCH_RADIUS_SQ;
        for (ItemEntity itemEntity : candidates) {
            if (!itemEntity.isAlive()) {
                continue;
            }
            double distSq = this.crow.distanceToSqr(itemEntity);
            if (distSq < nearestDist) {
                nearest = itemEntity;
                nearestDist = distSq;
            }
        }
        return nearest;
    }

    private static boolean isInTag(Item item, TagKey<Item> tag) {
        return item.builtInRegistryHolder().is(tag);
    }

    private void temporaryCooldown() {
        float satiation = this.crow.getSatiation();
        if (satiation >= 0.8f) {
            this.cooldownTimer = reducedTickDelay(1200);
        } else if (satiation >= 0.5f) {
            this.cooldownTimer = reducedTickDelay(4800);
        } else {
            this.cooldownTimer = reducedTickDelay(9600);
        }
    }

    private void broadcastScavenge(ItemStack carriedItem) {
        if (this.crow.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                ModNetworking.sendScavenge(player, this.crow.getId(), carriedItem);
            }
        }
    }

    @Override
    public void tick() {
        if (this.cooldownTimer > 0) {
            --this.cooldownTimer;
        }
    }

    public void dropCarriedItem() {
        ItemStack carried = this.crow.getCarriedItem();
        if (carried.isEmpty()) {
            return;
        }
        this.crow.setCarriedItem(ItemStack.EMPTY);
        this.crow.setState(CrowState.IDLE);
        ItemEntity itemEntity = new ItemEntity(
            this.crow.level(),
            this.crow.getX(),
            this.crow.getY() + 0.25,
            this.crow.getZ(),
            carried
        );
        itemEntity.setNoPickUpDelay();
        this.crow.level().addFreshEntity(itemEntity);
    }
}
