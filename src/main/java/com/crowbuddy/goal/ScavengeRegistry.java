package com.crowbuddy.goal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Atomic per-dimension item claims prevent two crows selecting the same entity. */
public final class ScavengeRegistry {
    private static final Map<ResourceKey<Level>, ScavengeRegistry> BY_DIMENSION = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Integer> claims = new ConcurrentHashMap<>();
    private ScavengeRegistry() {}
    public static ScavengeRegistry get(Level level) { return BY_DIMENSION.computeIfAbsent(level.dimension(), ignored -> new ScavengeRegistry()); }
    public static void remove(Level level) { BY_DIMENSION.remove(level.dimension()); }
    public boolean claim(int itemId, int crowId) { return claims.putIfAbsent(itemId, crowId) == null || claims.get(itemId) == crowId; }
    public void releaseAll(int crowId) { claims.entrySet().removeIf(entry -> entry.getValue() == crowId); }
    public boolean isClaimedByOther(int itemId, int crowId) { Integer owner = claims.get(itemId); return owner != null && owner != crowId; }
}
