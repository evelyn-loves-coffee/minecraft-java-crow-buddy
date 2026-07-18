package com.crowbuddy.goal;

import com.crowbuddy.entity.CrowEntity;
import com.crowbuddy.entity.CrowState;
import com.crowbuddy.networking.ModNetworking;
import com.crowbuddy.sound.ModSounds;
import com.crowbuddy.swarm.SwarmManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

public class SwarmDistressGoal extends Goal {

    private static final int DISTRESS_SOUND_INTERVAL = 20;
    private static final double ATTACK_RANGE_SQ = 1.5 * 1.5;
    private static final long PLAYER_WINDOW_TICKS = 80;
    private static final long RETALIATION_DURATION = 40;

    public enum Mode {
        RETALIATION,
        SWARM
    }

    private final CrowEntity crow;
    private final SwarmManager swarmManager;
    private final Mode mode;

    private LivingEntity target;
    private long tickTimer;
    private long lastHitTime;
    private long swarmStartTime;

    public SwarmDistressGoal(CrowEntity crow, SwarmManager swarmManager, Mode mode) {
        this.crow = crow;
        this.swarmManager = swarmManager;
        this.mode = mode;
        this.tickTimer = 0;
        this.lastHitTime = 0;
        this.swarmStartTime = 0;
    }

    // Public setters for external state updates
    public void setTarget(LivingEntity target) {
        this.target = target;
        if (target instanceof Player) {
            this.lastHitTime = this.crow.level().getGameTime();
        }
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public void setLastHitTime(long time) {
        this.lastHitTime = time;
    }

    public void clearTarget() {
        this.target = null;
    }

    @Override
    public boolean canUse() {
        if (this.crow.isInSittingPose()) {
            return false;
        }
        if (this.crow.isPerched()) {
            return false;
        }
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }

        if (this.mode == Mode.RETALIATION) {
            return this.swarmManager.isInRetaliation(
                this.crow.getId(), this.crow.level().getGameTime()
            );
        }

        return this.checkTargetWindow();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.crow.isInSittingPose()) {
            return false;
        }
        if (this.crow.isPerched()) {
            return false;
        }
        if (this.target == null || !this.target.isAlive()) {
            return false;
        }

        if (this.mode == Mode.RETALIATION) {
            return this.swarmManager.isInRetaliation(
                this.crow.getId(), this.crow.level().getGameTime()
            );
        }

        return this.checkTargetWindow();
    }

    private boolean checkTargetWindow() {
        if (this.target instanceof Player) {
            long currentTime = this.crow.level().getGameTime();
            return (currentTime - this.lastHitTime) < PLAYER_WINDOW_TICKS;
        }
        return true;
    }

    @Override
    public void start() {
        this.crow.setState(CrowState.COMBAT);
        this.tickTimer = 0;
        this.swarmStartTime = this.crow.level().getGameTime();
        if (this.target instanceof Player) {
            this.lastHitTime = this.crow.level().getGameTime();
        }
    }

    @Override
    public void stop() {
        this.crow.setState(CrowState.IDLE);
        this.target = null;
        this.lastHitTime = 0;
    }

    @Override
    public void tick() {
        if (!this.crow.isAlive()) {
            return;
        }

        if (!this.target.isAlive()) {
            this.stop();
            return;
        }

        this.tickTimer++;

        if (this.tickTimer % DISTRESS_SOUND_INTERVAL == 0) {
            playDistressSound();
        }

        double distSq = this.crow.distanceToSqr(this.target);

        if (distSq <= ATTACK_RANGE_SQ) {
            this.crow.getNavigation().stop();
            performAttack();
        } else {
            navigateToTarget();
        }
    }

    private void playDistressSound() {
        if (this.crow.level().isClientSide()) {
            return;
        }
        float pitch = this.crow.getRandom().nextFloat() * 0.3f + 0.85f;
        float volume = this.crow.getRandom().nextFloat() * 0.2f + 0.9f;
        this.crow.level().playSound(
            null,
            this.crow.blockPosition(),
            ModSounds.CROW_DISTRESS,
            this.crow.getSoundSource(),
            volume,
            pitch
        );
    }

    private void navigateToTarget() {
        PathNavigation navigation = (PathNavigation) this.crow.getNavigation();
        float speed = 1.0f;
        navigation.moveTo(this.target, speed);
    }

    private void performAttack() {
        if (!this.crow.canAttack(this.target)) {
            this.stop();
            return;
        }

        long currentTick = this.crow.level().getGameTime();

        if (this.crow.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            if (this.crow.doHurtTarget(serverLevel, this.target)) {
                this.lastHitTime = currentTick;
            }
        }
    }

    public boolean isTargetHostile(LivingEntity entity) {
        if (entity instanceof Monster) {
            return true;
        }
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            return mob.getTarget() != null;
        }
        return entity.getLastHurtByMob() != null;
    }
}
