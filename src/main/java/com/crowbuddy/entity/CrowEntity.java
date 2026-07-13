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
import net.minecraft.world.level.Level;
import com.crowbuddy.goal.ScavengeGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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
    private static final EntityDataAccessor<ItemStack> CARRIED_ITEM =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> SATIATION =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RELATIONSHIP =
        SynchedEntityData.defineId(CrowEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private ScavengeGoal scavengeGoal;

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
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.TemptGoal(
            this, 1.25, itemStack -> this.isFood(itemStack), false));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(
            this, 1.0, 6.0f, 10.0f));
        this.scavengeGoal = new ScavengeGoal(this);
        this.goalSelector.addGoal(3, this.scavengeGoal);
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PERCHED, false);
        builder.define(STATE, CrowState.IDLE.ordinal());
        builder.define(CARRIED_ITEM, ItemStack.EMPTY);
        builder.define(SATIATION, 1.0f);
        builder.define(RELATIONSHIP, 0.0f);
    }

    public void setPerched(boolean perched) {
        this.entityData.set(PERCHED, perched);
    }

    public boolean isPerched() {
        return this.entityData.get(PERCHED);
    }

    public void setState(CrowState state) {
        this.entityData.set(STATE, state.ordinal());
    }

    public CrowState getState() {
        int ordinal = this.entityData.get(STATE);
        CrowState[] states = CrowState.values();
        return ordinal >= 0 && ordinal < states.length ? states[ordinal] : CrowState.IDLE;
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

    public LivingEntity getOwner() {
        return net.minecraft.world.entity.EntityReference.getLivingEntity(
            this.getOwnerReference(), this.level());
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

    public static boolean isPoisonousFood(ItemStack itemStack) {
        Item item = itemStack.getItem();
        return item == net.minecraft.world.item.Items.COCOA_BEANS || isInTag(item, PARROT_POISONOUS);
    }

    private static boolean isInTag(Item item, TagKey<Item> tag) {
        for (net.minecraft.core.Holder<Item> h :
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            if (h.value() == item) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tame(net.minecraft.world.entity.player.Player player) {
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
            double speed = Math.min(Math.sqrt(distSq) * 0.5, 0.3);
            double dist = Math.sqrt(distSq);
            this.setDeltaMovement(
                this.getDeltaMovement().add(dx / dist * speed, dy / dist * speed, dz / dist * speed)
            );
        }
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(
            new AnimationController<>("controller", 0, this::predicate)
        );
    }

    private PlayState predicate(AnimationTest<CrowEntity> state) {
        if (this.isInSittingPose()) {
            return PlayState.STOP;
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
