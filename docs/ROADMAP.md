# Crow Buddy: Master Implementation Plan

## 1. Project Overview
The Crow Buddy mod aims to add a vanilla-plus, tameable crow entity to Minecraft. It mirrors real-life crow presence through biome-specific spawning and shares behaviors with Parrots (flight, shoulder-sitting) while introducing unique crow-centric mechanics.

## 1.1. Overall Status
| Phase | Status | Completion |
| :--- | :--- | :--- |
| Phase 1: Dependency & Environment Setup | ✅ Complete | 100% |
| Phase 2: Foundation - Registration & Data Generation | ✅ Complete | 100% |
| Phase 3: Networking & Core Mechanics | 🔄 In Progress | 7/8 (Swarm Intelligence pending — `SwarmManager` + `SwarmDistressGoal` + `CrowEventHub` wiring) |
| Phase 4: World & Environment | ⏳ Not Started | 0% |
| Phase 5: Polishing & Verification | ⏳ Not Started | 0% |

## 2. Technical Choices & Design Decisions
| Feature | Choice | Rationale |
| :--- | :--- | :--- |
| **Animation** | GeckoLib | Supports complex, fluid, and highly detailed crow-like behaviors. |
| **Data Storage** | SynchedEntityData | MC 26.2 auto-persists all EntityData accessors via `ValueInput`/`ValueOutput` (replaced legacy NBT I/O). |
| **Dependencies** | GeckoLib (via `implementation`) | Integrated as a prerequisite dependency. |
| **Assets** | Minimal structure/Placeholder | Implement minimal structure (lang files, dummy textures) in Phase 1. |
| **Registration** | Minecraft 26.2 Modern API | Align with "Tiny Takeover" standards and future-proof the mod. |

## 3. Roadmap to v1.0

### Phase 1: Dependency & Environment Setup
*   ~~Update `build.gradle` with GeckoLib dependencies (`modImplementation`).~~
*   ~~Verify build with `./gradlew build`.~~
*   ~~Finalize mixin configurations (e.g., `crowbuddy.client.mixins.json`).~~
*   ~~Create minimal asset directory structure (`geckolib/models`, `geckolib/animations`) with files.~~

### Phase 2: Foundation - Registration & Data Generation
*   ~~Implement centralized `ModEntities` (common) with GeckoLib support.~~
*   ~~Implement `CrowEntity` class with GeckoLib `GeoAnimatable` support.~~
*   ~~Implement `CrowGeoModel` from reference template at `Downloads/crow-model/geckolib5/CrowGeoModel.java` (package → `com.crowbuddy`).~~
*   ~~Implement `CrowRenderer` (extends `GeoEntityRenderer<CrowEntity, LivingEntityRenderState>`).~~
*   ~~Implement centralized `ModClientEntities` (client) for renderer registration via reflection.~~
*   ~~Implement centralized `ModItems` with `BLACK_OIL_SUNFLOWER_SEEDS`.~~
*   ~~Register entity and item in `CrowBuddy.onInitialize()` using MC 26.2 `Registry.register()` API.~~
*   ~~Update `fabric.mod.json` and `build.gradle` for source sets and entrypoints.~~
*   ~~Configure DataGen pipeline via `fabricApi { configureDataGeneration() }` (Fabric Maven required for nested jar resolution).~~

### Phase 3: Networking & Core Mechanics
**Implemented:**
*   ~~EntityData expansion — `PERCHED`, `STATE`, `CARRIED_ITEM`, `SATIATION`, `RELATIONSHIP` via `SynchedEntityData`. Migrated `SITTING` to inherited `isOrderedToSit()`/`isInSittingPose()` from `TamableAnimal`.~~
*   ~~`ModNetworking` layer — `DistressPayload` (S→C: entity ID + BlockPos + source ID), `ScavengePayload` (S→C: crow ID + carried item). Registered via `PayloadTypeRegistry.clientboundPlay()`, sent via `ServerPlayNetworking.send()`. Client receivers registered via `ClientPlayNetworking.registerGlobalReceiver()`.~~
*   ~~Event Callbacks — `ServerLivingEntityEvents.AFTER_DAMAGE` + `AttackEntityCallback.EVENT` registered in `CrowEventHub`. 4 handler stubs log DEBUG; deferred wiring to `SwarmManager` (subsection 6).~~
*   ~~Sit/Perch behavior — Shift+right-click toggles sit (`setOrderedToSit()`); normal right-click toggles perch (`PERCHED` boolean). Sit overrides perch. Tame required for perch. Follow logic in `tick()`.~~
*   ~~`isFood()` — Checks `COCOA_BEANS` → false, `parrot_poisonous_food` tag → false, `BLACK_OIL_SUNFLOWER_SEEDS` → true, `parrot_food` tag → true.~~
*   ~~`CrowEntity extends TamableAnimal` — Provides `isTame()`, `tame()`, `setOwner()`, `getOwner()`, `isOwnedBy()`, `wantsToAttack()`, `tryToTeleportToOwner()`, `canAttack()`.~~
*   ~~Dead mixin cleanup — Deleted `CrowBuddyMixin.java`, emptied `crowbuddy.mixins.json` (`"required": false`).~~
*   ~~Scavenging logic — `ScavengeGoal` at priority 3. Proximity acquisition (1.0 block, `distanceToSqr`). Weighted priority: `beacon_payment_items` > `piglin_loved` > `trim_materials` > fallback nearest. Satiation-driven cooldown: ≥0.8 → 60s, ≥0.5 → 240s, <0.5 → 480s. On pick-up: set `CARRIED_ITEM`, state → `CARRYING`, play `SoundEvents.ITEM_PICKUP`, broadcast `ScavengePayload`. `ScavengeGoal.dropCarriedItem()` spawns `ItemEntity` at crow position and resets state to `IDLE`. Wired into `registerGoals()` alongside `FloatGoal`(0), `TemptGoal`(1), `FollowOwnerGoal`(2), `RandomStrollGoal`(4), `LookAtPlayerGoal`(5), `RandomLookAroundGoal`(6).~~
*   ~~Placeholder distress sound — `ModSounds` registers `SoundEvent.createVariableRangeEvent("entity.crow.distress")` in `BuiltInRegistries.SOUND_EVENT`. `sounds.json` maps to placeholder WAV. Final audio asset deferred to Phase 5.~~

**Pending:**
*   **Implement Swarm Intelligence** — `SwarmManager` dispatcher + `SwarmDistressGoal` (priority 0 on `goalSelector`). Wire all 4 `CrowEventHub` stubs.
    *   **Triggers:** Untamed attacked → immediate swarm (source + 5 nearest, 32-block radius, `distanceSquared ≤ 1024`). Tamed attacked → 1st hit: full retaliation (~2s `wantsToAttack()` engagement). Tamed 3 hits in 30s → escalation to full swarm. Owner attacks hostile mob → swarm on that mob.
    *   **Targeting:** Hostile mobs → indefinite. Players → 4s sliding window (resets on hit landed).
    *   **Audio:** All 6 participating crows play distress with dual-layer variance (code + JSON pitch/volume randomization), every 20 ticks.
    *   **Cooldown:** 15s per-crow participation cooldown. Single emission (no relay). Same-dimension only.
    *   **Cancel:** Sit command cancels all swarm behavior. `dropCarriedItem()` on damage.
    *   **Navigation:** `PathNavigation` ground pathfinding (Phase 3). `FlyingPathNavigation` + flight physics deferred to Phase 5.

**Deferred to Phase 5:** Perched shoulder-positioning render (GeckoLib bone snap), client payload visual/audio reactions, flight physics (`FlyingPathNavigation`), GeckoLib dual additive wing animation, distress sound asset variants.

### Phase 4: World & Environment
*   Implement procedural spawning via `BiomeModifications`.
*   Implement Crow Nests as procedural Features with Block Tag compatibility.

### Phase 5: Polishing & Verification
*   Finalize assets (models, textures, sounds).
*   Conduct full build and typecheck.
*   Verify registry logs and initialization.
*   Final testing against PAWS standards.
