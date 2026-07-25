package com.crowbuddy.entity.ai.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AStarPathfinderTest {
    @Test
    void returnsExactTargetForClearDirectRoute() {
        AStarPathfinder planner = new AStarPathfinder((level, pos) -> true, 4);
        Vec3 target = new Vec3(12.5, 8.0, 0.5);
        assertEquals(List.of(target), planner.findPath(null, new Vec3(0.5, 8.0, 0.5), target));
    }

    @Test
    void routesInThreeDimensionsAroundBlockingWall() {
        TerrainSampler wallWithOpenTop = (level, pos) -> !(pos.getX() >= 4 && pos.getX() <= 7 && pos.getY() < 12);
        AStarPathfinder planner = new AStarPathfinder(wallWithOpenTop, 4);
        List<Vec3> path = planner.findPath(null, new Vec3(0.5, 8.0, 0.5), new Vec3(12.5, 8.0, 0.5));
        assertFalse(path.isEmpty());
        assertEquals(new Vec3(12.5, 8.0, 0.5), path.getLast());
        assertFalse(path.stream().allMatch(point -> BlockPos.containing(point).getY() == 8));
    }

    @Test
    void rejectsUnboundedConfiguration() {
        assertThrows(IllegalArgumentException.class,
            () -> new AStarPathfinder((level, pos) -> true, 0, 2_000));
        assertThrows(IllegalArgumentException.class,
            () -> new AStarPathfinder((level, pos) -> true, 4, 0));
    }
}
