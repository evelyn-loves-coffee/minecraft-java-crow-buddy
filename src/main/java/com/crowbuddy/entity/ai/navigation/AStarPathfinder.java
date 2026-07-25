package com.crowbuddy.entity.ai.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Bounded, terrain-aware 3D A* planner. Every coarse edge is sampled block-by-block. */
public final class AStarPathfinder implements FlightNavigator {
    public static final int DEFAULT_MAX_SEARCH_NODES = 2_000;
    private static final double VERTICAL_COST_MULTIPLIER = 1.5;

    private final TerrainSampler terrain;
    private final int gridSize;
    private final int maxSearchNodes;

    public AStarPathfinder(TerrainSampler terrain, int gridSize) {
        this(terrain, gridSize, DEFAULT_MAX_SEARCH_NODES);
    }

    public AStarPathfinder(TerrainSampler terrain, int gridSize, int maxSearchNodes) {
        if (gridSize < 1 || maxSearchNodes < 1) throw new IllegalArgumentException("positive limits required");
        this.terrain = terrain;
        this.gridSize = gridSize;
        this.maxSearchNodes = maxSearchNodes;
    }

    @Override
    public List<Vec3> findPath(Level level, Vec3 start, Vec3 target) {
        BlockPos startPos = BlockPos.containing(start);
        BlockPos targetPos = BlockPos.containing(target);
        if (!terrain.isPassable(level, startPos) || !terrain.isPassable(level, targetPos)) return List.of();
        if (segmentClear(level, startPos, targetPos)) return List.of(target);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::score));
        Map<BlockPos, Double> best = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        Node first = new Node(startPos, 0.0, heuristic(startPos, targetPos), null);
        open.add(first);
        best.put(startPos, 0.0);
        Node closest = first;
        int expanded = 0;

        while (!open.isEmpty() && expanded++ < maxSearchNodes) {
            Node current = open.poll();
            if (!closed.add(current.pos)) continue;
            if (heuristic(current.pos, targetPos) < heuristic(closest.pos, targetPos)) closest = current;
            if (segmentClear(level, current.pos, targetPos)) {
                return smooth(level, reconstruct(current, target));
            }
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if ((dx | dy | dz) == 0) continue;
                BlockPos next = current.pos.offset(dx * gridSize, dy * gridSize, dz * gridSize);
                if (closed.contains(next) || !terrain.isPassable(level, next)
                        || !segmentClear(level, current.pos, next)) continue;
                double step = Math.sqrt(dx * dx + dz * dz + dy * dy * VERTICAL_COST_MULTIPLIER * VERTICAL_COST_MULTIPLIER) * gridSize;
                double candidate = current.g + step;
                if (candidate >= best.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                best.put(next, candidate);
                open.add(new Node(next, candidate, heuristic(next, targetPos), current));
            }
        }
        return List.of();
    }

    private boolean segmentClear(Level level, BlockPos from, BlockPos to) {
        int steps = Math.max(1, Math.max(Math.abs(to.getX() - from.getX()),
            Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ()))));
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            BlockPos sample = BlockPos.containing(
                from.getX() + (to.getX() - from.getX()) * t,
                from.getY() + (to.getY() - from.getY()) * t,
                from.getZ() + (to.getZ() - from.getZ()) * t);
            if (!terrain.isPassable(level, sample)) return false;
        }
        return true;
    }

    private List<Vec3> reconstruct(Node node, Vec3 exactTarget) {
        List<Vec3> path = new ArrayList<>();
        for (Node cursor = node; cursor.parent != null; cursor = cursor.parent) {
            path.add(Vec3.atCenterOf(cursor.pos));
        }
        Collections.reverse(path);
        path.add(exactTarget);
        return path;
    }

    private List<Vec3> smooth(Level level, List<Vec3> path) {
        if (path.size() < 3) return path;
        List<Vec3> result = new ArrayList<>();
        int anchor = 0;
        result.add(path.getFirst());
        while (anchor < path.size() - 1) {
            int next = path.size() - 1;
            BlockPos from = BlockPos.containing(path.get(anchor));
            while (next > anchor + 1 && !segmentClear(level, from, BlockPos.containing(path.get(next)))) next--;
            result.add(path.get(next));
            anchor = next;
        }
        return result;
    }

    @Override
    public boolean isPathValid(Level level, List<Vec3> path) {
        for (Vec3 waypoint : path) if (!terrain.isPassable(level, BlockPos.containing(waypoint))) return false;
        return !path.isEmpty();
    }

    @Override public int getMaxSearchNodes() { return maxSearchNodes; }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dy = (a.getY() - b.getY()) * VERTICAL_COST_MULTIPLIER, dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private record Node(BlockPos pos, double g, double h, Node parent) {
        double score() { return g + h; }
    }
}
