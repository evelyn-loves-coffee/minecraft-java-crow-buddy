package com.crowbuddy.swarm;

import com.crowbuddy.CrowBuddy;
import com.crowbuddy.entity.CrowEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SwarmManager {

    private static final int SWARM_RADIUS_SQ = 1024;
    private static final int SWARM_CAP = 6;
    private static final long COOLDOWN_TICKS = 300;
    private static final long ESCALATION_WINDOW_TICKS = 600;
    private static final int ESCALATION_THRESHOLD = 3;
    private static final long RETALIATION_TICKS = 40;

    private static final Map<ResourceKey<Level>, SwarmManager> INSTANCE_BY_DIM = new ConcurrentHashMap<>();

    private final Map<Integer, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<Integer, Long> retaliationTimers = new ConcurrentHashMap<>();
    private final Map<Integer, CopyOnWriteArrayList<Long>> escalationHistory = new ConcurrentHashMap<>();

    public static SwarmManager get(Level level) {
        return INSTANCE_BY_DIM.computeIfAbsent(
            level.dimension(),
            k -> new SwarmManager()
        );
    }

    public static void remove(net.minecraft.server.MinecraftServer server, Level level) {
        INSTANCE_BY_DIM.remove(level.dimension());
    }

    public void checkCooldown(int crowId, long currentTick) {
        Long cd = cooldowns.get(crowId);
        if (cd != null && (currentTick - cd) < COOLDOWN_TICKS) {
            cooldowns.put(crowId, cd);
            return;
        }
        cooldowns.remove(crowId);
    }

    public boolean isInCooldown(int crowId, long currentTick) {
        Long cd = cooldowns.get(crowId);
        return cd != null && (currentTick - cd) < COOLDOWN_TICKS;
    }

    public void setCooldown(int crowId, long currentTick) {
        cooldowns.put(crowId, currentTick);
    }

    public void checkRetaliation(int crowId, long currentTick) {
        Long rt = retaliationTimers.get(crowId);
        if (rt != null && (currentTick - rt) >= RETALIATION_TICKS) {
            retaliationTimers.remove(crowId);
        }
    }

    public boolean isInRetaliation(int crowId, long currentTick) {
        Long rt = retaliationTimers.get(crowId);
        return rt != null && (currentTick - rt) < RETALIATION_TICKS;
    }

    private List<CrowEntity> findNearbyCrows(CrowEntity source, Level level) {
        List<CrowEntity> candidates = new java.util.ArrayList<>();
        AABB searchBox = source.getBoundingBox().inflate(Math.sqrt(SWARM_RADIUS_SQ));
        List<CrowEntity> allCrows = level.getEntitiesOfClass(CrowEntity.class, searchBox);

        for (CrowEntity crow : allCrows) {
            if (crow == source) {
                continue;
            }
            if (!crow.isAlive()) {
                continue;
            }
            if (source.distanceToSqr(crow) > SWARM_RADIUS_SQ) {
                continue;
            }
            candidates.add(crow);
        }

        candidates.sort((a, b) -> Double.compare(
            source.distanceToSqr(a),
            source.distanceToSqr(b)
        ));

        int maxResponders = Math.min(SWARM_CAP - 1, candidates.size());
        return candidates.subList(0, maxResponders);
    }

    public void activateSwarmMode(CrowEntity crow, LivingEntity target) {
        if (crow.isInSittingPose()) {
            return;
        }
        crow.getSwarmGoal().setTarget(target);
        if (crow.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double pitch = crow.getRandom().nextFloat() * 0.3f + 0.85f;
            double volume = crow.getRandom().nextFloat() * 0.2f + 0.9f;
            crow.level().playSound(
                null,
                crow.blockPosition(),
                com.crowbuddy.sound.ModSounds.CROW_DISTRESS,
                crow.getSoundSource(),
                (float) volume,
                (float) pitch
            );

            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                com.crowbuddy.networking.ModNetworking.sendDistress(
                    player,
                    crow.getId(),
                    target.blockPosition(),
                    crow.getId()
                );
            }
        }
    }

    public void onCrowDamaged(CrowEntity crow, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!crow.getCarriedItem().isEmpty()) {
            crow.dropCarriedItem();
        }

        Level level = crow.level();
        long currentTick = level.getGameTime();

        if (isInCooldown(crow.getId(), currentTick)) {
            return;
        }

        LivingEntity attacker = source.getEntity() instanceof LivingEntity
            ? (LivingEntity) source.getEntity()
            : null;

        if (attacker == null) {
            return;
        }

        if (!crow.isTame()) {
            triggerSwarm(crow, attacker);
            return;
        }

        if (crow.isInSittingPose()) {
            return;
        }

        recordEscalationHit(crow.getId(), currentTick);

        if (getEscalationCount(crow.getId(), currentTick) >= ESCALATION_THRESHOLD) {
            triggerSwarm(crow, attacker);
            escalationHistory.remove(crow.getId());
            return;
        }

        triggerRetaliation(crow, attacker);
    }

    public void onPlayerAttackCrow(Player player, CrowEntity crow) {
        Level level = crow.level();
        long currentTick = level.getGameTime();

        if (isInCooldown(crow.getId(), currentTick)) {
            return;
        }

        if (!crow.isTame()) {
            triggerSwarm(crow, player);
            return;
        }

        if (crow.isInSittingPose()) {
            return;
        }

        recordEscalationHit(crow.getId(), currentTick);

        if (getEscalationCount(crow.getId(), currentTick) >= ESCALATION_THRESHOLD) {
            triggerSwarm(crow, player);
            escalationHistory.remove(crow.getId());
            return;
        }

        triggerRetaliation(crow, player);
    }

    public void onPlayerAttackTarget(Player player, Entity target) {
        if (target instanceof LivingEntity livingTarget) {
            if (isHostileMob(livingTarget)) {
                CrowEntity nearbyCrow = findNearbyCrow(player);
                if (nearbyCrow != null && nearbyCrow.isTame()) {
                    LivingEntity crowOwner = nearbyCrow.getOwner();
                    if (crowOwner == player) {
                        triggerSwarm(nearbyCrow, livingTarget);
                    }
                }
            }
        }
    }

    public void onNonCrowDamaged(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        return;
    }

    private void triggerSwarm(CrowEntity source, LivingEntity target) {
        Level level = source.level();
        long currentTick = level.getGameTime();
        setCooldown(source.getId(), currentTick);
        activateSwarmMode(source, target);

        List<CrowEntity> responders = findNearbyCrows(source, level);
        CrowBuddy.LOGGER.info(
            "Swarm activated: crow={} target={} responders={}",
            source.getId(), target.getName().getString(), responders.size()
        );

        for (CrowEntity crow : responders) {
            if (isInCooldown(crow.getId(), currentTick)) {
                continue;
            }
            if (crow.isInSittingPose()) {
                continue;
            }
            setCooldown(crow.getId(), currentTick);
            activateSwarmMode(crow, target);
        }
    }

    private void triggerRetaliation(CrowEntity crow, LivingEntity attacker) {
        Level level = crow.level();
        long currentTick = level.getGameTime();
        setRetaliationCooldown(crow.getId(), currentTick);
        activateSwarmMode(crow, attacker);
        CrowBuddy.LOGGER.info(
            "Retaliation: crow={} attacker={}",
            crow.getId(), attacker.getName().getString()
        );
    }

    private void setRetaliationCooldown(int crowId, long currentTick) {
        retaliationTimers.put(crowId, currentTick);
        setCooldown(crowId, currentTick);
    }

    private void recordEscalationHit(int crowId, long gameTick) {
        List<Long> history = escalationHistory.computeIfAbsent(
            crowId, k -> new CopyOnWriteArrayList<>()
        );
        history.removeIf(ts -> (gameTick - ts) > ESCALATION_WINDOW_TICKS);
        history.add(gameTick);
    }

    private int getEscalationCount(int crowId, long gameTick) {
        List<Long> history = escalationHistory.get(crowId);
        if (history == null) {
            return 0;
        }
        history.removeIf(ts -> (gameTick - ts) > ESCALATION_WINDOW_TICKS);
        return history.size();
    }

    private static boolean isHostileMob(LivingEntity entity) {
        if (entity instanceof Monster) {
            return true;
        }
        if (entity instanceof Mob mob && mob.getTarget() != null) {
            return true;
        }
        if (entity.getLastHurtByMob() != null) {
            return true;
        }
        return false;
    }

    private CrowEntity findNearbyCrow(Player player) {
        AABB searchBox = player.getBoundingBox().inflate(32);
        List<CrowEntity> nearbyCrows = player.level().getEntitiesOfClass(CrowEntity.class, searchBox);

        CrowEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (CrowEntity crow : nearbyCrows) {
            if (!crow.isTame()) {
                continue;
            }
            LivingEntity owner = crow.getOwner();
            if (owner != player) {
                continue;
            }
            double dist = player.distanceToSqr(crow);
            if (dist < closestDist) {
                closestDist = dist;
                closest = crow;
            }
        }
        return closest;
    }
}
