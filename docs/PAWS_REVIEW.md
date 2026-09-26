# PAWS Review — Crow Buddy (mod 1.0.3, MC 26.2, JDK 25)

Review type: **static + empirical** (deobfuscated Minecraft 26.2 bytecode, JDK 25 reflection/VarHandle/Unsafe tests, Fabric API 0.155.2+26.2 inspection).
Build: `./gradlew build` → **BUILD SUCCESSFUL**. Tests: **44/44 pass**.

## Fixes applied (2026-09-26)

All review items were fixed and re-verified with `./gradlew build` (**BUILD SUCCESSFUL**, **44/44 tests pass**, 2 new regression tests added).

| # | Review item | Fix | Files |
| --- | --- | --- | --- |
| 1 | **Critical:** reflection attribute registration silently fails | Reflection block deleted; `FabricDefaultAttributeRegistry.register(CROW, CrowEntity.createAttributes().build())` (supported Fabric API) + fail-fast: `DefaultAttributes.hasSupplier(CROW)` check throws `IllegalStateException` at mod init instead of hiding the failure | `registry/ModEntities.java` |
| 2 | Distress audio tripled; `targetPos` dead wire; `entityId`/`sourceId` redundant | Client replay removed — sound is now server-side only (positional, activation + 20-tick goal loop); client renders ANGRY_VILLAGER + CRIT particles on the crow only; payload collapsed to a single `sourceId` field; `sendDistress(player, sourceId)` | `client/networking/ModClientNetworking.java`, `networking/DistressPayload.java`, `networking/ModNetworking.java`, `swarm/SwarmManager.java` |
| 3 | SwarmManager leak on crow death (plus same-class claim leak in `ScavengeRegistry`) | `ServerLivingEntityEvents.AFTER_DEATH` handler: `clearCrowState(crow)` + `ScavengeRegistry.releaseAll(crow)` — dead crows no longer block item claims or leak map entries | `event/CrowEventHub.java` |
| 4 | Nest `HATCHING + ticksRemaining==0` state trap | State machine `tick()` guard: an active stage with `ticksRemaining <= 0` completes the stage instead of stalling; block entity no longer skips `tick()` on `ticksRemaining == 0`; +2 regression tests | `block/entity/CrowNestStateMachine.java`, `block/entity/CrowNestBlockEntity.java`, `CrowNestStateMachineEdgeCasesTest.java` |
| 5 | Scavenge pickup double-sound | Client-side `ITEM_PICKUP` replay removed (server positional sound remains) | `client/networking/ModClientNetworking.java` |
| 6 | Dead `SwarmManager.checkCooldown` / `checkRetaliation` | Removed | `swarm/SwarmManager.java` |
| 7 | Dead `Mode.RETALIATION` — LLD Phase 3 "single hit ⇒ ~2 s retaliation" was designed but never wired | **Wired:** `triggerRetaliation` activates the goal in `Mode.RETALIATION`, capping the engagement at the 40-tick retaliation window (2 s) instead of the 200-tick swarm window; goal `mode` is now per-engagement via `setMode` | `swarm/SwarmManager.java`, `goal/SwarmDistressGoal.java` |
| 8 | Dead `AStarPathfinder.closest` state | Removed | `entity/ai/navigation/AStarPathfinder.java` |
| 9 | Dead `FlightNavigator.isPathValid` / `getMaxSearchNodes` | Removed from interface and implementation | `entity/ai/navigation/FlightNavigator.java`, `AStarPathfinder.java` |
| 10 | Dead `CrowEventHub.log` | Removed | `event/CrowEventHub.java` |
| 11 | Biome docs vs code (TEST_PLAN "non-desert"; LLD_PHASE_4 missing badlands, wrong weight) | Both now say "overworld biomes except oceans, rivers, badlands"; LLD weight corrected 1 → 5 (matches code) | `docs/TEST_PLAN.md`, `docs/LLD_PHASE_4.md` |
| 12 | "Turtle-egg probabilities" wording | → "Nest-trampling probabilities (1/100 on step, 1/3 on fall)" | `docs/TEST_PLAN.md` |
| 13 | Vanilla recipe overrides under-documented | `LLD_PHASE_2` now enumerates the three overridden recipes (arrow, brush, writable book), the tag contents (`minecraft:feather` + `crowbuddy:black_feather`), and the vanilla-player effect | `docs/LLD_PHASE_2.md` |
| 14 | Stale test count (38) | → 44, verified 2026-09-26 | `docs/TEST_PLAN.md` |
| 15 | LLD Phase 3 payload description | Updated: single source ID; sound server-side, client renders particles only | `docs/LLD_PHASE_3.md` |

**Deliberately not changed:**
- **A\* worst-case cost** — bounded by design (`DEFAULT_MAX_SEARCH_NODES = 2,000`); the `2,000 × (T + 130)` per-failure cost is inherent to the algorithm and acceptable per the review; revisit only if in-game hitches are observed.
- **Scavenge broadcast to all dimension players** — acceptable (scavenge is local/rare); noted for a future nearby-only optimization.
- **CRIT particles on the crow** — intentional (the crow is the entity that is angry); kept as the single particle target.
- **No entity-construction smoke test** — the test harness is plain JUnit without a Loom/Minecraft runtime, so `CrowEntity` cannot be constructed in tests; the fail-fast init assertion (fix #1) is the guard instead.

---

## Executive summary

| Severity | Item | Category |
| --- | --- | --- |
| **Critical / showstopper** | `ModEntities.registerAttributes()` silently fails on JDK 25 → all crow spawns NPE in `LivingEntity` constructor | P3 Workability |
| High | Distress audio tripled (server trigger + server 20-tick repeat + client replay); `targetPos` dead wire; `entityId`/`sourceId` redundant | P3 Workability |
| Medium | Dead-state / dead-code across navigation + swarm subsystems | P4 Scalability |
| Medium | Vanilla recipes (arrow, brush, writable book) globally overridden to require `#crowbuddy:feathers`; docs under-documented | P2 Auditability |
| Low | SwarmManager leak on crow death; nest `HATCHING + ticksRemaining==0` state trap; A* worst-case cost; scavenge pickup double-sound | P3 / P1 |

The **single blocking defect** is attribute registration. Until it is fixed, the mod is not playable — crows cannot spawn, breed, or hatch. Everything below is secondary.

---

## Critical — P3 Workability: attribute registration is broken on the target toolchain

**File:** `src/main/java/com/crowbuddy/registry/ModEntities.java` — `registerAttributes()`
**Root cause:** reflection on `net.minecraft.world.entity.ai.attributes.DefaultAttributes#SUPPLIERS`.

Verified in the deobfuscated `minecraft-merged-deobf-26.2.jar`:
- `SUPPLIERS` is `private static final`, a Guava `ImmutableMap`, built entirely in a `static {}` initializer and **not** reassigned by `DefaultAttributes.validate()`.
- `DefaultAttributes` exposes **no** public registration method (only `getSupplier`, `hasSupplier`, `validate`) — vanilla leaves no sanctioned injection point, which is why the mod reached for reflection (a supported Fabric-API hook does exist, see below).

Verified empirically on JDK 25.0.4.1 (standalone tests; second re-review re-ran them, crash artifacts in `/tmp/gold/`):
- `Field.set` on the `static final` field → `IllegalAccessException: Can not set static final ... field ... SUPPLIERS`.
- Legacy `modifiers`-field hack → `NoSuchFieldException: modifiers` (no such field).
- `VarHandle.set` on the `static final` field → `UnsupportedOperationException: set`.
- `sun.misc.Unsafe.putObject(null, staticFieldOffset, value)` → **fatal JVM crash (SIGSEGV)**. The JDK 25 implementation treats the static-field offset as an *instance* write (null base + offset) — **Unsafe is not a viable fix**.

**A supported Fabric hook DOES exist (first review was wrong on this point).** `fabric-api 0.155.2+26.2` bundles `fabric-object-builder-api-v1 24.1.0`, which exposes `FabricDefaultAttributeRegistry.register(EntityType<? extends LivingEntity>, AttributeSupplier.Builder)` (plus an `AttributeSupplier` overload and a `MODIFY` event). Its backing Mixin (`DefaultAttributesMixin`, in the `required: true` `fabric-object-builder-v1` mixin config, `compatibilityLevel: JAVA_25`) injects at `DefaultAttributes.<clinit>` TAIL and converts `SUPPLIERS` from an `ImmutableMap` into a mutable `IdentityHashMap` (`@Shadow @Final @Mutable` accessor), so `register()` is a plain `Map.put`.

**Crash chain (verified):**
1. `Field.set` throws → exception is caught and logged (not rethrown).
2. `DefaultAttributes.SUPPLIERS` never contains `crowbuddy:crow`.
3. `DefaultAttributes.getSupplier(CROW)` → `null`.
4. `new CrowEntity(...)` → `LivingEntity` constructor: `new AttributeMap(null)` then `setHealth(getMaxHealth())`.
5. `AttributeMap.getValue` falls back to `supplier.getValue` on an empty local map → **NPE**.

**Impact:** every crow spawn path fails — natural spawning (`CrowSpawning`), spawn eggs, and nest hatching. The NPE occurs in the constructor path; if it propagates through a level tick it can crash the server.

**Recommended fix:**
1. Delete the whole `registerAttributes()` reflection block and call the supported Fabric API instead:
   `FabricDefaultAttributeRegistry.register(ModEntities.CROW.get(), CrowEntity.createAttributes().build());`
   (`net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry` — already on the classpath via the `fabric-api` umbrella dependency). Do **not** use `Unsafe.putObject` (JVM crash, see above); a hand-rolled Mixin on `DefaultAttributes` is an acceptable alternative but unnecessary.
2. **Fail fast**: after registration, assert `DefaultAttributes.hasSupplier(CROW.get())`; rethrow/log a fatal error rather than swallowing.
3. Add a smoke test that a freshly constructed `CrowEntity` does not NPE (guarded so it does not require a full level).

---

## P1 Performance

**A* worst-case cost is bounded but heavy.** `AStarPathfinder` budgets `DEFAULT_MAX_SEARCH_NODES = 2,000`, `gridSize = 4` (set in `CrowEntity`), 26 neighbors/iteration. Worst case per failed path ≈ **2,000 × (T + 130)** `isPassable`-ish checks, where **T is the block distance from the expanded node to the target**: each expanded node does `segmentClear(current→target)` (samples **T** points, not 1) + 26 neighbors × (1 direct `isPassable(next)` + 1 `segmentClear(current→next)` = 4 samples) = T + 130 per node. That is ≈ **262k at T≈1 (lower bound)**, ≈ **460k at T=70**, ≈ **516k at T=100**. *(History: "208k" omitted the direct neighbor `isPassable` calls; the "262k" figure additionally assumed T=1 for the to-target segment.)* 

**Scavenge pickup sound is broadcast + replayed.** `ScavengeGoal.collectFromTarget` plays `ITEM_PICKUP` server-side (volume 0.2) **and** `ModClientNetworking.handleScavenge` plays it client-side (volume 0.4, `distanceBased=false`) to every player in the dimension. Doubling of audio; minor bandwidth amplification.

**Scavenge broadcast granularity.** `broadcastScavenge(updated)` sends a full `ItemStack` (with components) to **all** players in the dimension on every transfer (up to 8 per collection). Scavenge is local/rare, so acceptable, but consider `ServerPlayNetworking.sendToNearby`-style targeting.

---

## P2 Auditability

**1. Spawning-biome docs vs code.** `CrowSpawning` excludes `IS_OCEAN`, `IS_RIVER`, `IS_BADLANDS`. `docs/TEST_PLAN.md:9` describes crows spawning in "non-ocean, non-river, **non-desert**, non-underground" biomes (note: the "non-desert" wording is in TEST_PLAN, not LLD_PHASE_1, which does not mention deserts; `LLD_PHASE_4.md:5` says "non-ocean, non-river" and further under-reports the `IS_BADLANDS` exclusion). Verified: there is **no** general `IS_DESERT` tag in the checked `BiomeTags` set (only `HAS_DESERT_*` structure tags) → **crows can spawn in desert biomes**, contrary to the test plan. Docs describe behavior the code does not enforce.

**2. Trampling-probability wording.** `CrowNestBlock.stepOn` = 1/100, `fallOn` = 1/3. `docs/LLD_PHASE_4.md` matches the code. `docs/TEST_PLAN.md` calls these "turtle-egg probabilities" — misleading, since vanilla turtle eggs use `1/20`. Rename to nest-trampling.

**3. Vanilla recipe overrides under-documented.** `data/minecraft/recipe/{arrow,brush,writable_book}.json` **override** vanilla recipes to require `#crowbuddy:feathers`:
- arrow → flint + stick + feathers (vanilla arrows already use feathers; this swaps the feather).
- brush → copper_ingot + stick + feathers (**new** requirement; brushes were feather-free).
- writable_book → book + ink_sac + feathers (**new** requirement; writable books were feather-free).

`docs/LLD_PHASE_2.md` mentions only "compatibility recipes" — it does not list which vanilla recipes are overridden. Because the tag includes `minecraft:feather`, nothing is locked for vanilla players (chicken feathers still work) — the functional effect is that **black feather becomes a drop-in replacement** for regular feather. The real changes: (a) brush and writable book gain a **new feather requirement** not present in vanilla; (b) the three `data/minecraft/recipe/*.json` files **replace vanilla data** — any other mod/datapack modifying the same recipe silently conflicts (last-loaded wins); (c) none of this is enumerated in the docs.

**4. Loot table `random_sequence` is valid in 26.2.** Verified via deobfuscated `LootTable`: the field `randomSequence` (codec key `"random_sequence"`) exists at the top level. The crow loot table is well-formed (no defect). *(Recorded to close a suspected false positive.)*

---

## P3 Workability

**1. Distress audio tripled + payload defect.** Three sources play `CROW_DISTRESS` for one swarm trigger:
- `SwarmManager.activateSwarmMode` → `level.playSound` (once per crow at swarm trigger).
- `SwarmDistressGoal.playDistressSound` → `level.playSound` **every 20 ticks** while the goal is engaged (up to ~10× per 200-tick engagement).
- Client `handleDistress` → `playLocalSound(..., 0.8, pitch, distanceBased=false)` **once per received payload** (full volume to every receiving player).
- `DistressPayload(entityId, targetPos, sourceId)`: sender writes `entityId = crow.getId()`, `targetPos = target.blockPosition()`, `sourceId = crow.getId()`. Client uses `sourceId` for sound/ANGRY_VILLAGER particles and `entityId` for CRIT particles, and **never reads `targetPos`**.
- Effects: (a) layered/distressed distress audio (server activation + server repeat + client replay); (b) full-volume client replay to all dimension players; (c) CRIT particles spawn on the crow (`entityId == sourceId == crow.getId()`), not on the target mob at `targetPos` — this is a design choice, not a clear bug; (d) `targetPos` is serialized but dead wire, and `entityId`/`sourceId` are always identical (redundant field).
- Fix: pick one distress-sound source (drop the client replay, or the server ones); decide whether CRIT should mark the crow or the target; drop `targetPos` (or use it) and collapse `entityId`/`sourceId` to one field.

**2. SwarmManager leak on crow death.** `SwarmManager` maps keyed by crow ID (`cooldowns`, `retaliationTimers`, `escalationHistory`) are cleaned only via `clearCrowState` (neutral-sit path) and level unload (`ServerLevelEvents.UNLOAD`). No cleanup on entity death → dead crow entries persist until dimension unload. Bounded by total crows ever alive in the dimension; low severity. Add an `EntityEvents.ENTITY_UNLOAD`/`AFTER_DEATH` cleanup.

**3. Nest `HATCHING + ticksRemaining == 0` state trap.** If a loaded nest is in `STAGE_HATCHING` with `ticksRemaining == 0`, `CrowNestStateMachine.tick()` does nothing and the nest is stuck. Rare / corruption- or migration-dependent, not the normal runtime path. Add a defensive `tick`/load guard.

**4. Scavenge pickup double-sound** (see P1).

---

## P4 Scalability

**Dead state / dead code (verified, all unused):**
- `AStarPathfinder`: `closest` Node is tracked (lines 49, 55) but never read — the method returns `List.of(target)` on segment-clear or `List.of()` on exhaustion. Dead state; a missed fallback-optimization point.
- `SwarmManager.checkCooldown(int, long)` — defined, never called.
- `SwarmManager.checkRetaliation(int, long)` — defined, never called.
- `FlightNavigator.isPathValid(Level, List<Vec3>)` — interface method (implemented in `AStarPathfinder`), never called by anyone.
- `FlightNavigator.getMaxSearchNodes()` — interface method (implemented in `AStarPathfinder`), never called by anyone.
- `SwarmDistressGoal.Mode.RETALIATION` — logic branches reference `Mode.RETALIATION` (goal lines 82, 103) but **no** `Mode.RETALIATION` goal is ever constructed in `CrowEntity.registerGoals()` (only `Mode.SWARM`). Dead enum usage.
- `SwarmManager.checkRetaliation`/`checkCooldown` dead → the escalation cooldown + retaliation timers they inspect are effectively dormant.
- `CrowEventHub.log(String)` — unused private helper.

**Recommendation:** remove the dead methods/fields or wire them into the escalation/cooldown logic if the intended behavior is "retaliation." If retaliation is intended but unimplemented, that is a latent gap, not just dead code — flag it explicitly.

---

## Verified non-issues / rejected false positives

- **Nest heightmap usage is correct in 26.2.** `Level#getHeight(MOTION_BLOCKING, x, z)` returns the air position above the topmost motion-blocking block (out-of-bounds → seaLevel+1; missing chunk → minY). `CrowNestBuildGoal`'s use of it as `surfacePosition` is valid. No off-by-one.
- **Breeding call path is compatible.** `BreedGoal` calls `Animal.spawnChildFromBreeding`; the mod's override intentionally bypasses vanilla child spawning in favor of nest-based hatching.
- **`Block.UPDATE_CLIENTS` (2) usage is valid** for the nest's cosmetic state sync.
- **Nest retry path works.** Failed hatch retry sets `HATCHING` with `ticksRemaining = 1`; the block-entity tick marks the state dirty and the retry proceeds.
- **`SwarmManager.get(level)` in `CrowEntity.registerGoals()` is safe** — the entity's level is initialized at that point.
- **`random_sequence` in the crow loot table is valid** in 26.2 (see P2.4).
- **`#crowbuddy:feathers` tag resolves** (`minecraft:feather` + `crowbuddy:black_feather`); the overridden recipes are data-valid.
- **LootTableEvents.MODIFY** (v3) and `ServerLivingEntityEvents.AFTER_DAMAGE` / `AttackEntityCallback` wiring compile and match the event signatures.

---

## Re-review validation log (fresh perspective)

Each finding was re-read as an unconfirmed claim and re-checked against actual code, deobfuscated MC 26.2 bytecode, and (for the critical claim) a standalone JDK 25.0.4.1 runtime test. Result: all findings hold, with four corrections/nuances applied in this document.

| Claim | How validated | Verdict |
| --- | --- | --- |
| `Field.set` on `DefaultAttributes.SUPPLIERS` throws on JDK 25 | Standalone test: `static final` `Map` field, same class and other class → `IllegalAccessException` both ways | **Holds** (mechanism confirmed) |
| `SUPPLIERS` is `private static final` Map | `javap` on deobfuscated `DefaultAttributes` | **Holds** |
| `getSupplier` returns null when absent | `javap` bytecode: `SUPPLIERS.get(type)` → `areturn` | **Holds** |
| `LivingEntity.<init>` calls `getMaxHealth()` | `javap` bytecode: 3 `getMaxHealth` calls in `<init>` | **Holds** |
| `AttributeMap.getValue` falls back to `supplier.getValue` | `javap` bytecode: `ifnull → supplier.getValue` → null supplier NPE | **Holds** |
| Distress sound played by server + client | Read `SwarmManager` + `SwarmDistressGoal` + client handler | **Holds** (third source also confirmed) |
| `targetPos` serialized, never read on client | Read `ModNetworking` (writes `targetPos.asLong()`) + client handler (never reads it) | **Holds** |
| `entityId == sourceId == crow.getId()` | Read `SwarmManager.activateSwarmMode` send call | **Holds** |
| `checkCooldown`/`checkRetaliation`/`isPathValid`/`getMaxSearchNodes`/`CrowEventHub.log` have no callers | `grep` across `src/` | **Holds** |
| `Mode.RETALIATION` never constructed | `grep` for `new SwarmDistressGoal` → only `Mode.SWARM` | **Holds** |
| Recipes override vanilla arrow/brush/writable_book | Read all three recipe JSONs | **Holds** |
| No `IS_DESERT` biome tag in 26.2 | `javap` on deobfuscated `BiomeTags` (only `HAS_DESERT_*` present) | **Holds** |
| Nest `STAGE_HATCHING + ticksRemaining==0` never advances | Read `CrowNestStateMachine.tick()` + block-entity load/save | **Holds** (reachable only via corrupt/migrated save) |
| A* worst-case cost | Read `AStarPathfinder` constants + neighbor loop | **Holds** (corrected to ~262k) |
| `random_sequence` valid in 26.2 | `javap` on deobfuscated `LootTable` (field + codec key) | **Holds** |

**Corrections made during re-review:** (1) biome citation changed from `LLD_PHASE_1.md` → `docs/TEST_PLAN.md:9` (which actually says "non-desert"); (2) A* bound corrected from 208k → ~262k; (3) distress "double" upgraded to "triple" (added `SwarmDistressGoal` 20-tick source); (4) "CRIT on wrong entity" reframed as a design choice (CRIT lands on the crow), with the solid facts (double-sound, dead `targetPos`, redundant `entityId`/`sourceId`) retained.

## Second re-review (fresh perspective — all findings treated as unconfirmed again)

Re-verified every finding against source, deobfuscated 26.2 bytecode, and fresh JDK 25.0.4.1 runtime tests. **All findings hold; two major errors in the critical section were found and corrected** (both in the *fix recommendation*, not in the defect itself):

| Claim | How re-validated (second pass) | Verdict |
| --- | --- | --- |
| `Field.set` / `VarHandle.set` on `SUPPLIERS` fail on JDK 25 | Re-ran standalone proxy test (same `private static final Map` shape, cross-class): `IllegalAccessException: Can not set static final java.util.Map field` / `UnsupportedOperationException: set` | **Holds** |
| `Unsafe.putObject(null, staticFieldOffset, value)` "succeeds" (first review) | Re-ran on OpenJDK 25.0.4.1: `staticFieldOffset` works (offset 120), but `putObject` **crashes the JVM — SIGSEGV** (static offset handled as instance write at null+0x78; `hs_err` log captured) | **WRONG in first review — corrected.** Unsafe is not a fix; it kills the server process |
| "Fabric API exposes no default-attribute registration event" (first review) | Inspected the `fabric-api 0.155.2+26.2` dependency tree: `fabric-object-builder-api-v1 24.1.0` ships `FabricDefaultAttributeRegistry.register(...)` + `MODIFY` event; Mixin `DefaultAttributesMixin` (`required: true`, `JAVA_25`) converts `SUPPLIERS` → mutable `IdentityHashMap` at `<clinit>` TAIL; `register()` = `Map.put` via `@Shadow @Mutable` accessor | **WRONG in first review — corrected.** Supported API exists and is the right fix |
| A* ~262k worst case | Re-read `AStarPathfinder.find` + `segmentClear`: to-target segment samples **T** points (T = block distance to target), so per node = T + 130, total = 2,000×(T+130); 262k is the T≈1 lower bound | **Holds, reframed** as `2,000 × (T + 130)` |
| Distress triple-sound / dead `targetPos` / redundant IDs / no death cleanup / nest trap / scavenge double-sound / all dead code / recipes / biome tags / test count | Re-read `SwarmManager`, `SwarmDistressGoal`, `ModClientNetworking`, `DistressPayload`, `CrowNestStateMachine`, `ScavengeGoal`, `CrowSpawning`, `CrowNestBlock`, all three overridden recipe JSONs, `#crowbuddy:feathers` tag, crow loot table; re-grepped every dead-code claim; re-ran `./gradlew test --rerun` (42/42); re-ran `javap` on `BiomeTags` (no general `IS_DESERT`) | **All hold** |

**Corrections made in second re-review:** (1) `Unsafe.putObject` finding corrected from "succeeds" to **fatal JVM crash (SIGSEGV) on JDK 25** — the prior "verified" label had no test artifact behind it; the fresh test produced `hs_err` logs. (2) Fix recommendation replaced: use **`FabricDefaultAttributeRegistry.register(CROW, CrowEntity.createAttributes().build())`** — the supported, Mixin-backed Fabric API already in the dependency tree — instead of Unsafe or a hand-rolled Mixin. (3) A* worst case reframed as `2,000 × (T + 130)` with 262k as the lower bound. The critical defect itself (reflection silently fails → `getSupplier(CROW)` null → `LivingEntity` constructor NPE) is **unchanged and fully re-confirmed** via bytecode of the real 26.2 `LivingEntity` (`new AttributeMap` → `setHealth(getMaxHealth())` on a null supplier).

---

## AGENTS.md reporting section

### 1. Risk assessment
- **Highest risk (critical):** attribute-registration swallow-and-log hides a spawn-time NPE. The fix must use `FabricDefaultAttributeRegistry.register` (supported Fabric API, already in the dependency tree) or a Mixin. **Do not use `Unsafe.putObject`** — verified to SIGSEGV the JVM on JDK 25; `Field.set`/`VarHandle`/`modifiers` all throw.
- **Behavioral risk:** the vanilla recipe overrides change crafting for all modded players (brush/writable book gain a feather requirement; black feather becomes usable in three vanilla recipes), and replacing vanilla recipe files can silently conflict with other mods/datapacks that modify the same recipes.
- **Data risk:** none confirmed (loot table valid, tag resolves).

### 2. Confidence level
- Critical finding: **high** — empirical (deobfuscated bytecode + JDK 25 runtime tests, re-verified end-to-end).
- Distress audio (triple) + payload defect: **high** — confirmed in `SwarmManager`, `SwarmDistressGoal`, and client handler.
- Dead-code findings: **high** — grep-verified no callers.
- Recipe-overrite auditability: **high** — files read.
- Biome doc mismatch: **high** — `IS_DESERT` tag absence verified in deobfuscated `BiomeTags`.
- Nest state trap: **medium** — logic-path confirmed; reachable only via corrupt/migrated save (not normal operation).
- A* worst-case cost: **high** — constants and loop verified; cost is 2,000×(T+130) checks per failed path (≈262k lower bound at T≈1; ≈460k–516k at typical 70–100 block distances).

### 3. Path to improvement
1. Fix attribute registration via `FabricDefaultAttributeRegistry.register(CROW, CrowEntity.createAttributes().build())` (supported Fabric API — **not** Unsafe, which crashes the JVM on JDK 25) + fail-fast assertion.
2. Consolidate distress audio (server OR client) and fix CRIT target + drop dead `targetPos`.
3. Add crow-death cleanup to SwarmManager; wire or drop `checkCooldown`/`checkRetaliation`/`Mode.RETALIATION`.
4. Remove dead navigation state (`closest`, `isPathValid`, `getMaxSearchNodes`).
5. Update docs: biomes (IS_BADLANDS vs "desert"), trampling wording, and enumerate the three overridden vanilla recipes.
6. Stale test count in `TEST_PLAN.md` (38 → 42).

### 4. Intent adherence
The mod's stated intent (tameable crow with breeding, scavenging, swarm distress, and a feather economy) is architecturally coherent and the navigation/breeding/nest subsystems are implemented correctly in 26.2. The feather economy's viability depends on the critical attribute-registration fix; until then the intent is unfulfilled. All PAWS categories are addressed above.
