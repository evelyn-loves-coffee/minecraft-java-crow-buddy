package com.crowbuddy.entity.ai.navigation;

import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface FlightNavigator {
    List<Vec3> findPath(Level level, Vec3 start, Vec3 target);
    boolean isPathValid(Level level, List<Vec3> path);
    int getMaxSearchNodes();
}
