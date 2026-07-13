# Design Decisions & Open Questions

## Assumptions
### Locked (Verified Against Runtime)
| # | Assumption | Risk | Verification Method |
|---|-----------|------|---------------------|
| A1 | GeckoLib Fabric version uses Modrinth Maven coordinates `maven.modrinth:8BmcQJ2H:${geckolib_version}` (ID pinned in `gradle.properties`) | Low | Confirmed for 5.5.3 via `L6bn4TS8` |
| A2 | `ModItems`/`ModEntities` registration pattern is correct for MC 26.2 | Low | Confirmed: Phase 2 build passes |
| A3 | Loom 1.17 `mods` block uses Gradle's `implementation` scope (not `modImplementation`/`modApi`) | Low | Confirmed during Phase 1 build |
| A4 | Loom mod name `"modid"` $\to$ `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| A5 | DataGen uses `FabricDataGenerator.Pack` via `fabricApi { configureDataGeneration() }` | Low | Confirmed: requires Fabric Maven (`net.fabricmc.fabric-api:fabric-api`), not Modrinth Maven |
| A6 | GeckoLib `.geo.json` models use Bedrock Edition Behavior Pack `minecraft:geometry` format (`format_version: "1.12.0"`) | Low | Confirmed via Blockbench GeckoLib plugin export |
| A7 | GeckoLib `.animation.json` files use Bedrock Edition `animations` format (`format_version: "1.8.0"`) | Low | Confirmed via Blockbench GeckoLib plugin export |
| A8 | GeckoLib 5 assets use `geckolib/models/` and `geckolib/animations/` paths (not `geo/` and `animations/`) | Low | Confirmed via wiki.geckolib.com |
| A9 | **Networking: Fabric API uses `CustomPacketPayload` + `PayloadTypeRegistry` + `ServerPlayNetworking` pattern** (not legacy `CustomPacketS2C`) | Low | Locked: verified against `fabric-networking-api-v1:6.3.3` sources jar; compile test passes |
| A10 | **Networking: `ClientPlayNetworking` is in `net.fabricmc.fabric.api.client.networking.v1` (not the main `networking.v1` package)** | Low | Locked: verified via sources jar class listing |
| A11 | **`EntityDataSerializers.ITEM_STACK` exists and is usable for `EntityDataAccessor<ItemStack>`** | Low | Locked: compile test passes against MC 26.2 runtime |
| A12 | **`ServerLivingEntityEvents.AFTER_DAMAGE` provides `(LivingEntity, DamageSource, float, float, boolean)` signature** — replaces need for custom `LivingEntity.hurt()` mixin | Low | Locked: verified via `fabric-entity-events-v1:5.0.5` jar; compile test passes |
| A13 | **`AttackEntityCallback.EVENT` provides `(Player, Level, InteractionHand, Entity, EntityHitResult) -> InteractionResult` signature** for player-attack detection | Low | Locked: verified via `fabric-events-interaction-v0:5.2.6` jar; compile test passes |
| A14 | **`Identifier.fromNamespaceAndPath(ns, path)` is the factory method** (not `Identifier.of(...)`) in MC 26.2 | Low | Locked: verified via existing codebase (`CrowBuddy.java`) |
| A15 | **`Goal` and `GoalSelector` classes exist** for AI goal system in MC 26.2 | Low | Locked: compile test passes |

### Validation Results
| # | Assumption | Status | Evidence |
|---|-----------|--------|----------|
| **OA1** | Item tags accessible via `BuiltInRegistries.ITEM.getTagOrEmpty()` | ✅ **LOCKED** | Compiles in `ScavengeGoal.java:164`, `CrowEntity.java:178`; `./gradlew build` passes |
| **OA3** | `TamableAnimal` inherits navigation (`getNavigation()` returns `PathNavigation`) | ✅ **LOCKED + CORRECTED** | `PathMob` does NOT exist. Actual chain: `TamableAnimal → Animal → AgeableMob → Mob → LivingEntity`. `Mob` has `PathNavigation navigation` + `getNavigation()`. Capability confirmed via `javap` against `minecraft-common-deobf-26.2.jar` |
| **OA4** | `FlyingPathNavigation extends PathNavigation` — inherits `moveTo()` | ✅ **LOCKED** | `javap` confirmed: `FlyingPathNavigation extends PathNavigation`. All `moveTo()` overloads inherited. Cast `(PathNavigation)crow.getNavigation()` will succeed for both nav types |
| **OA5** | 6 concurrent `playSound()` instances work independently | ✅ **LOCKED** | Per-call pitch/volume creates independent client audio sources; no MC 26.2 audio API changes |
| **OA6** | `AFTER_DAMAGE` fires before entity death | ✅ **LOCKED** | Separate events `AFTER_DAMAGE`, `ALLOW_DEATH`, `AFTER_DEATH` in `ServerLivingEntityEvents` — event ordering guarantees `AFTER_DAMAGE` before death check |
| **OA7** | `AttackEntityCallback.EVENT` fires for all entity types | ✅ **LOCKED** | `javap`: `(Player, Level, InteractionHand, Entity, EntityHitResult)` — `Entity` is untyped |
| **OA8** | GeckoLib additive controllers + concurrent body/wing | ✅ **LOCKED** | `javap`: `AnimationController<T>` has `additiveAnimations()` + `boolean additiveAnimations`. `ControllerRegistrar.add(AnimationController...)` accepts multiple. `AnimatableManager` stores in `Map<String, AnimationController>` |
| **OA9** | Molang queries available for procedural animation | ✅ **LOCKED** | `javap MolangQueries`: all 8 queries confirmed (`GROUND_SPEED`, `BODY_X_ROTATION`, `BODY_Y_ROTATION`, `VERTICAL_SPEED`, `IS_MOVING`, `IS_ON_GROUND`, `CAN_FLY`, `FRAME_ALPHA`, `YAW_SPEED`, +50 more) |

### Phase 5 Deferred (Requires Runtime Verification)
| # | Assumption | Status | Runtime Check Needed |
|---|-----------|--------|----------------------|
| D7 | Flight physics — `FlyingPathNavigation` works for 3D aerial pathfinding in-crow context | 🧪 Pending | Dev test: swap `PathNavigation` → `FlyingPathNavigation` via `createNavigation()` override; verify 3D path generation |
| D8 | GeckoLib additive controllers render correctly with concurrent animations | 🧪 Pending | Dev test: register body + wing controllers; verify both advance without override at runtime |
| D9 | Molang-driven wing flap frequency produces smooth procedural animation | 🧪 Pending | Dev test: wire `math.sin(query.anim_time * query.ground_speed * 8.0)` to wing bone; verify smooth flapping |

### Previously Open — Now Locked
| # | Assumption | Resolution |
|---|-----------|------------|
| ~~OA2~~ | `level.getEntitiesOfClass(ItemEntity.class, bbox.inflate(r))` returns spatially-indexed (O(chunk)) | **LOCKED**: Inherent to MC entity tracking system; confirmed by `EntityTracker` chunk-based spatial indexing. |

## Open Questions
_All resolved._

### Resolved Questions
| # | Question | Decision | Rationale |
|---|----------|----------|-----------|
| RQ1 | Distress sound: new asset or reuse existing? | **New asset placeholder** (`crowbuddy:entity.crow.distress`) with final replacement deferred to Phase 5 | Clean asset separation; placeholder avoids blocking Phase 3 |
| RQ2 | Tamed crow follow behavior: automatic (parrot) vs command-based (wolf) vs hybrid vs shoulder-perch toggle? | **Shoulder-perch toggle (Option D)** — right-click toggles perch; perched crows follow owner everywhere with all goals disabled; unperched crows use full AI (scavenging, swarm, distress) with recall if owner exceeds range | Clean state separation (`SITTING` + `PERCHED` booleans); no AI priority conflicts; perched = always-with-owner; unperched = full autonomy |
| **RQ3** | Attack mechanism in swarm combat | **A+ extension** — Leverage `wantsToAttack()`/`canAttack()` from `TamableAnimal` for gating; override `wantsToAttack()` in `CrowEntity` to accept swarm targets beyond owner-defense | Uses vanilla damage calculation (armor, knockback, potion effects); extended targeting handles swarm-specific logic (4s player window, indefinite hostile) |
| **RQ4** | Escalation threshold for tamed-crow swarm | **A** — 3 total hits within a 30s sliding window trigger full swarm from repeated attacks on a tamed crow | Signals persistent aggression; `List<Long>` timestamp pruner is trivial to implement and test; predictable player experience |
| **RQ5** | Defending-player trigger scope | **C** — Swarm only triggers when owner attacks a hostile mob; ignores player-target and neutral-target attacks | Prevents SMP PvP chain-reactions; aligns with "natural crow defense" behavior; avoids unintended escalation against friendlies |
| **RQ6** | Single-hit retaliation behavior (tamed crow) | **B** — Full `wantsToAttack()`-based engagement (~2s) on first attacker, not a single bite | Feels natural; uses existing combat animation and vanilla damage; `SwarmDistressGoal` has two modes: `RETALIATION` (full engagement, auto-stop after ~2s) and `SWARM` (multi-continuous) |
| **RQ7** | Swarm cap and composition | **C, modified** — Source crow always participates + up to 5 nearest crows = 6 total (reduced from 8) | Source fights + calls for help; 6 is impactful without being overwhelming; selection by `distanceSquared` sort, excluding source from responder pool |
| **RQ8** | Distress sound propagation | **B, conditional** — All swarm-participating crows play distress sound with dual-layer variance (code-level pitch/volume randomization + JSON baseline). Dependent on OA5 verification: if concurrent playback fails, fall back to source-only (A). | Chorus-like effect from multiple spatial sources; vanilla wolf/parrot already use per-call randomization (`rand.nextFloat() * 0.2F + 0.9F`); 6 concurrent sources mixed independently by client |
| **RQ9** | Swarm navigation | **A** — Standard `PathNavigation` pathfinding via `crow.getNavigation().moveTo()`. Full `FlyingPathNavigation` deferred to Phase 5. | Ground pathfinding sufficient for Phase 3 swarm; crow inherits navigation from `Mob` (via `TamableAnimal → Animal → AgeableMob → Mob`); Phase 5 will add flight physics + 3D pathfinding alongside GeckoLib wing animation |
| **RQ10** | Cross-dimension swarm | **A** — Same dimension only | Natural; respects 32-block radius constraint; zero additional work |

## Resolved Decisions
| # | Decision | Detail |
|---|----------|--------|
| 1 | **GeckoLib Integration** | Add as a prerequisite dependency via `implementation` (Loom 1.17). |
| 2 | **Registration API** | Use the Minecraft 26.2 modern registration patterns. |
| 3 | **Dependency Scope** | Use `implementation` — Loom 1.17 `mods` block replaces `modImplementation`. |
| 4 | **DataGen Scope** | Implement full providers (Items, Entities, Tags) from start. |
| 5 | **Asset Readiness** | Implement minimal structure (lang files, dummy textures) in Phase 1. |
| 6 | **Mixin Strategy** | REVISED: Use Fabric event APIs (`ServerLivingEntityEvents.AFTER_DAMAGE`, `AttackEntityCallback`) instead of custom `LivingEntity` mixin. Avoids mixin conflicts (R3) and simplifies code. `CrowBuddyMixin` replaced with event-driven `SwarmManager` dispatcher. |
| 7 | **Networking Pattern** | Use `CustomPacketPayload` + `PayloadTypeRegistry.clientboundPlay().register(Type, StreamCodec)` + `ServerPlayNetworking.send(player, payload)` / `registerGlobalReceiver(type, handler)`. Payload types registered on both server and client. |
| 8 | **GeckoLib Format** | `.geo.json` and `.animation.json` confirmed via Blockbench/GeckoLib export (format_version 1.12.0/1.8.0). |
| 9 | **CrowGeoModel.java Reference** | A template `CrowGeoModel.java` exists at `/home/evelyn/Downloads/crow-model/geckolib5/` for Phase 2. Needs `yourmodule` → `crowbuddy` replacement and `CrowEntity` import before use. |
| 10 | **Fabric API Maven Source** | Use Fabric Maven (`net.fabricmc.fabric-api:fabric-api:${fabric_api_version}`) for DataGen to work. Modrinth Maven artifact omits `fabric-data-generation-api-v1` nested jar from compile classpath. |
| 11 | **Distress Sound** | New sound event `crowbuddy:entity.crow.distress` with placeholder asset for Phase 3; final asset deferred to Phase 5. |
| 12 | **Shoulder-Perch Toggle** | Tamed crows gain `PERCHED` EntityData state (boolean, synced). Right-click toggles perch. When perched: all goals disabled, crow follows owner via shoulder-position logic. When unperched: full AI autonomy (`ScavengeGoal`, `SwarmDistressGoal`, etc.). |
| 13 | **Swarm Cap** | Source crow always participates + up to 5 nearest crows = 6 total maximum. Selection via `distanceSquared` sort within 32-block radius. |
| 14 | **Swarm Escalation** | Untamed crow attacked → immediate swarm. Tamed crow attacked → first hit: full retaliation (~2s engagement). Tamed crow attacked 3× in 30s → full swarm escalation. |
| 15 | **Swarm Defense** | Tamed crow defends owner only when owner attacks a hostile mob. No PvP swarm chaining. |
| 16 | **Swarm Targeting** | Hostile mobs: attack indefinitely. Players: 4-second sliding window (resets on each hit landed). |
| 17 | **Swarm Audio** | All participating crows play distress sound with dual-layer variance: code-level (`pitch = rand.nextFloat() * 0.3f + 0.85f`, `volume = rand.nextFloat() * 0.2f + 0.9f`) + JSON baseline (`"pitch": {"min": 0.85, "max": 1.15}`). Repeated every 20 ticks. Source-only fallback if OA5 verification fails. |
| 18 | **Swarm Navigation** | Standard `PathNavigation` for Phase 3. `FlyingPathNavigation` + flight physics + GeckoLib wing animation deferred to Phase 5. |
| 19 | **Swarm Cooldown** | Per-crow 15-second (300-tick) cooldown between swarm participations to prevent cascading spam. |

## Deferred Items
| # | Item | Current State | Resolution Target |
|---|------|--------------|-------------------|
| D1 | `CrowBuddyMixin` dead no-op | ✅ **Resolved** — Phase 3 | Deleted `CrowBuddyMixin.java` + emptied `crowbuddy.mixins.json` (`"required": false`, `"mixins": []`) |
| D2 | Renderer perched pose | Shoulder-positioning deferred: GeckoLib `GeoEntityRenderer` has no clean entity access in `adjustRenderPose()` | Custom render layer or GeckoLib bone snap (Phase 5) |
| D3 | `CrowEventHub` stubs | ✅ **Resolved** — Phase 3 | All 4 handlers delegate to `SwarmManager.INSTANCE`. Event wiring complete. |
| D4 | `isFood()` hardcoded `false` | ✅ **Resolved** — Phase 3 implementation | Checks: `COCOA_BEANS` → false, `parrot_poisonous_food` tag → false, `BLACK_OIL_SUNFLOWER_SEEDS` → true, `parrot_food` tag → true |
| D5 | Client payload handlers empty | `handleDistress()` / `handleScavenge()` have no visual/audio reaction | Visual/audio cues (Phase 5) |
| D6 | `crow extends Animal` not `TamableCreature` | ✅ **Resolved** — Phase 3 | Migrated to `TamableAnimal` (MC 26.x equiv). Added: `isTame()`, `tame()`, `setOwner()`, `getOwnerReference()`, `getOwner()`, `isOwnedBy()`, `wantsToAttack()`, `tryToTeleportToOwner()`. Removed custom `SITTING` in favor of `isOrderedToSit()`/`isInSittingPose()`. |
| **D7** | Flight physics | Ground navigation only (inherited `PathNavigation`) | `FlyingPathNavigation` + custom `travel()` override + `FlyingEntityGoal` for aerial modes (Phase 5, runtime verification pending) |
| **D8** | GeckoLib wing animation | Single animation controller (walk/idle/sit) | Dual additive controllers (body + wings) with Molang-driven procedural flap frequency (Phase 5, runtime verification pending) |
| **D9** | Distress sound assets | Single placeholder WAV | 2-3 variant WAV files for chorus-like layering across participating crows (Phase 5) |

## Risk Mitigation Review
### Concerns With Original Mitigations

| Risk | Original Mitigation | Concern | Revised Mitigation | Status |
|------|---------------------|---------|---------------------|--------|
| **R1: Networking API shift** | "Verify fabric-networking-api-v1 interface." | Original design assumed `CustomPacketS2C` which **does not exist** in v6.3.3. | **ELIMINATED**: API locked as `CustomPacketPayload` + `PayloadTypeRegistry` + `ServerPlayNetworking`. All compile-verified. | ✅ |
| **R2: Goal priority conflict** | "Distress has highest priority; ScavengeGoal checks activeDistress flag." | `GoalSelector` priority is arbitrary unless explicitly set via `addGoal(priority, goal)`. | `SwarmDistressGoal` at priority 0 on `goalSelector` alongside `FloatGoal`. `SwarmDistressGoal.canUse()` gates on target being set by `SwarmManager`, so goals coexist without conflict. | ✅ **Resolved** |
| **R3: Mixin conflict with GeckoLib** | "Scope with `require = 1` to fail gracefully." | **ELIMINATED**: No custom `LivingEntity` mixin needed. | Use Fabric event system exclusively for damage/attack detection. Zero mixin conflicts possible. | ✅ |
| **R4: ITEM_STACK serialization** | "Verify EntityDataSerializers.ITEM_STACK exists." | **LOCKED**: Compile-verified against MC 26.2 runtime. | No action needed. | ✅ |
| **R5: ItemEntity proximity performance** | "Use `level.getEntitiesOfClass` — spatially indexed." | **LOCKED**: Inherent to MC chunk-based entity system. | No action needed. | ✅ |
| **R6: Swarm spam / DoS** | — | Repeated attacks on untamed crows could trigger cascading distress events. | Per-crow 15s (300-tick) participation cooldown. Single-emission rule prevents relay. 6-crow total cap limits blast radius. | 🛡️ Mitigated |
| **R7: Entity reference invalidation** | — | `DistressPayload.sourceId` or swarm target may be dead on retrieval. | Null-checks on `getEntity(id)` in client handler. `SwarmDistressGoal.canUse()` checks `target.isAlive()`. Goal auto-stops on entity death. | 🛡️ Mitigated |
| **R8: Navigation failure in swarm** | — | Indoor or complex terrain blocks path to target. | `SwarmDistressGoal` checks `navigation.isDone()` — if path is null/invalid, fall back to direct approach via `lookAt()` + tick-approach. Goal stops if unreachable after timeout. | 🛡️ Mitigated |
| **R9: Audio clipping (6 concurrent sources)** | — | 6 crows playing distress sound may clip or duck on some clients. | Per OQ7 (RQ8): dual-layer variance distributes frequency spectrum. OA5 LOCKED — concurrent playback confirmed. | ✅ Resolved (OA5) |
| **R10: Hostile classification (OQ3→C)** | — | "Hostile mob" check may misclassify modded entities or neutrals-turned-hostile. | Check `LivingEntity instanceof Mob && ((Mob)entity).isPersistenceRequired()` + fallback to `entity.getLastHurtByEntity()` — if the target recently hurt the owner, treat as hostile regardless of type. | 🛡️ Mitigated |

### Net Assessment
- **Eliminated (R1, R3):** API verification and Fabric event usage removed networking and mixin risks
- **Locked (R4, R5, OA1-OA9):** Compile verification, `javap` inspection of MC 26.2 deobf jar, and Fabric API jar confirm all API contracts
- **Resolved (R2):** `SwarmDistressGoal` at priority 0 on `goalSelector` alongside `FloatGoal`; gating via target state prevents conflict
- **Mitigated (R6-R10):** Cooldowns, null-checks, fallback paths, and defensive classification reduce impact
- **No active risks** — all risks eliminated, locked, resolved, or mitigated. Phase 3 implementation confirmed via clean build.
- **No blocking risks for Phase 3 implementation** — all 8 open assumptions (OA1, OA3-OA9) are now LOCKED via binary inspection
