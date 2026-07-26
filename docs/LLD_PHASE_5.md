# Crow Buddy: Phase 5 Low-Level Design (LLD)

## 1. Client Rendering

- `CrowGeoModel` selected adult and baby geometry based on entity age.
- `CrowNestBlockEntityRenderer` rendered eggs during `EGGS` and applied a position-derived static tilt during `HATCHING`; the tilt was not animated over time.
- The renderer omitted fledgling and remnant stages because hatch completion removed the nest.

## 2. Sound Mapping

| Sound | Trigger | Implementation |
|---|---|---|
| `CROW_MATE` | Breeding completed | `CrowEntity.breed()` |
| `CROW_EGG_LAY` | A parent constructed and started a nest | `CrowNestBlockEntity.startIncubation()` |
| `CROW_HATCH` | Incubation entered hatching (EGGS → HATCHING) | `CrowNestBlockEntity.advanceStage()` with happy villager particles |
| `CROW_FLEDGLING` | A baby spawned and the nest disappeared | `CrowNestBlockEntity.advanceStage()` with crit particles |
| `CROW_GROW` | A baby aged into an adult | `CrowEntity.ageUp()` |
| `CROW_DISTRESS` | Swarm or distress behavior activated | `SwarmManager.onCrowDamaged()` and `SwarmDistressGoal` |
| `CROW_BABY_FLIGHT` | Baby crow takes its first flight after growing | `CrowEntity.ageUp()` when baby transitions to adult |

All seven sound events are registered in `ModSounds.java` and mapped in `assets/crowbuddy/sounds.json`. The checked-in `.ogg` files are placeholders; candidate source links are retained in `SOUNDS.md`. Sound triggers fire regardless of whether actual audio files are present.

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
---

## 5. Navigation Refactor - Implementation

### Objective
Centralize goal and pathfinding systems in the crow-buddy Minecraft mod, then implement intelligent 3D flight pathfinding.

### Status
Completed. Verified against Minecraft 26.2 mappings. Build passes with 38 tests, zero failures.

### Architecture

#### Core Principle
Goals define intent (destination, entity, or condition). CrowNavigator owns all planning and steering. Goals never directly manipulate pathfinding or velocity.

#### Component Overview

| Component | Responsibility | File |
|---|---|---|
| TerrainSampler | Single-method interface for passability checks | entity/ai/navigation/TerrainSampler.java |
| DefaultTerrainSampler | Conservative flight clearance: blocks solids, leaves, fluids, out-of-bounds | entity/ai/navigation/DefaultTerrainSampler.java |
| FlightNavigator | Interface for pathfinding (implemented by AStarPathfinder) | entity/ai/navigation/FlightNavigator.java |
| AStarPathfinder | Bounded 3D A* with terrain sampling and path smoothing | entity/ai/navigation/AStarPathfinder.java |
| CrowNavigator | Central orchestrator: mode selection, path following, steering | entity/ai/navigation/CrowNavigator.java |
| CrowPathCache | Per-entity TTL-based path cache (40 game ticks) | entity/ai/navigation/CrowPathCache.java |
| MovementMode | Enum: HOP (vanilla navigation) vs FLY (custom steering) | entity/ai/navigation/MovementMode.java |
| ScavengeRegistry | Atomic per-dimension item claims across crows | goal/ScavengeRegistry.java |

#### TerrainSampler Interface
```java
@FunctionalInterface
interface TerrainSampler {
    boolean isPassable(Level level, BlockPos pos);
}
```
Simplified from initial design to a single method. DefaultTerrainSampler rejects:
- Positions outside world border, below minY, or at/above maxY
- Blocks with fluid state
- Blocks tagged #minecraft:leaves
- Blocks with non-empty collision shape

#### FlightNavigator Interface
```java
interface FlightNavigator {
    List<Vec3> findPath(Level level, Vec3 start, Vec3 target);
    boolean isPathValid(Level level, List<Vec3> path);
    int getMaxSearchNodes();
}
```

#### AStarPathfinder Implementation
- 4-block grid resolution (configurable constructor parameter)
- 2,000 max search nodes (configurable, default 2000)
- 26 three-dimensional neighbors per node
- Block-by-block edge validation via segmentClear() along each coarse edge
- Vertical cost multiplier: 1.5x (no canopy exit bonus in final implementation)
- Path smoothing: line-of-sight shortcut removal after path reconstruction
- Returns empty list if no path found; caller falls back to direct target
- Start and goal passability checked before search begins

#### CrowNavigator Implementation
- Entity-owned: instantiated in CrowEntity constructor with new AStarPathfinder(new DefaultTerrainSampler(), 4)
- Movement mode decision via CrowBehaviorPolicy.shouldUseGroundHop():
  - HOP if |verticalDifference| <= 0.5 and horizontalDistanceSq <= 9.0
  - FLY otherwise
- HOP mode: delegates to vanilla PathNavigation.moveTo()
- FLY mode: stops vanilla navigation, requests flight path, applies custom PID-like steering
- Dynamic entity targets refreshed when target moves >1 block
- Path cache expires after 40 game ticks (tick-based, not wall-clock)
- Replans after 20 ticks without measurable progress (stall detection)
- Arrival: scales velocity to 35%, clears AIRBORNE, triggers land animation
- Grounded low-velocity safety check in clear() repairs stale airborne state

#### CrowPathCache Implementation
- Per-entity (owned by CrowNavigator instance)
- TTL: 40 game ticks using Level.getGameTime()
- Cache hit: same target within 1 block and not expired
- Stores immutable copy of path via List.copyOf()

#### ScavengeRegistry Implementation
- Static ConcurrentHashMap<ResourceKey<Level>, ScavengeRegistry> for per-dimension instances
- Internal ConcurrentHashMap<Integer, Integer> maps item entity ID to crow entity ID
- claim(itemId, crowId): atomic via putIfAbsent; returns true if this crow owns or newly claims
- releaseAll(crowId): removes all claims by a specific crow
- isClaimedByOther(itemId, crowId): checks if another crow owns this item
- Cleanup on dimension unload via ServerLevelEvents.UNLOAD in CrowBuddy.onInitialize()

#### Movement Mode Decision Logic
```java
// CrowBehaviorPolicy.shouldUseGroundHop()
return Math.abs(verticalDifference) <= 0.5 && horizontalDistanceSq <= 9.0;
```

#### Path Following
- Crow follows cached waypoint list with momentum blending
- Steering: desired velocity scaled by speed (0.30 adult, 0.20 baby), blended with current movement at 72%/28%
- Re-invoke pathfinding only if stuck (20 ticks without progress) or cache expired
- Paths preserve directional momentum via smoothing and blending

### Runtime Flow

1. A goal selects a destination and calls CrowNavigator.navigateTo(destination, speed).
2. CrowNavigator applies hop thresholds or requests a cached/new 3D flight path.
3. AStarPathfinder samples every coarse-grid edge through TerrainSampler; solids, leaves, fluids, world-border positions, and height violations are rejected.
4. CrowNavigator.tick() follows smoothed waypoints with momentum blending and triggers bounded replanning on target movement, TTL expiry on next request, or stall.
5. Goal stop, sitting, landing, attack range, and entity removal clear navigation ownership.

### Refactored Goals

All 5 custom goals refactored to use CrowNavigator:
- ScavengeGoal: selects target item via ScavengeRegistry, delegates movement to CrowNavigator
- CrowFlightGoal: requests flight destination, CrowNavigator handles pathfinding
- SwarmDistressGoal: targets distressed crow or attacker via CrowNavigator
- HigherGroundStrollGoal: selects elevated position, CrowNavigator navigates
- CrowNestBuildGoal: acquires MOVE control only after valid nest position found; uses CrowNavigator for travel

Goals retain lifecycle-only takeoff/landing velocity changes but no longer implement travel steering or issue path-navigation requests.

### PAWS Fixes Applied

#### P1 Performance
1. CrowPathCache TTL: game-tick-based (getGameTime()) instead of System.currentTimeMillis() to avoid desync
2. CrowPathCache dimension tracking: static Map with remove(Level) for dimension unload cleanup

#### P2 Auditability
1. AStarPathfinder cost tuning: vertical penalty (1.5x) applied to move cost calculation
2. AStarPathfinder smoothing: line-of-sight shortcut removal after reconstruction
3. AStarPathfinder passability: start/goal passability checks using terrainSampler.isPassable()

#### P3 Workability
1. CrowNestBuildGoal: acquires MOVE control only after valid nest position (prevents stationary timeout)
2. Food temptation: selects nearest tempting player, delegates movement to CrowNavigator
3. Flight arrival: clears AIRBORNE/no-gravity, triggers landing; grounded safety check in clear()
4. CrowEntity onRemove(RemovalReason): calls navigator.clear(this) on entity removal

### MC 26.2 API Adaptations
- ResourceKey<Level> used directly as map key in ScavengeRegistry
- Entity removal via onRemove(RemovalReason) override in CrowEntity
- CrowPathCache uses Level.getGameTime() for tick-based TTL

### Relevant Files

- src/main/java/com/crowbuddy/entity/ai/navigation/: Navigation system (7 files)
- src/main/java/com/crowbuddy/goal/ScavengeRegistry.java: Shared scavenge claims
- src/main/java/com/crowbuddy/goal/: Refactored custom goals
- src/main/java/com/crowbuddy/entity/ai/goal/CrowNestBuildGoal.java: Nest building goal (uses CrowNavigator)
- src/main/java/com/crowbuddy/entity/CrowEntity.java: Entity class; owns CrowNavigator instance
- src/main/java/com/crowbuddy/entity/CrowBehaviorPolicy.java: Movement mode thresholds and behavior policies

### Remaining Empirical Verification

Automated tests cover direct-path selection, vertical obstacle routing, planner configuration validation, atomic scavenge claims, and behavior policies/state machines.

In-game GameTests still needed for:
- Entity-sized clearance in tight spaces
- Moving-target combat pursuit
- Dense-canopy escape routing
- Goal preemption and CrowNavigator handoff
- Chunk-edge pathfinding behavior
- Visible hop/flight mode transitions

The planner deliberately falls back to the exact target when its bounded search finds no path, preserving behavior liveness. This fallback should be observed in adversarial terrain.
