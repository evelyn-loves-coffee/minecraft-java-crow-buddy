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

### Open (Need Runtime Verification)
| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| OA1 | Item tags `minecraft:beacon_payment_items`, `minecraft:piglin_loved`, `minecraft:trim_materials` are accessible via `FabricTagRegistry` / `RegistryTags` in MC 26.2 | Low | Verify via DataGen tag access or runtime check |
| OA2 | `level.getEntitiesOfClass(ItemEntity.class, bbox.inflate(r))` returns spatially-indexed results (O(chunk) not O(all entities)) | Low | Confirmed by Minecraft's entity tracking system; inherent to chunk system |

## Open Questions
_All resolved._

### Resolved Questions
| # | Question | Decision | Rationale |
|---|----------|----------|-----------|
| RQ1 | Distress sound: new asset or reuse existing? | **New asset placeholder** (`crowbuddy:entity.crow.distress`) with final replacement deferred to Phase 5 | Clean asset separation; placeholder avoids blocking Phase 3 |
| RQ2 | Tamed crow follow behavior: automatic (parrot) vs command-based (wolf) vs hybrid vs shoulder-perch toggle? | **Shoulder-perch toggle (Option D)** — right-click toggles perch; perched crows follow owner everywhere with all goals disabled; unperched crows use full AI (scavenging, swarm, distress) with recall if owner exceeds range | Clean state separation (`SITTING` + `PERCHED` booleans); no AI priority conflicts; perched = always-with-owner; unperched = full autonomy |

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

## Deferred Items (Phase 3 Implementation)
| # | Item | Current State | Resolution Target |
|---|------|--------------|-------------------|
| D1 | `CrowBuddyMixin` dead no-op | ✅ **Resolved** — Phase 3 | Deleted `CrowBuddyMixin.java` + emptied `crowbuddy.mixins.json` (`"required": false`, `"mixins": []`) |
| D2 | Renderer perched pose | Shoulder-positioning deferred: GeckoLib `GeoEntityRenderer` has no clean entity access in `adjustRenderPose()` | Custom render layer or GeckoLib bone snap (Phase 5) |
| D3 | `CrowEventHub` stubs | 4 methods logging DEBUG; delegation to `SwarmManager` not wired | Wire in subsection 6 |
| D4 | `isFood()` hardcoded `false` | ✅ **Resolved** — Phase 3 implementation | Checks: `COCOA_BEANS` → false, `parrot_poisonous_food` tag → false, `BLACK_OIL_SUNFLOWER_SEEDS` → true, `parrot_food` tag → true |
| D5 | Client payload handlers empty | `handleDistress()` / `handleScavenge()` have no visual/audio reaction | Visual/audio cues (Phase 5) |
| D6 | `crow extends Animal` not `TamableCreature` | ✅ **Resolved** — Phase 3 | Migrated to `TamableAnimal` (MC 26.x equiv). Added: `isTame()`, `tame()`, `setOwner()`, `getOwnerReference()`, `getOwner()`, `isOwnedBy()`, `wantsToAttack()`, `tryToTeleportToOwner()`. Removed custom `SITTING` in favor of `isOrderedToSit()`/`isInSittingPose()`. |

## Risk Mitigation Review
### Concerns With Original Mitigations

| Risk | Original Mitigation | Concern | Revised Mitigation |
|------|---------------------|---------|---------------------|
| **R1: Networking API shift** | "Verify fabric-networking-api-v1 interface." | Original design assumed `CustomPacketS2C` which **does not exist** in v6.3.3. The mitigation was directionally correct but incomplete. | **ELIMINATED**: API locked as `CustomPacketPayload` + `PayloadTypeRegistry` + `ServerPlayNetworking`. All compile-verified. |
| **R2: Goal priority conflict** | "Distress has highest priority; ScavengeGoal checks activeDistress flag." | `GoalSelector` priority is arbitrary unless explicitly set via `addGoal(priority, goal)`. Need explicit ordering. | `SwarmDistressGoal` at priority 0; `ScavengeGoal` at priority 3. `SwarmDistressGoal` sets volatile `isDistressed` flag checked by all goals. |
| **R3: Mixin conflict with GeckoLib** | "Scope with `require = 1` to fail gracefully." | **ELIMINATED**: No custom `LivingEntity` mixin needed. Fabric provides `ServerLivingEntityEvents.AFTER_DAMAGE` and `AttackEntityCallback` — both verified. | Use Fabric event system exclusively for damage/attack detection. Zero mixin conflicts possible. |
| **R4: ITEM_STACK serialization** | "Verify EntityDataSerializers.ITEM_STACK exists." | **LOCKED**: Compile-verified against MC 26.2 runtime. | No action needed. |
| **R5: ItemEntity proximity performance** | "Use `level.getEntitiesOfClass` — spatially indexed." | **LOCKED**: Inherent to MC chunk-based entity system. O(chunk) complexity. | No action needed. |

### Net Assessment
- Risks R1, R3 eliminated through API verification and Fabric event usage
- Risks R2, R4, R5 addressed with explicit ordering, compile verification, and chunk-system guarantees
- **No remaining blocking risks for Phase 3 implementation**
