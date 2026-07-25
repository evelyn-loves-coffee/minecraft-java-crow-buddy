package com.crowbuddy.entity.ai.navigation;

import java.util.List;
import net.minecraft.world.phys.Vec3;

final class CrowPathCache {
    private static final long TTL_TICKS = 40;
    private List<Vec3> path = List.of();
    private Vec3 target;
    private long createdTick;

    boolean matches(Vec3 requested, long tick) {
        return target != null && target.distanceToSqr(requested) <= 1.0 && tick - createdTick < TTL_TICKS && !path.isEmpty();
    }
    List<Vec3> path() { return path; }
    void store(Vec3 requested, List<Vec3> value, long tick) { target = requested; path = List.copyOf(value); createdTick = tick; }
    void clear() { target = null; path = List.of(); createdTick = 0; }
}
