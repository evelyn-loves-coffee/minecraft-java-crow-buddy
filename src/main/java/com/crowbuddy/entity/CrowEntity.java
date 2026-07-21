package com.crowbuddy.entity;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animatable.manager.AnimatableManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.crowbuddy.goal.CrowFlightGoal;
import com.crowbuddy.goal.ScavengeGoal;
import com.crowbuddy.goal.SwarmDistressGoal;
import com.crowbuddy.swarm.SwarmManager;

public class CrowEntity extends TamableAnimal implements GeoAnimatable {
    private static final TagKey<Item> PARROT_POISONOUS = TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "parrot_poisonous_food")
    );
    private static final TagKey<Item> PARROT_FOOD = TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        Identifier.fromNamespaceAndPath("minecraft", "parrot_food")
    );

    private static final EntityDataAccessor<Boolean> PERCHED =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.INT);

    private static final int STATE_ID = 0;
    private static final int STATE_SEARCHING = 1;
    private static final int STATE_CARRYING = 2;
    private static final int STATE_COMBAT = 3;
    private static final int STATE_DISTRESS = 4;
    private static final int STATE_SWARM = 5;
    private static final int STATE_NESTING = 6;
    private static final EntityDataAccessor<ItemStack> CARRIED_ITEM =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> SATIATION =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RELATIONSHIP =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IN_MATING_STATE =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AIRBORNE =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private ScavengeGoal scavengeGoal;
    private SwarmDistressGoal swarmGoal;
    private boolean behaviorControllerActive = false;
    private int oneShotStartTick = -1;
    private int takeoffStartTick = -1;
    private int landStartTick = -1;
    private net.minecraft.core.BlockPos homeNestPos;
    private static final int PECK_DURATION_TICKS = 8;
    private static final int CAW_DURATION_TICKS = 15;
    private static final int PREEN_DURATION_TICKS = 46;
    private static final int BABY_PECK_DURATION_TICKS = 6;
    private static final int BABY_CAW_DURATION_TICKS = 12;
    private static final int BABY_PREEN_DURATION_TICKS = 35;
    private static final int BABY_TAKEOFF_DURATION_TICKS = 13;
    private static final int BABY_LAND_DURATION_TICKS = 14;
    private static final int ADULT_TAKEOFF_DURATION_TICKS = 14;
    private static final int ADULT_LAND_DURATION_TICKS = 16;

    public CrowEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.crowbuddy.entity.ai.goal.CrowNestSeekGoal(this, 1.0, 1200));
        final CrowEntity self = this;
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(
            this, 1.0, 6.0f, 10.0f) {
            @Override
            public boolean canUse() {
                return !self.isPerched() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        if (this.isBaby()) {
            registerBabyGoals();
        } else {
            registerAdultGoals();
        }
    }

    private void registerAdultGoals() {
        this.swarmGoal = new SwarmDistressGoal(this, SwarmManager.get(this.level()), SwarmDistressGoal.Mode.SWARM);
        this.goalSelector.addGoal(0, this.swarmGoal);
        this.goalSelector.addGoal(1, new CrowFlightGoal(this));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.TemptGoal(
            this, 1.25, itemStack -> this.isFood(itemStack), false));
        this.scavengeGoal = new ScavengeGoal(this);
        this.goalSelector.addGoal(4, this.scavengeGoal);
    }

    private void registerBabyGoals() {
        this.goalSelector.addGoal(1, new com.crowbuddy.goal.BabyFlightGoal(this));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.FollowParentGoal(this, 1.0));
        this.goalSelector.addGoal(4, new com.crowbuddy.goal.BabyNestReturnGoal(this));
        final CrowEntity self = this;
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                if (self.isAirborne()) return false;
                net.minecraft.core.BlockPos nest = self.getHomeNestPos();
                if (nest != null && self.distanceToSqr(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5) > 400.0) {
                    return false;
                }
                return super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PERCHED, false);
        builder.define(STATE, CrowState.IDLE.stateId());
        builder.define(CARRIED_ITEM, ItemStack.EMPTY);
        builder.define(SATIATION, 1.0f);
        builder.define(RELATIONSHIP, 0.0f);
        builder.define(IN_MATING_STATE, false);
        builder.define(AIRBORNE, false);
    }

    public void setPerched(boolean perched) {
        this.entityData.set(PERCHED, perched);
    }

    public boolean isPerched() {
        return this.entityData.get(PERCHED);
    }

    public void setState(CrowState state) {
        this.entityData.set(STATE, state.stateId());
    }

    public CrowState getState() {
        int id = this.entityData.get(STATE);
        return CrowState.fromStateId(id);
    }

    public void setCarriedItem(ItemStack itemStack) {
        this.entityData.set(CARRIED_ITEM, itemStack);
    }

    public ItemStack getCarriedItem() {
        ItemStack stack = this.entityData.get(CARRIED_ITEM);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    public void setSatiation(float value) {
        this.entityData.set(SATIATION, Math.min(1.0f, Math.max(0.0f, value)));
    }

    public float getSatiation() {
        return this.entityData.get(SATIATION);
    }

    public void setRelationship(float value) {
        this.entityData.set(RELATIONSHIP, value);
    }

    public float getRelationship() {
        return this.entityData.get(RELATIONSHIP);
    }

    public void setInMatingState(boolean inMatingState) {
        this.entityData.set(IN_MATING_STATE, inMatingState);
    }

    public boolean isInMatingState() {
        return this.entityData.get(IN_MATING_STATE);
    }

    public void setAirborne(boolean airborne) {
        this.entityData.set(AIRBORNE, airborne);
    }

    public boolean isAirborne() {
        return this.entityData.get(AIRBORNE);
    }

    public void triggerTakeoffAnimation() {
        this.takeoffStartTick = this.tickCount;
    }

    public void triggerLandAnimation() {
        this.landStartTick = this.tickCount;
    }

    public void setHomeNestPos(net.minecraft.core.BlockPos pos) {
        this.homeNestPos = pos;
    }

    public net.minecraft.core.BlockPos getHomeNestPos() {
        return this.homeNestPos;
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.homeNestPos != null) {
            output.putInt("homeNestX", this.homeNestPos.getX());
            output.putInt("homeNestY", this.homeNestPos.getY());
            output.putInt("homeNestZ", this.homeNestPos.getZ());
            output.putString("homeNestDim", this.level().dimension().toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        int x = input.getIntOr("homeNestX", -1);
        int y = input.getIntOr("homeNestY", -1);
        int z = input.getIntOr("homeNestZ", -1);
        if (x != -1 && y != -1 && z != -1) {
            String savedDim = input.getStringOr("homeNestDim", "");
            if (!savedDim.isEmpty() && this.level() != null) {
                if (this.level().dimension().toString().equals(savedDim)) {
                    this.homeNestPos = new net.minecraft.core.BlockPos(x, y, z);
                }
            }
        }
    }

    public LivingEntity getOwner() {
        return net.minecraft.world.entity.EntityReference.getLivingEntity(
            this.getOwnerReference(), this.level());
    }

    public SwarmDistressGoal getSwarmGoal() {
        return this.swarmGoal;
    }

    @Override
    public boolean wantsToAttack(LivingEntity other, LivingEntity target) {
        if (this.isBaby()) {
            return false;
        }
        if (super.wantsToAttack(other, target)) {
            return true;
        }
        if (this.swarmGoal != null) {
            LivingEntity swarmTarget = this.swarmGoal.getTarget();
            if (swarmTarget == target) {
                return true;
            }
        }
        return false;
    }

    public ScavengeGoal getScavengeGoal() {
        return this.scavengeGoal;
    }

    public void dropCarriedItem() {
        if (this.scavengeGoal != null) {
            this.scavengeGoal.dropCarriedItem();
        }
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        Item item = itemStack.getItem();
        if (item == net.minecraft.world.item.Items.COCOA_BEANS) {
            return false;
        }
        if (isInTag(item, PARROT_POISONOUS)) {
            return false;
        }
        if (item == com.crowbuddy.item.ModItems.BLACK_OIL_SUNFLOWER_SEEDS) {
            return true;
        }
        if (isInTag(item, PARROT_FOOD)) {
            return true;
        }
        return false;
    }

    public boolean isBreedingItem(ItemStack itemStack) {
        return isFood(itemStack) || itemStack.is(Items.GOLDEN_DANDELION);
    }

    public static boolean isPoisonousFood(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == net.minecraft.world.item.Items.COCOA_BEANS || isInTag(item, PARROT_POISONOUS);
    }

    private static boolean isInTag(Item item, TagKey<Item> tag) {
        return item.builtInRegistryHolder().is(tag);
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        this.setSatiation(1.0f);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        InteractionResult foodResult = super.mobInteract(player, hand);
        if (foodResult.consumesAction()) {
            return foodResult;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isCrouching()) {
            this.setOrderedToSit(!this.isOrderedToSit());
        } else if (!this.isInSittingPose()) {
            this.setPerched(!this.isPerched());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        if (this.isInSittingPose()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.25, 0.5));
            return;
        }
        if (this.isPerched() && this.isTame()) {
            this.followOwnerPerch();
        }
    }

    private void followOwnerPerch() {
        LivingEntity owner = this.getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        this.lookAt(owner, 180.0f, 180.0f);
        double dx = owner.getX() - this.getX();
        double dz = owner.getZ() - this.getZ();
        double dy = (owner.getY() + owner.getBbHeight() / 2.0) - this.getY();
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > 0.25) {
            double dist = Math.sqrt(distSq);
            double speed = Math.min(dist * 0.5, 0.3);
            this.setDeltaMovement(
                this.getDeltaMovement().add(dx / dist * speed, dy / dist * speed, dz / dist * speed)
            );
        }
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        // BY DESIGN: Crow breeding is handled entirely through the CrowNest block entity,
        // not the vanilla breeding mechanic. Eggs are laid in nests and hatch there.
        return null;
    }

    @Override
    public boolean canFallInLove() {
        return this.isTame() && !this.isBaby();
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel serverLevel, net.minecraft.world.entity.animal.Animal other) {
        this.setInLoveTime(0);
        this.setInMatingState(true);
        if (other instanceof CrowEntity otherCrow) {
            otherCrow.setInLoveTime(0);
            otherCrow.setInMatingState(true);
        }
        serverLevel.sendParticles(
            net.minecraft.core.particles.ParticleTypes.HEART,
            this.getX(), this.getY() + 0.5, this.getZ(),
            7, 0.0, 0.1, 0.0, 0.0
        );
        this.playSound(com.crowbuddy.sound.ModSounds.CROW_MATE, 1.0f, 1.0f);
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!this.level().isClientSide()) {
            this.playSound(com.crowbuddy.sound.ModSounds.CROW_GROW, 1.0f, 1.0f);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
            new AnimationController<>("behaviorController", 0, this::behaviorPredicate)
        );
        controllerRegistrar.add(
            new AnimationController<>("movementController", 0, this::movementPredicate)
        );
    }

    private PlayState behaviorPredicate(AnimationTest<CrowEntity> state) {
        if (this.behaviorControllerActive) {
            if (this.tickCount - this.oneShotStartTick >= this.getOneShotDuration()) {
                this.behaviorControllerActive = false;
                this.oneShotStartTick = -1;
                return PlayState.STOP;
            }
            return PlayState.CONTINUE;
        }
        boolean isBaby = this.isBaby();
        if (isBaby && this.isInSittingPose()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("sleep"));
            return PlayState.CONTINUE;
        }
        if (isBaby && !state.isMoving()
                && this.getSatiation() < 0.4f
                && this.tickCount % 90 == 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("beg"));
            return PlayState.CONTINUE;
        }
        CrowState currentState = this.getState();
        if (currentState == CrowState.COMBAT && this.getTarget() != null) {
            this.behaviorControllerActive = true;
            this.oneShotStartTick = this.tickCount;
            state.setAndContinue(RawAnimation.begin().thenPlay("peck"));
            return PlayState.CONTINUE;
        }
        if (currentState == CrowState.DISTRESS) {
            this.behaviorControllerActive = true;
            this.oneShotStartTick = this.tickCount;
            state.setAndContinue(RawAnimation.begin().thenPlay("caw"));
            return PlayState.CONTINUE;
        }
        if (currentState == CrowState.IDLE && !state.isMoving()
                && this.getSatiation() < 0.3f
                && this.tickCount % 120 == 0) {
            this.behaviorControllerActive = true;
            this.oneShotStartTick = this.tickCount;
            state.setAndContinue(RawAnimation.begin().thenPlay("preen"));
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private int getOneShotDuration() {
        boolean isBaby = this.isBaby();
        CrowState currentState = this.getState();
        if (currentState == CrowState.COMBAT) {
            return isBaby ? BABY_PECK_DURATION_TICKS : PECK_DURATION_TICKS;
        }
        if (currentState == CrowState.DISTRESS) {
            return isBaby ? BABY_CAW_DURATION_TICKS : CAW_DURATION_TICKS;
        }
        return isBaby ? BABY_PREEN_DURATION_TICKS : PREEN_DURATION_TICKS;
    }

    private void resetBehaviorController() {
        this.behaviorControllerActive = false;
    }

    private PlayState movementPredicate(AnimationTest<CrowEntity> state) {
        if (this.behaviorControllerActive) {
            return PlayState.STOP;
        }
        if (this.isInSittingPose()) {
            return PlayState.STOP;
        }
        boolean isBaby = this.isBaby();

        // Takeoff animation (one-shot, both ages)
        if (this.takeoffStartTick != -1) {
            int duration = isBaby ? BABY_TAKEOFF_DURATION_TICKS : ADULT_TAKEOFF_DURATION_TICKS;
            if (this.tickCount - this.takeoffStartTick >= duration) {
                this.takeoffStartTick = -1;
            } else {
                state.setAndContinue(RawAnimation.begin().thenPlay("takeoff"));
                return PlayState.CONTINUE;
            }
        }

        // Land animation (one-shot, both ages)
        if (this.landStartTick != -1) {
            int duration = isBaby ? BABY_LAND_DURATION_TICKS : ADULT_LAND_DURATION_TICKS;
            if (this.tickCount - this.landStartTick >= duration) {
                this.landStartTick = -1;
            } else {
                state.setAndContinue(RawAnimation.begin().thenPlay("land"));
                return PlayState.CONTINUE;
            }
        }

        // Airborne
        if (this.isAirborne()) {
            if (this.takeoffStartTick == -1) {
                this.takeoffStartTick = this.tickCount;
            }
            Vec3 vel = this.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
            if (horizontalSpeed > 0.15) {
                state.setAndContinue(RawAnimation.begin().thenLoop("fly"));
            } else {
                state.setAndContinue(RawAnimation.begin().thenLoop("glide"));
            }
            return PlayState.CONTINUE;
        }

        // Ground movement
        if (isBaby) {
            if (state.isMoving()) {
                state.setAndContinue(RawAnimation.begin().thenLoop("hop"));
            } else {
                state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            return PlayState.CONTINUE;
        }

        if (state.isMoving()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
