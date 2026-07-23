package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.entity.CrowState;
import com.crowbuddy.entity.CrowBehaviorPolicy;
import com.crowbuddy.networking.ModNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

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

    private static final double SEARCH_RADIUS = 12.0;
    private static final double SEARCH_RADIUS_SQ = SEARCH_RADIUS * SEARCH_RADIUS;
    private static final double PICKUP_DISTANCE_SQ = 1.0;
    private static final double MOVE_SPEED = 1.15;
    private static final int MAX_SEEK_TICKS = 200;

    private final CrowEntity crow;
    private ItemEntity targetItem;
    private long nextSearchTick;
    private int seekTicks;
    private double flightAltitude;
    private net.minecraft.world.phys.Vec3 lastProgressPosition;
    private int stalledTicks;
    private boolean groundApproach;

    public ScavengeGoal(CrowEntity crow) {
        this.crow = crow;
        this.nextSearchTick = 0;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.crow.isInLove() || this.crow.isInMatingState()) return false;
        if (!CrowBehaviorPolicy.canScavenge(
                this.crow.isBaby(), this.crow.isInSittingPose() || this.crow.isOrderedToSit(),
                !this.crow.getCarriedItem().isEmpty(), this.crow.getSatiation())) {
            return false;
        }
        if (this.crow.level().getGameTime() < this.nextSearchTick) {
            return false;
        }
        ServerLevel level = getServerLevel(this.crow);
        if (level == null) return false;
        this.targetItem = findBestItem(level);
        if (this.targetItem == null) {
            this.nextSearchTick = this.crow.level().getGameTime() + reducedTickDelay(20);
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.seekTicks = 0;
        this.crow.setState(CrowState.SEARCHING);
        this.lastProgressPosition = this.crow.position();
        this.stalledTicks = 0;
        this.beginTargetApproach(true);
        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} began scavenging {} at {}",
            this.crow.getId(), this.targetItem.getItem(), this.targetItem.blockPosition());
    }

    @Override
    public void tick() {
        this.seekTicks++;
        if (this.targetItem == null || !this.targetItem.isAlive()) return;
        this.recoverStalledFlight();
        if (this.crow.distanceToSqr(this.targetItem) > PICKUP_DISTANCE_SQ) {
            if (this.groundApproach) {
                if (this.seekTicks % adjustedTickDelay(10) == 0) {
                    this.crow.getNavigation().moveTo(this.targetItem, MOVE_SPEED);
                }
            } else {
                this.flyTowardItem();
            }
            return;
        }
        this.collectFromTarget();
    }

    private void collectFromTarget() {
        ItemStack source = this.targetItem.getItem();
        ItemStack carried = this.crow.getCarriedItem();
        int limit = source.isStackable() ? CrowBehaviorPolicy.MAX_SCAVENGE_STACK_SIZE : 1;
        int carriedCount = carried.isEmpty() ? 0 : carried.getCount();
        int transferCount = CrowBehaviorPolicy.scavengeTransferCount(
            carriedCount, source.getCount(), source.isStackable());
        if (transferCount <= 0) {
            this.finishCollection(carried);
            return;
        }

        ItemStack updated = carried.isEmpty()
            ? source.copyWithCount(transferCount)
            : carried.copy();
        if (!carried.isEmpty()) updated.grow(transferCount);
        source.shrink(transferCount);
        this.crow.setCarriedItem(updated);
        if (source.isEmpty()) {
            this.targetItem.remove(Entity.RemovalReason.DISCARDED);
        } else {
            this.targetItem.setItem(source);
        }
        this.crow.level().playSound(
            null,
            this.crow.blockPosition(),
            net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
            this.crow.getSoundSource(),
            0.2f,
            1.0f
        );
        this.broadcastScavenge(updated);

        if (updated.getCount() >= limit || !(this.crow.level() instanceof ServerLevel serverLevel)) {
            this.finishCollection(updated);
            return;
        }
        this.targetItem = findNearestMatchingItem(serverLevel, updated);
        if (this.targetItem == null) {
            this.finishCollection(updated);
        } else {
            this.beginTargetApproach(true);
            com.crowbuddy.CrowBuddy.LOGGER.debug(
                "Crow {} continuing collection of {} at {} ({}/{})",
                this.crow.getId(), updated.getItem(), this.targetItem.blockPosition(),
                updated.getCount(), limit);
        }
    }

    private void finishCollection(ItemStack carried) {
        this.crow.setState(CrowState.CARRYING);
        this.targetItem = null;
        this.lastProgressPosition = null;
        this.stalledTicks = 0;
        this.groundApproach = false;
        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} completed collection with {}", this.crow.getId(), carried);
    }

    @Override
    public void stop() {
        this.crow.getNavigation().stop();
        if (this.crow.isAirborne()) {
            this.crow.setAirborne(false);
            this.crow.triggerLandAnimation();
            var movement = this.crow.getDeltaMovement();
            this.crow.setDeltaMovement(movement.x * 0.35, Math.min(0.0, movement.y), movement.z * 0.35);
        }
        if (this.targetItem != null && this.targetItem.isAlive()) {
            this.nextSearchTick = this.crow.level().getGameTime() + reducedTickDelay(20);
        }
        if (this.crow.getCarriedItem().isEmpty()) {
            this.crow.setState(CrowState.IDLE);
        }
        this.targetItem = null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetItem != null
            && this.targetItem.isAlive()
            && this.seekTicks < MAX_SEEK_TICKS
            && !this.crow.isInLove()
            && !this.crow.isInMatingState()
            && !this.crow.isBaby()
            && !this.crow.isInSittingPose()
            && !this.crow.isOrderedToSit()
            && this.crow.getSatiation() >= CrowBehaviorPolicy.MIN_SCAVENGE_SATIATION;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private void flyTowardItem() {
        boolean ascending = CrowBehaviorPolicy.shouldAscendBeforeScavenging(
            this.seekTicks, this.crow.getY(), this.flightAltitude);
        double dx = this.targetItem.getX() - this.crow.getX();
        double dz = this.targetItem.getZ() - this.crow.getZ();
        double horizontalDistanceSq = dx * dx + dz * dz;
        double targetY = CrowBehaviorPolicy.scavengeFlightTargetY(
            this.seekTicks, horizontalDistanceSq, this.flightAltitude, this.targetItem.getY());
        var target = ascending
            ? new net.minecraft.world.phys.Vec3(
                this.crow.getX(), this.flightAltitude, this.crow.getZ())
            : new net.minecraft.world.phys.Vec3(
                this.targetItem.getX(), targetY, this.targetItem.getZ());
        var delta = target.subtract(this.crow.position());
        if (delta.lengthSqr() < 0.01) return;
        var desired = delta.normalize().scale(0.28 * MOVE_SPEED);
        var movement = this.crow.getDeltaMovement().scale(0.68).add(desired.scale(0.32));
        if (this.crow.onGround()) {
            movement = new net.minecraft.world.phys.Vec3(
                movement.x, Math.max(0.42, movement.y), movement.z);
        }
        this.crow.setDeltaMovement(movement);
        this.crow.getLookControl().setLookAt(
            target.x, target.y, target.z, 12.0f, this.crow.getMaxHeadXRot());
    }

    private void beginTargetApproach(boolean allowGroundHop) {
        double dx = this.targetItem.getX() - this.crow.getX();
        double dz = this.targetItem.getZ() - this.crow.getZ();
        this.groundApproach = allowGroundHop && CrowBehaviorPolicy.shouldUseGroundHop(
            this.targetItem.getY() - this.crow.getY(), dx * dx + dz * dz);
        if (this.groundApproach) {
            this.crow.setAirborne(false);
            this.crow.getNavigation().moveTo(this.targetItem, MOVE_SPEED);
            return;
        }

        this.flightAltitude = Math.max(
            this.crow.getY() + 3.5, this.targetItem.getY() + 2.5);
        this.seekTicks = 0;
        this.crow.launchToward(
            new net.minecraft.world.phys.Vec3(
                this.crow.getX(), this.flightAltitude, this.crow.getZ()),
            0.0, 0.60);
    }

    private void recoverStalledFlight() {
        if (this.lastProgressPosition == null
                || this.crow.position().distanceToSqr(this.lastProgressPosition) >= 0.04) {
            this.lastProgressPosition = this.crow.position();
            this.stalledTicks = 0;
            return;
        }
        if (++this.stalledTicks < 15) return;

        this.stalledTicks = 0;
        this.lastProgressPosition = this.crow.position();
        this.beginTargetApproach(false);
        com.crowbuddy.CrowBuddy.LOGGER.debug(
            "Crow {} relaunched stalled scavenging flight toward {} at altitude {}",
            this.crow.getId(), this.targetItem.blockPosition(), this.flightAltitude);
    }

    private ItemEntity findBestItem(ServerLevel level) {
        Set<Integer> claimedItemIds = claimedItemIds(level);
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
            if (!itemEntity.isAlive() || claimedItemIds.contains(itemEntity.getId())) {
                continue;
            }
            double distSq = this.crow.distanceToSqr(itemEntity);
            if (distSq > SEARCH_RADIUS_SQ) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (stack.is(BEACON_PAYMENT) && distSq < bestBeaconDist) {
                bestBeacon = itemEntity;
                bestBeaconDist = distSq;
            }
            if (stack.is(PIGLIN_LOVED) && distSq < bestPiglinDist) {
                bestPiglin = itemEntity;
                bestPiglinDist = distSq;
            }
            if (stack.is(TRIM_MATERIALS) && distSq < bestTrimDist) {
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
            if (!itemEntity.isAlive() || claimedItemIds.contains(itemEntity.getId())) {
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

    private ItemEntity findNearestMatchingItem(ServerLevel level, ItemStack carried) {
        Set<Integer> claimedItemIds = claimedItemIds(level);
        ItemEntity nearest = null;
        double nearestDistance = SEARCH_RADIUS_SQ;
        for (ItemEntity candidate : level.getEntitiesOfClass(
                ItemEntity.class,
                this.crow.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
            if (!candidate.isAlive()
                    || claimedItemIds.contains(candidate.getId())
                    || !ItemStack.isSameItemSameComponents(carried, candidate.getItem())) {
                continue;
            }
            double distance = this.crow.distanceToSqr(candidate);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private Set<Integer> claimedItemIds(ServerLevel level) {
        Set<Integer> claimed = new HashSet<>();
        for (CrowEntity other : level.getEntitiesOfClass(
                CrowEntity.class,
                this.crow.getBoundingBox().inflate(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS))) {
            if (other == this.crow || other.getScavengeGoal() == null) continue;
            int itemId = other.getScavengeGoal().targetItemId();
            if (itemId >= 0) claimed.add(itemId);
        }
        return claimed;
    }

    public int targetItemId() {
        return this.targetItem != null && this.targetItem.isAlive()
            ? this.targetItem.getId()
            : -1;
    }

    private void broadcastScavenge(ItemStack carriedItem) {
        if (this.crow.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                ModNetworking.sendScavenge(player, this.crow.getId(), carriedItem);
            }
        }
    }

    public void dropCarriedItem() {
        this.dropCarriedItemAt(this.crow);
    }

    public void dropCarriedItemAt(Entity recipient) {
        ItemStack carried = this.crow.getCarriedItem();
        if (carried.isEmpty()) {
            return;
        }
        this.crow.setCarriedItem(ItemStack.EMPTY);
        this.crow.setState(CrowState.IDLE);
        ItemEntity itemEntity = new ItemEntity(
            this.crow.level(),
            recipient.getX(),
            recipient.getY() + 0.25,
            recipient.getZ(),
            carried
        );
        itemEntity.setNoPickUpDelay();
        this.crow.level().addFreshEntity(itemEntity);
    }
}
