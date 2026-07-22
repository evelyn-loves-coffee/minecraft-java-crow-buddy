package com.crowbuddy.entity;

import com.crowbuddy.CrowBuddy;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.EasingType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.crowbuddy.goal.CrowFlightGoal;
import com.crowbuddy.goal.HigherGroundStrollGoal;
import com.crowbuddy.goal.ScavengeGoal;
import com.crowbuddy.goal.SwarmDistressGoal;
import com.crowbuddy.item.ModItems;
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

    private static final EntityDataAccessor<Integer> STATE =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.INT);

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

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private ScavengeGoal scavengeGoal;
    private SwarmDistressGoal swarmGoal;
    private boolean behaviorControllerActive = false;
    private int oneShotStartTick = -1;
    private int takeoffStartTick = -1;
    private int landStartTick = -1;
    private int begUntilTick = -1;
    private int nextBegTick = 0;
    private int nextPreenTick = 0;
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
    private static final float FEEDING_SATIATION = 0.25f;

    public CrowEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.TEMPT_RANGE, 3.0)
            .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.swarmGoal = new SwarmDistressGoal(this, SwarmManager.get(this.level()), SwarmDistressGoal.Mode.SWARM);
        this.goalSelector.addGoal(0, this.swarmGoal);
        this.goalSelector.addGoal(1, new com.crowbuddy.entity.ai.goal.CrowNestSeekGoal(this, 1.0, 1200));
        this.goalSelector.addGoal(1, new CrowFlightGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.BreedGoal(this, 1.0, CrowEntity.class));
        final CrowEntity self = this;
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(
            this, 1.0, 6.0f, 10.0f));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.TemptGoal(
            this, 1.25, itemStack -> this.isFood(itemStack), false));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.FollowParentGoal(this, 1.0));
        this.goalSelector.addGoal(3, new com.crowbuddy.goal.BabyNestReturnGoal(this));
        this.scavengeGoal = new ScavengeGoal(this);
        this.goalSelector.addGoal(5, this.scavengeGoal);
        this.goalSelector.addGoal(6, new HigherGroundStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                if (self.isAirborne() || self.isInSittingPose()) return false;
                net.minecraft.core.BlockPos nest = self.getHomeNestPos();
                if (self.isBaby() && nest != null
                        && self.distanceToSqr(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5) > 400.0) {
                    return false;
                }
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, CrowState.IDLE.stateId());
        builder.define(CARRIED_ITEM, ItemStack.EMPTY);
        builder.define(SATIATION, 1.0f);
        builder.define(RELATIONSHIP, 0.0f);
        builder.define(IN_MATING_STATE, false);
        builder.define(AIRBORNE, false);
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
        this.setNoGravity(airborne);
    }

    public boolean isAirborne() {
        return this.entityData.get(AIRBORNE);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier,
                                   net.minecraft.world.damagesource.DamageSource source) {
        this.resetFallDistance();
        return false;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        if (!super.checkSpawnRules(level, reason)) return false;
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }
        net.minecraft.core.BlockPos pos = this.blockPosition();
        int surfaceY = level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return pos.getY() >= surfaceY - 1 && level.canSeeSky(pos);
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
        output.putInt("crowState", this.getState().stateId());
        output.putFloat("satiation", this.getSatiation());
        output.putFloat("relationship", this.getRelationship());
        output.putBoolean("inMatingState", this.isInMatingState());
        output.store("carriedItem", ItemStack.OPTIONAL_CODEC, this.getCarriedItem());
        output.putBoolean("hasHomeNest", this.homeNestPos != null);
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
        // Shoulder perching was removed. Clear no-gravity when loading legacy saves.
        this.setNoGravity(false);
        this.setState(CrowState.fromStateId(input.getIntOr("crowState", CrowState.IDLE.stateId())));
        this.setSatiation(input.getFloatOr("satiation", 1.0f));
        this.setRelationship(CrowBehaviorPolicy.clampRelationship(input.getFloatOr("relationship", 0.0f)));
        this.setInMatingState(input.getBooleanOr("inMatingState", false));
        this.setCarriedItem(input.read("carriedItem", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        if (!input.getBooleanOr("hasHomeNest", false)) return;
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
        if (itemStack.is(PARROT_POISONOUS)) {
            return false;
        }
        if (item == ModItems.BLACK_OIL_SUNFLOWER_SEEDS) {
            return true;
        }
        if (itemStack.is(PARROT_FOOD)) {
            return true;
        }
        return false;
    }

    public boolean isBreedingItem(ItemStack itemStack) {
        return isFood(itemStack) || itemStack.is(Items.GOLDEN_DANDELION);
    }

    public static boolean isPoisonousFood(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == net.minecraft.world.item.Items.COCOA_BEANS || itemStack.is(PARROT_POISONOUS);
    }

    @Override
    public void tame(Player player) {
        super.tame(player);
        this.setSatiation(1.0f);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (isPoisonousFood(itemStack)) {
            if (this.level().isClientSide()) return InteractionResult.SUCCESS;
            this.usePlayerItem(player, hand, itemStack);
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.POISON, 200));
            return InteractionResult.SUCCESS;
        }
        if (this.isFood(itemStack)) {
            if (CrowBehaviorPolicy.isBreedingCooldown(this.getAge())) {
                return InteractionResult.PASS;
            }
            boolean isTamingFood = itemStack.is(ModItems.BLACK_OIL_SUNFLOWER_SEEDS);
            if (this.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (CrowBehaviorPolicy.shouldSpeedUpGrowth(this.isBaby(), true)) {
                InteractionResult growthResult = super.mobInteract(player, hand);
                boolean consumedFood = growthResult.consumesAction();
                if (consumedFood) {
                    this.finishFeeding();
                }
                if (CrowBehaviorPolicy.shouldEmitGrowthParticles(true, consumedFood)) {
                    this.broadcastGrowthParticles();
                }
                return growthResult;
            }

            if (this.isTame()) {
                InteractionResult breedingResult = super.mobInteract(player, hand);
                if (breedingResult.consumesAction()) {
                    this.finishFeeding();
                    return breedingResult;
                }
            }

            this.feed(player, hand, itemStack, 2.0f, 2.0f);
            this.finishFeeding();
            if (CrowBehaviorPolicy.canAttemptTaming(this.isBaby(), this.isTame(), isTamingFood)) {
                boolean tamed = this.getRandom().nextInt(
                    CrowBehaviorPolicy.TAMING_CHANCE_DENOMINATOR) == 0;
                if (tamed) {
                    this.tame(player);
                }
                this.level().broadcastEntityEvent(this, tamed ? (byte) 7 : (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }

        InteractionResult foodResult = super.mobInteract(player, hand);
        if (foodResult.consumesAction()) {
            return foodResult;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!this.isTame() || !this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (player.isCrouching()) {
            this.setOrderedToSit(!this.isOrderedToSit());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private void finishFeeding() {
        this.setSatiation(this.getSatiation() + FEEDING_SATIATION);
        this.setRelationship(Math.min(1.0f, this.getRelationship() + 0.05f));
        this.dropCarriedItem();
    }

    private void broadcastGrowthParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        serverLevel.sendParticles(
            net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
            this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(),
            7, 0.25, 0.2, 0.25, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAirborne()) {
            if (!this.level().isClientSide()) {
                this.maintainForwardFlightMomentum();
            }
            this.alignBodyWithFlight();
        }
        if (this.level().isClientSide()) {
            return;
        }
        float satiation = this.getSatiation();
        if (satiation > 0.0f) {
            this.setSatiation(satiation - 0.0005f);
        }
        if (this.isInSittingPose()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.25, 0.5));
            return;
        }
    }

    private void maintainForwardFlightMomentum() {
        Vec3 velocity = this.getDeltaMovement();
        double minimumSpeed = CrowBehaviorPolicy.minimumFlightSpeed(this.isBaby());
        double horizontalSpeed = velocity.horizontalDistance();
        if (horizontalSpeed >= minimumSpeed) return;

        Vec3 forward;
        if (horizontalSpeed > 1.0E-4) {
            forward = new Vec3(velocity.x / horizontalSpeed, 0.0, velocity.z / horizontalSpeed);
        } else {
            forward = Vec3.directionFromRotation(0.0f, this.getYRot());
        }
        this.setDeltaMovement(forward.x * minimumSpeed, velocity.y, forward.z * minimumSpeed);
    }

    private void alignBodyWithFlight() {
        Vec3 velocity = this.getDeltaMovement();
        float flightYaw = CrowBehaviorPolicy.flightYawDegrees(
            velocity.x, velocity.z, this.getYRot());
        this.setYRot(flightYaw);
        this.yBodyRot = flightYaw;
        this.yHeadRot = flightYaw;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        // BY DESIGN: Crow breeding is handled entirely through the CrowNest block entity,
        // not the vanilla breeding mechanic. Eggs are laid in nests and hatch there.
        return null;
    }

    @Override
    public boolean canFallInLove() {
        return CrowBehaviorPolicy.canEnterLoveMode(
            this.isTame(), this.isBaby(), this.getAge(), this.isInLove(),
            com.crowbuddy.entity.ai.goal.CrowNestSeekGoal.findNearestAvailableNest(this) != null);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel serverLevel, net.minecraft.world.entity.animal.Animal other) {
        this.setAge(CrowBehaviorPolicy.BREEDING_COOLDOWN_TICKS);
        this.resetLove();
        this.setInMatingState(true);
        if (other instanceof CrowEntity otherCrow) {
            otherCrow.setAge(CrowBehaviorPolicy.BREEDING_COOLDOWN_TICKS);
            otherCrow.resetLove();
            // A single parent seeks a nest so one courtship cannot fill two nests.
            otherCrow.setInMatingState(false);
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
            new AnimationController<CrowEntity>("behaviorController", 3, this::behaviorPredicate)
                .setOverrideEasingType(EasingType.EASE_IN_OUT_SINE)
        );
        controllerRegistrar.add(
            new AnimationController<CrowEntity>("movementController", 5, this::movementPredicate)
                .setOverrideEasingType(EasingType.EASE_IN_OUT_SINE)
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
        if (isBaby && this.tickCount < this.begUntilTick) {
            state.setAndContinue(RawAnimation.begin().thenLoop("beg"));
            return PlayState.CONTINUE;
        }
        if (isBaby && !state.isMoving() && this.getSatiation() < 0.4f
                && this.tickCount >= this.nextBegTick) {
            this.begUntilTick = this.tickCount + 30 + this.getRandom().nextInt(31);
            this.nextBegTick = this.begUntilTick + 160 + this.getRandom().nextInt(161);
            state.setAndContinue(RawAnimation.begin().thenLoop("beg"));
            return PlayState.CONTINUE;
        }
        CrowState currentState = this.getState();
        if (currentState == CrowState.COMBAT
                && (this.getTarget() != null || (this.swarmGoal != null && this.swarmGoal.getTarget() != null))) {
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
                && this.tickCount >= this.nextPreenTick) {
            this.nextPreenTick = this.tickCount + 400 + this.getRandom().nextInt(401);
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
            state.setAndContinue(RawAnimation.begin().thenLoop("fly"));
            return PlayState.CONTINUE;
        }

        // Ground movement
        // Animation predicates run client-side, where server path-navigation state is
        // not synchronized. GeckoLib's movement signal is derived from rendered motion.
        boolean isMoving = state.isMoving();
        if (isBaby) {
            if (isMoving) {
                state.setAndContinue(RawAnimation.begin().thenLoop("hop"));
            } else {
                state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
            }
            return PlayState.CONTINUE;
        }

        if (isMoving) {
            state.setAndContinue(RawAnimation.begin().thenLoop("hop"));
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
