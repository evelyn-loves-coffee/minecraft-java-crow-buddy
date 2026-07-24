# Crow Buddy: Phase 5 Low-Level Design (LLD)

## 1. Client Rendering

- `CrowGeoModel` selected adult and baby geometry based on entity age.
- `CrowNestBlockEntityRenderer` rendered eggs during `EGGS` and applied a position-derived static tilt during `HATCHING`; the tilt was not animated over time.
- The renderer omitted fledgling and remnant stages because hatch completion removed the nest.

## 2. Sound Mapping

| Sound | Trigger |
|---|---|
| `CROW_MATE` | Breeding completed |
| `CROW_EGG_LAY` | A parent constructed and started a nest |
| `CROW_HATCH` | Incubation entered hatching |
| `CROW_FLEDGLING` | A baby spawned and the nest disappeared |
| `CROW_GROW` | A baby aged into an adult |
| `CROW_DISTRESS` | Swarm or distress behavior activated |
| `CROW_BABY_FLIGHT` | The event was registered and retained for planned audio, but no gameplay trigger was enacted |

The checked-in `.ogg` files remained placeholders. Candidate source links were retained in `SOUNDS.md`.

## 3. Verification

- Automated verification used `./gradlew clean build --warning-mode all`.
- In-game verification was designed to cover model rendering, sound timing, parent travel, hatch removal, and trampling.

## 4. PAWS Verification

| Pillar | Phase 5 rule |
|---|---|
| Performance | Rendering switched on the compact nest stage and submitted stage-specific geometry without adding server tick work. |
| Auditability | Sound events were centralized, mapped to explicit lifecycle transitions, and documented when registered without a trigger. |
| Workability | Client rendering remained isolated from common code, and automated verification covered 29 tests without failures. |
| Scalability | Entity models, nest rendering, sound registration, and lifecycle effects remained separate and independently replaceable. |

The phase required a clean build with deprecation linting and retained manual in-game checks for visual and audio behavior.

---

## 5. Navigation Refactor - LLD & Requirements

### Objective
Centralize goal and pathfinding systems in the crow-buddy Minecraft mod, then implement intelligent 3D flight pathfinding.

---

### Analysis Summary

#### Goals System (Current State)
- 5 custom goals: ScavengeGoal, SwarmDistressGoal, CrowFlightGoal, HigherGroundStrollGoal, CrowNestBuildGoal
- 8 vanilla goals registered on crow entity
- Goals scattered across `goal/` and `entity/ai/goal/` directories
- Goals define targets only; navigation code decides movement mode (hop vs flight) based on distance/elevation

#### Pathfinding System (Current State)
- Hybrid vanilla `PathNavigation` + duplicated custom PID flight steering in 3+ goals
- No A* implementation; relies on vanilla ground pathfinding + custom flight steering
- Navigation logic duplicated across multiple goals

---

### High-Level Design (HLD)

#### Centralized Goals
- Goals define targets only (position, entity, or condition)
- Goals do NOT contain navigation logic
- `CrowNavigator` decides movement mode based on distance/elevation thresholds

#### Centralized Navigation
- `CrowNavigator` orchestrates path planning and execution
- `FlightNavigator` handles 3D A* pathfinding with terrain awareness
- `TerrainSampler` abstracts block state queries for obstacle detection
- Shared navigation infrastructure across all goals

#### Intelligent 3D Pathfinding
- A* operates in full 3D space
- Grid resolution: 4 blocks (based on analysis)
- Obstacles: solids + leaves + liquids all blocked for flight
- Canopy awareness: detect and navigate around/over tree canopies
- Path smoothing to preserve directional momentum

---

### Low-Level Design (LLD)

#### Core Interfaces

##### TerrainSampler
```java
interface TerrainSampler {
    boolean isPassable(World world, BlockPos pos);
    boolean isSolid(BlockState state);
    boolean isLiquid(BlockState state);
    boolean isLeaf(BlockState state);
    boolean isAir(BlockState state);
    double getObstacleCost(BlockState state);
    boolean isUnderCanopy(World world, BlockPos pos);
    double getCanopyDensity(World world, BlockPos pos);
}
```

##### FlightNavigator
```java
interface FlightNavigator {
    List<Vec3d> findPath(World world, Vec3d start, Vec3d target);
    boolean isPathValid(List<Vec3d> path, World world);
    int getMaxSearchNodes();
}
```

##### CrowNavigator
```java
class CrowNavigator {
    void setTarget(Vec3d target);
    void setTarget(Entity target);
    boolean hasPath();
    boolean isFollowingPath();
    boolean hasReachedTarget();
    void tick(CrowEntity crow);
    MovementMode getMovementMode();
    void invalidatePath();
}
```

#### Core Classes

##### PathNode
- Grid coordinates (x, y, z)
- gCost, hCost, parent reference
- Distance and heuristic calculations

##### AStarPathfinder
- 4-block grid resolution
- 2000 max nodes for search
- Vertical penalty: 1.5x cost
- Canopy exit bonus: -2.0 cost
- Path smoothing (collinear point removal)

##### CrowPathCache
- TTL-based path caching (configurable)
- Detection if crow hasn't progressed toward target
- Automatic path invalidation on world changes

##### ScavengeRegistry
- Shared registry for validating scavenged items across crows
- Thread-safe operations

#### Movement Mode Decision Logic
```
if (horizontalDistance < 8 && verticalDistance < 3) {
    return MovementMode.HOP;
} else {
    return MovementMode.FLY;
}
```

#### Path Following
- Crow follows cached waypoint list
- Re-invoke pathfinding only if stuck (no progress for N ticks)
- Paths preserve directional momentum (no jerky reversals)

---

### Assumptions & Questions (Answered)

| # | Assumption/Question | Answer |
|---|---------------------|--------|
| 1 | Goals define targets only; navigation code decides movement mode | CONFIRMED |
| 2 | Crows never walk/slide on ground; short distances use hops, everything else uses flight | CONFIRMED |
| 3 | `isInMatingState` means "assigned to build nest after breeding" (not "seeking partner") | CONFIRMED - used by CrowNestBuildGoal |
| 4 | CrowNavigator builds waypoint list; crow follows cached path; re-invoke only if stuck | CONFIRMED |
| 5 | Paths must preserve directional momentum (no jerky reversals) | CONFIRMED |
| 6 | A* must operate in full 3D (e.g., crow under tree canopy reaching target above) | CONFIRMED |
| 7 | Grid resolution TBD after analysis | DECIDED: 4 blocks (optimal balance of detail vs performance) |
| 8 | Solids + leaves + liquids are all blocked for flight | CONFIRMED |
| 9 | Shared ScavengeRegistry for validating scavenged items across crows | CONFIRMED |
| 10 | TTL cache for paths with detection if crow hasn't progressed toward target | CONFIRMED |
| 11 | Race conditions in SwarmManager needs code-based guard | CONFIRMED - atomic operations or locks |
| 12 | Test strategy: mocks for pure logic + headless Minecraft test framework | CONFIRMED |

---

### Tree Pathfinding Analysis Results

#### Grid Resolution Analysis
| Scenario | 2b Grid | 4b Grid | 6b Grid | 8b Grid |
|----------|---------|---------|---------|---------|
| Short (20h x 5v) | 300 nodes | 50 nodes | 16 nodes | 9 nodes |
| Medium (40h x 15v) | 3200 nodes | 400 nodes | 147 nodes | 50 nodes |
| Long (80h x 25v) | 20800 nodes | 2800 nodes | 980 nodes | 400 nodes |
| Under-tree (10h x 10v) | 125 nodes | 27 nodes | 8 nodes | 8 nodes |

#### Key Findings
- Max climb angle needed: ~45-60 degrees for tight escapes
- Optimal grid resolution: 4-6 blocks (4b chosen for under-tree detail)
- Path phases: EXIT, CLIMB, CRUISE, OVER, DESCEND
- All scenarios workable with 3D A* at 4b resolution

---

### Implementation Order

1. Create navigation directory structure
2. Implement TerrainSampler interface and DefaultTerrainSampler
3. Implement PathNode and AStarPathfinder
4. Implement FlightNavigator
5. Implement CrowNavigator (central orchestrator)
6. Implement CrowPathCache with TTL
7. Implement ScavengeRegistry
8. Refactor existing goals to use CrowNavigator
9. Add race condition guard to SwarmManager
10. Add tests

---

### Relevant Files

- `/home/evelyn/Apps/crow-buddy/src/main/java/com/crowbuddy/goal/`: Current custom goals
- `/home/evelyn/Apps/crow-buddy/src/main/java/com/crowbuddy/entity/ai/goal/CrowNestBuildGoal.java`: Nest building goal (stays in ai/goal)
- `/home/evelyn/Apps/crow-buddy/src/main/java/com/crowbuddy/entity/CrowEntity.java`: Entity class; holds goal registration, state, navigation field
- `/home/evelyn/Apps/crow-buddy/src/main/java/com/crowbuddy/entity/CrowBehaviorPolicy.java`: Shared thresholds/policy logic
- `/home/evelyn/Apps/crow-buddy/src/main/java/com/crowbuddy/swarm/SwarmManager.java`: Swarm coordination; needs race condition guard
- `/home/evelyn/Apps/crow-buddy/src/main/java/com/crowbuddy/entity/ai/navigation/`: Target directory for new navigation system

---

### Implementation Notes & PAWS Fixes

#### Completed Implementation
- Sections 1-5 fully implemented: TerrainSampler, AStarPathfinder, FlightNavigator, CrowNavigator, goal refactors, SwarmManager fixes, ScavengeRegistry
- All 5 custom goals refactored to use CrowNavigator (ScavengeGoal, CrowFlightGoal, SwarmDistressGoal, HigherGroundStrollGoal, CrowNestBuildGoal)
- CrowEntity holds CrowNavigator field with DefaultFlightNavigator(AStarPathfinder(DefaultTerrainSampler(), 4))
- Build verified successful

#### PAWS P1 Fixes Applied
1. **CrowPathCache TTL**: Converted from System.currentTimeMillis() to game-tick-based (getGameTime()) to avoid desync across server restarts
2. **CrowPathCache dimension tracking**: Added static Map<ResourceKey<Level>, CrowPathCache> with remove(Level) for dimension unload cleanup
3. **CrowNavigator.executeMovement()**: Removed no-op method; navigateTo() now returns true after caching path
4. **CrowEntity cleanup**: Added onRemove(RemovalReason) override to call navigator.clear(this) on entity removal

#### PAWS P2 Fixes Applied
1. **AStarPathfinder cost tuning**: Added vertical penalty (1.5x) and canopy exit bonus (-2.0) to move cost calculation
2. **AStarPathfinder smoothing**: Added smoothPath() with collinear point removal (cross product < 0.01 threshold)
3. **DefaultFlightNavigator.getProgress()**: Fixed to track waypoint index and compute progress along actual path length
4. **ScavengeGoal**: Removed unused MovementMode import
5. **AStarPathfinder passability**: Added start/goal passability checks using terrainSampler.isPassable()

#### MC 26.2 API Adaptations
- ResourceKey<Level> used directly as map key (no location()/getKey() method available)
- Entity.remove(RemovalReason) used for cleanup hook (not onRemove/onEntityRemoved)
- CrowPathCache requires Level parameter in constructor (no no-arg constructor)
