# Phase 4: World & Environment — Design Document

## Overview

Phase 4 adds crows to the world through two mechanisms:
1. **Procedural entity spawning** via Fabric API `BiomeModifications`
2. **Crow Nest structures** as procedural features with block-tag compatibility

Both items ensure mod compatibility (no hard-coded biome IDs, no hard-coded block types) and deliver the "realistic crow presence" design goal.

---

## Part 1: Procedural Spawning via `BiomeModifications`

### 1.1. What Is `BiomeModifications`

`BiomeModifications` (Fabric API, `net.fabricmc.fabric.api.biome.v1.BiomeModifications`) provides a declarative API for adding/removing spawn entries to biomes *after* world generation but *before* the game runs. It replaces the pre-24.x `GlobalLootModifier` / `ModifySpawnLists` pattern and works with MC 26.2's overhauled world-gen pipeline.

**Key types:**
- `BiomeModifications` — static API entry point
- `addSpawn(BiomeSelectors.Predicate, EntityType, int weight, int min, int max)` — add entity to spawn list
- `BiomeSelectors` — predicate builders for biome selection (e.g., `filter()`, `include()`, `exclude()`)

### 1.2. Biome Selection Strategy

Crows are highly adaptable scavengers. The design calls for "température, humidity, and forest density" analysis, but `BiomeModifications` operates on *biome entries*, not continuous environmental vectors. We have two approaches:

#### Option A: Biome Whitelist (Recommended)
Explicitly list biomes where crows should spawn. A curated set of ~12-15 biomes covering:
- Forest variants (Oak, Dark, Flower, Birch, Bamboo Jungle)
- Plains variants (Plains, Sunflower, Meadow)
- Swamp variants (Swamp)
- Mountain/wooded edges (Windswept variants with trees)
- The "everywhere except extremes" approach: spawn in *all* biomes except Desert, Badlands, Ice Spikes, Dripstone Cave, Deep Dark, Nether, and End biomes.

**Rationale:** Most comprehensive, most compatible with mod-added biomes that follow vanilla naming conventions. Easy to tune.

#### Option B: Biome Tag-Based Selection
Use the `minecraft:is_overworld` tag approach or craft a custom biome tag in DataGen.

**Rationale:** Fully mod-compatible. But requires mods to tag their biomes appropriately. May be incomplete for modded content.

**Decision: `BiomeSelectors.foundInOverworld()` — spawns in ALL overworld biomes (vanilla + modded).**

This is a whitelist approach with `foundInOverworld()`. Crows are extremely adaptable in reality — they live in cities, farmland, and urban environments alongside forests. The only biomes that realistically exclude crows are deserts and extremely cold ones, but the "overworld" predicate already excludes Nether and End. The `foundInOverworld()` approach is the simplest and most mod-compatible.

**Alternative if finer granularity is desired:** `BiomeSelectors.foundInOverworld()` combined with `BiomeSelectors.excludeByKey()` for specific desert/cave biomes.

### 1.3. Spawn Parameters

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Weight | 2-3 | Rarer than wolves (8), foxes (8), rabbits (10). Crows are noticeable but not ubiquitous — ~1/3-1/4 the spawn frequency of wolves |
| Min group size | 2 | Crows are social; single crows are less visually interesting. Minimum flock of 2 |
| Max group size | 4 | Larger flocks feel dramatic. 2-4 creates a visible group without overcrowding |
| Spawn category | `MobCategory.CREATURE` | Already set in `ModEntities.java:12` |

**Comparison to vanilla CREATURE mobs:**

| Mob | Weight | Min | Max | Perception |
|-----|--------|-----|-----|------------|
| Parrot | 30 | 1 | 2 | Very common |
| Rabbit | 10 | 1 | 3 | Common |
| Wolf | 8 | 1 | 4 | Moderate |
| Fox | 8 | 1 | 2 | Moderate |
| **Crow** | **2-3** | **2** | **4** | **Rare but noticeable** |

### 1.4. Implementation

New file: `src/main/java/com/crowbuddy/worldgen/CrowSpawning.java`

**Prerequisites:**
- `CrowBuddyClient.java` — must be created (currently missing). Referenced in `fabric.mod.json` but file absent from source. Required for particles and BE renderer.
- `CrowEntity.getBreedingOffspring()` — must be fixed (currently returns `null`). Must return new `CrowEntity` and call `setBaby(true)` for offspring.

**Actual API signature (verified):**
```java
BiomeModifications.addSpawn(
    BiomeSelectors.foundInOverworld(),   // Predicate<BiomeSelectionContext>
    MobCategory.CREATURE,
    ModEntities.CROW,
    2,   // weight — rare (1/4 of wolf frequency)
    2,   // min group — social flocks
    4    // max group — dramatic larger groups
);
```

Registration wired in `CrowBuddy.onInitialize()` via `CrowSpawning.initialize()`.

**Biome selection — verified methods in `BiomeSelectors`:**
- `foundInOverworld()` — matches all overworld biomes (vanilla + modded) ✅ **chosen**
- `excludeByKey(keys)` — if we want to exclude specific biomes (e.g., Desert, Badlands)
- `includeByKey(keys)` — whitelist approach if needed later
- `tag(tagKey)` — biome tag-based selection for datapack-driven spawning

---

## Part 2: Crow Nest — Procedural Feature

### 2.1. Design Overview

The Crow Nest is a decorative, placed structure — not a block that the player crafts. It's a cluster of sticks/sticks-with-leaves that attaches to tree trunks, created procedurally during world generation.

**Key properties:**
- Decorative: no inventory interaction, no crafting recipe
- Procedural placement via `ConfiguredFeature` + `PlacedFeature`
- Attachment: uses `minecraft:logs` block tag to find valid surfaces
- Capacity: visual/perceptual only — up to 2 adults + 1 baby can perch nearby

### 2.2. Block or Structure?

Two approaches exist:

#### Option A: Block Entity (CrowNestBlock)
A real block placed during generation. Pros: collision handling, right-click interaction, baby crow spawning. Cons: requires block item, recipe, or datapack placement logic.

#### Option B: Multi-block Structure / Decorative Blocks (Recommended)
The nest is a *placement of existing blocks* (e.g., 3x3x2 cluster of `OAK_PLANKS` or raw `STICK`-like geometry) placed during generation. No new block type. The "nest" is a spatial concept, not a block type.

**Decision: Option B — procedural placement of existing, tagged blocks.** This keeps the mod lightweight and avoids block-item-recipe complexity for a decorative element.

**Alternative if interaction is desired:** A single `CrowNestBlock` placed at the feature's center, surrounded by decorative blocks. This gives us right-click interaction for baby-spawning without needing a full structure.

#### Revised Decision: BlockEntity + BlockItem
A `CrowNestBlock` that returns a `CrowNestBlockEntity`. The block entity manages:
- Lifecycle state: empty → eggs → incubating → hatching → juveniles → empty
- Tick timers for egg incubation and juvenile growth
- Crow occupancy tracking (adults entering/exiting)
- Baby crow spawning on hatch

A `BlockItem` + crafting recipe allow player placement, enabling controlled breeding setups.

### 2.3. `CrowNestBlock` + `CrowNestBlockEntity` Design

```java
public class CrowNestBlock extends Block implements EntityBlock {
    public CrowNestBlock() {
        super(BlockBehaviour.Properties.of()
            .noCollission()           // Players walk through it (perch-like)
            .strength(0.3f)           // Fragile (like honeycomb)
            .noOcclusion()            // Renders from all sides
            .sound(SoundType.WOOD)
            .mapColor(MapColor.BROWN)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrowNestBlockEntity(pos, state);
    }
}
```

```java
public class CrowNestBlockEntity extends BlockEntity {

    // Lifecycle constants (D7: single offspring, guaranteed hatch)
    static final int INCUBATION_TICKS = 12000;  // ~60s real time for eggs → hatch
    static final int JR_GROWTH_TICKS = 24000;   // ~120s for baby flying → adult
    static final int MAX_EGGS = 1;              // single egg per cycle
    static final int MAX_JUVENILES = 1;         // single juvenile per cycle

    // Lifecycle state (D8: extended lifecycle with baby flying stage)
    int stage;          // 0=idle, 1=eggs, 2=hatching, 3=fledgling, 4=baby_flying
    int ticksRemaining; // countdown within current stage
    UUID babyUuid;      // single tracked baby entity (MaxJuv=1)

    // MC 26.2 persistence — ValueInput/ValueOutput (NOT CompoundTag)
    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putInt("stage", this.stage);
        output.putInt("ticks", this.ticksRemaining);
        output.putInt("eggs", this.eggCount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        this.stage = input.getIntOr("stage", 0);
        this.ticksRemaining = input.getIntOr("ticks", 0);
        this.eggCount = input.getIntOr("eggs", 0);
    }

    // Tick for incubation / juvenile countdown
    public static void tick(Level level, BlockPos pos, BlockState state, CrowNestBlockEntity be) {
        if (!level.isClientSide && be.ticksRemaining > 0) {
            be.ticksRemaining--;
            if (be.ticksRemaining <= 0) {
                be.advanceStage(level, pos); // advance or hatch
            }
            be.setChanged();
        }
    }
}
```

**Persistence note (MC 26.2 BREAKING CHANGE):** Override `loadAdditional(ValueInput)` and `saveAdditional(ValueOutput)`. Do NOT use `CompoundTag.load()/save()` — those don't exist on BlockEntity in 26.2.

**BlockItem + Recipe:**
- `CrowNestBlockItem` registered alongside block
- Crafting recipe: sticks + wool (or similar lightweight recipe)
- Player-placed nests follow the same lifecycle as world-gen nests

### 2.4. Procedural Feature Pipeline

Minecraft's feature system works in layers:

1. **`BlockStateProvider`** → What block state to place
2. **`Feature`** → How to place it (placement rules, shape, validation)
3. **`ConfiguredFeature`** → Specific configuration of a feature
4. **`PlacedFeature`** → When/where the configured feature is placed (decorators)

#### 4.4.1 Configuration

The nest feature places a single `crowbuddy:crow_nest` block on the side of a log block. The placement rule:
- Look for blocks matching `#minecraft:logs`
- Place `crow_nest` in a random air/solid block adjacent to the log face
- Only at Y-levels ≥ sea level (crows are arboreal, not underground)

#### 2.4.2 Implementation Strategy

**Registration chain (verified against MC 26.2):**

1. **Custom `Feature` subclass** — `CrowNestFeature extends Feature<NoneFeatureConfiguration>`. Constructor takes `Codec<NoneFeatureConfiguration>`. Override `abstract boolean place(FeaturePlaceContext<FC>)`.
   - Register in `BuiltInRegistries.FEATURE` during `onInitialize()`.
   - `FeaturePlaceContext` provides: `level() -> WorldGenLevel`, `random() -> RandomSource`, `origin() -> BlockPos`, `config() -> FC`.

2. **`ConfiguredFeature`** — `new ConfiguredFeature<>(CROW_NEST, NoneFeatureConfiguration.INSTANCE)`.
   - This is a `Record(F, FC)` — immutable, constructed directly.
   - **Must be registered** in `Registries.CONFIGURED_FEATURE` with a `ResourceKey<ConfiguredFeature<?,?>>`.

3. **`PlacedFeature`** — `new PlacedFeature(configuredFeatureHolder, List.of(CountPlacement.of(2)))`.
   - This is a `Record(Holder<ConfiguredFeature>, List<PlacementModifier>)`.
   - **Must be registered** in `Registries.PLACED_FEATURE` with a `ResourceKey<PlacedFeature>`.

4. **`BiomeModifications.addFeature()`** — Add the placed feature to target biomes:
   ```java
   BiomeModifications.addFeature(
       BiomeSelectors.foundInOverworld(),
       GenerationStep.Decoration.VEGETAL_DECORATION,
       PlacedFeatureKey  // ResourceKey<PlacedFeature>
   );
   ```

**Critical constraint:** `BiomeModifications.addFeature()` takes a `ResourceKey<PlacedFeature>`, not a `PlacedFeature` instance. The placed feature must exist in the registry before biome modification runs. This means `ConfiguredFeature` and `PlacedFeature` registration timing must precede `BiomeModifications` setup.

### 2.5. File/New Classes

| File | Package | Purpose |
|------|---------|---------|
| `CrowNestBlock.java` | `com.crowbuddy.block` | Nest block + `EntityBlock` interface |
| `CrowNestBlockEntity.java` | `com.crowbuddy.block.entity` | Lifecycle state machine: eggs → incubation → hatch → juvenile → fledge |
| `CrowNestBlockItem.java` | `com.crowbuddy.item` | BlockItem for player placement |
| `CrowsEggBlock.java` *(optional)* | `com.crowbuddy.block` | Egg visual block (or repurpose `TurtleEggBlock`) |
| `CrowNestSeekGoal.java` | `com.crowbuddy.entity.ai.goal` | AI goal: pathfind to nearest nest after mating |
| `CrowNestFeature.java` | `com.crowbuddy.worldgen.feature` | Custom feature that places nest on log face |
| `ModBlocks.java` | `com.crowbuddy.registry` | Block + BlockEntity registration |
| `ModItems.java` | `com.crowbuddy.registry` | BlockItem + any breeding food item |
| `ModConfiguredFeatures.java` | `com.crowbuddy.worldgen` | ConfiguredFeature registration |
| `ModPlacedFeatures.java` | `com.crowbuddy.worldgen` | PlacedFeature registration |
| `CrowSpawning.java` | `com.crowbuddy.worldgen` | BiomeModifications wiring |
| `ModWorldGen.java` | `com.crowbuddy.worldgen` | Master initialization that calls all above |

### 2.6. Registration Flow & Timing

```
CrowBuddy.onInitialize()
├── ModEntities.register()          // EntityType<CrowEntity> in BuiltInRegistries.ENTITY_TYPE
├── ModBlocks.register()            // CrowNestBlock in BuiltInRegistries.BLOCK
├── ModWorldGen.register()          // Master initializer
│   ├── ModFeatures.register()      // CrowNestFeature in BuiltInRegistries.FEATURE
│   ├── ModConfiguredFeatures.register()  // ConfiguredFeature in Registries.CONFIGURED_FEATURE
│   ├── ModPlacedFeatures.register()     // PlacedFeature in Registries.PLACED_FEATURE
│   └── CrowSpawning.initialize()
│       ├── BiomeModifications.addSpawn()       // Crow entity spawning
│       └── BiomeModifications.addFeature()     // Nest feature (requires PlacedFeature pre-registered)
```

**Timing constraint:** Steps in `ModWorldGen.register()` MUST complete before `CrowSpawning.initialize()` runs, because `BiomeModifications.addFeature()` references a `ResourceKey<PlacedFeature>` that must already be registered.

### 2.7. Crow Breeding & Nest Lifecycle (Turtle Precedent)

Full lifecycle modeled after turtle egg/hatch mechanics. This is a **scope expansion into Phase 5 territory** but implemented in Phase 4 because it requires the nest BlockEntity.

#### Lifecycle States

```
  ┌─────────────────────────────────────────────────────────────────┐
  │                       NEST LIFECYCLE                             │
  │                                                                  │
  │  EMPTY ──(mating adults enter)──> EGGS ──(incubation 12000t)──>  │
  │       ↑                                               │          │
  │       │                                               ▼          │
  │       │                                        HATCHING          │
  │       │                                               │          │
  │       │                                               ▼          │
  │       │                                    FLEDGLING (in nest)   │
  │       │                                          (brief)         │
  │       │                                               │          │
  │       │                                               ▼          │
  │       │                                    BABY FLYING           │
  │       │              (limited non-combat capabilities)           │
  │       │                     (growth timer 24000t)               │
  │       │                                               │          │
  │       │                      (or golden dandelion →   │          │
  │       │                       keep as baby)           ▼          │
  │       └─────────────────────────────────────────── ADULT         │
  └─────────────────────────────────────────────────────────────────┘
```

#### Phase 1: Mating & Nest Location

1. Two adult crows enter "mating state" — hearts particle effect (`ParticleTypes.HEART`), similar to vanilla animal mating
2. Mating trigger: right-click with food item (dual-feed vanilla pattern). No satiation gate (D3: vanilla alignment)
3. Both crows pathfind to nearest `CrowNestBlock` within X blocks (e.g., 32)
4. Both crows "enter" nest — teleported to perch position, play animation, hearts appear
5. After short delay, crows leave nest (return to normal AI)
6. BlockEntity transitions: `idle → eggs`
7. Sound: `crow.mate` placeholder (D10, Phase 5 audio)

#### Phase 2: Eggs (Incubation)

1. Nest BlockEntity places 1 egg on/above the nest block (D7: MaxEggs=1)
2. Egg visual: custom model or repurposed turtle egg block placed on nest
3. Incubation timer: `INCUBATION_TICKS = 12000` (~60s real time)
4. Eggs are visible via BlockEntity renderer or placed blocks
5. If nest is broken before incubation completes, eggs are lost
6. Sound: `crow.egg_lay` placeholder (D10, Phase 5 audio)

#### Phase 3: Hatching & Fledgling

1. After incubation: `eggs → hatching` — brief animation sequence, particle burst
2. Fledgling crow spawns in nest — `CrowEntity` with `setBaby(true)` via `AgeableMob`
3. Fledgling stays in nest briefly (short timer), then transitions to baby flying
4. Sound: `crow.hatch` placeholder (D10, Phase 5 audio)

#### Phase 4: Baby Flying (D8 revised)

1. Fledgling leaves nest → becomes flying baby crow: `stage = 4 (baby_flying)`
2. Baby flying crow has **limited non-combat capabilities** compared to adults:
   - Reduced or restricted combat AI goals
   - Retains mobility, following, wandering, tamable behavior
   - Visual: smaller scale model via `AgeableMob.isBaby()`
3. Growth timer: `JR_GROWTH_TICKS = 24000` (~120s) until adulthood
4. `FollowParentGoal` for baby to follow nearby adults
5. Golden dandelion (MC 26.2 Tiny Takeover) can keep crow in baby form indefinitely
6. Sound: `crow.baby_flight` placeholder (D10, Phase 5 audio)

#### Phase 5: Growth to Adult

1. After growth timer (or `ageUp()` call): baby crow grows to adult
2. Fledging animation: `ParticleTypes.CRIT` burst
3. Crow transitions to full adult AI behavior and capabilities
4. Nest BlockEntity resets: `baby_flying → idle`
5. Sound: `crow.grow` placeholder (D10, Phase 5 audio)

#### Implementation Notes

| Component | Class | Notes |
|-----------|-------|-------|
| Mating trigger | Extend `CrowEntity` | Right-click with food item → hearts → mate. No satiation gate (D3). Sound placeholder `crow.mate` (D10) |
| Nest-seeking AI goal | `CrowNestSeekGoal` | Pathfind to nearest `CrowNestBlock` within range |
| "Enter nest" animation | Server-side particle + client-side animation | Crow positions at nest perch |
| Egg entity | `CrowEggBlock` or repurpose `TurtleEggBlock` | Single egg (D7: MaxEggs=1). Sound placeholder `crow.egg_lay` (D10) |
| Fledgling | `CrowEntity.isBaby()` | Spawns in nest. Sound placeholder `crow.hatch` (D10) |
| Baby flying crow | `CrowEntity.isBaby()` | Limited non-combat capabilities. Sound placeholder `crow.baby_flight` (D10) |
| BlockEntity state | `CrowNestBlockEntity` | Persistent: stage, tick counters, baby UUID |
| Growth timer | BlockEntity tick | Server-side countdown, baby → adult via `AgeableMob.ageUp()` |
| Golden dandelion | Vanilla Tiny Takeover | Keeps crow in baby form (D8) |
| Adult transition | `AgeableMob.ageUp()` | Sound placeholder `crow.grow` (D10) |

#### Precedent: Turtle Lifecycle + AgeableMob

| Turtle Step / Vanilla Mechanism | Crow Equivalent | MC API |
|-------------|----------------|--------|
| Turtle lays eggs on beach | Adults "enter" nest, 1 egg spawns | BlockEntity tick + entity placement |
| Eggs have incubation stage | Incubation countdown (12000t) | BlockEntity tick countdown |
| Eggs hatch with animation | Hatch animation + fledgling spawn | `ParticleTypes.CRIT`, entity summon |
| Baby turtles follow parent | Baby crows follow nearby adult | `FollowParentGoal` (vanilla AI goal) |
| Baby growth → adult | Baby flying → adult (24000t) | `AgeableMob.ageUp()` |
| Golden dandelion (MC 26.2) | Keeps crow in baby form | Vanilla Tiny Takeover mechanic |
| `AgeableMob.isBaby()` | Baby flying visual + capability limit | Entity data sync |

---

## Assumptions

### Method: Binary Inspection of JARs

All assumptions below validated via `javap` against cached JARs on this machine (2025-07-13):

| Artifact | Path |
|----------|------|
| MC 26.2 deobf | `~/.gradle/caches/fabric-loom/minecraftMaven/.../minecraft-common-deobf-26.2.jar` |
| Fabric API 0.154.2+26.2 | `~/.gradle/caches/modules-2/.../fabric-api-0.154.2+26.2.jar` |
| Fabric biome-api-v1 18.0.6 | `~/.gradle/caches/modules-2/.../fabric-biome-api-v1-18.0.6+c7bd5b8e9e.jar` |

### Locked (Verified Against Runtime Binary)

| # | Assumption | Status | Evidence |
|---|-----------|--------|----------|
| **A20** | `BiomeModifications.addSpawn()` exists and works | ✅ **LOCKED** | `javap`: `addSpawn(Predicate<BiomeSelectionContext>, MobCategory, EntityType<?>, int, int, int)` — signature confirmed in `fabric-biome-api-v1-18.0.6.jar` |
| **A20b** | `BiomeModifications.addFeature()` exists for procedural features | ✅ **LOCKED** | `javap`: `addFeature(Predicate<BiomeSelectionContext>, GenerationStep.Decoration, ResourceKey<PlacedFeature>)` — confirmed. **Note:** requires `PlacedFeature` to be pre-registered in `Registries.PLACED_FEATURE` |
| **A21** | `BiomeSelectors` provides biome predicates | ✅ **LOCKED (CORRECTED)** | Actual methods: `all()`, `vanilla()`, `foundInOverworld()`, `foundInTheNether()`, `foundInTheEnd()`, `tag(TagKey<Biome>)`, `excludeByKey(ResourceKey<Biome>...)`, `includeByKey(ResourceKey<Biome>...)`, `spawnsOneOf(EntityType<?>...)`. No `filter()` method — use `foundInOverworld()` + potential `excludeByKey()` for blacklist |
| **A22** | `Feature` base class + `place()` signature | ✅ **LOCKED** | `javap`: `abstract class Feature<FC extends FeatureConfiguration>`, constructor takes `Codec<FC>`, abstract `boolean place(FeaturePlaceContext<FC>)`. Also static `Feature<NONE>` instances: `SIMPLE_BLOCK`, `BLOCK_BLOB`, etc. available for reuse |
| **A23** | `ConfiguredFeature` and `PlacedFeature` exist | ✅ **LOCKED** | `javap`: `ConfiguredFeature<FC, F>` is a `Record(feature, config)`. `PlacedFeature` is a `Record(feature: Holder<ConfiguredFeature>, placement: List<PlacementModifier>)`. **Both registries exist:** `Registries.CONFIGURED_FEATURE`, `Registries.PLACED_FEATURE` |
| **A24** | `BiomeModifications.create(Identifier)` for custom modifications | ✅ **LOCKED** | `javap`: `BiomeModification.add(ModificationPhase, Predicate<BiomeSelectionContext>, Consumer<BiomeModificationContext>)` — confirmed. Inner interfaces: `GenerationSettingsContext.addFeature(...)`, `MobSpawnSettingsContext.addSpawn(...)` |
| **A25** | Block tag `#minecraft:logs` exists and is populated | ✅ **LOCKED** | Core vanilla tag since pre-1.13; `BiomeSelectionContext.getBiome()` accessible for tag checks |
| **A26** | `BlockBehaviour.Properties` builder API is stable | ✅ **LOCKED** | Standard since MC 1.17; no 26.x overhaul touched block properties |
| **A27** | A `noCollission()` block renders normally with standard block model | ✅ **LOCKED** | Standard pattern (e.g., `campfire`, `ladder`) |
| **A28** | `FeaturePlaceContext` provides `level()`, `random()`, `origin()`, `config()` | ✅ **LOCKED** | `javap`: getters confirmed: `level() → WorldGenLevel`, `random() → RandomSource`, `origin() → BlockPos`, `config() → FC`, `topFeature() → Optional<ConfiguredFeature>` |
| **A29** | `GenerationStep.Decoration.VEGETAL_DECORATION` exists for surface feature placement | ✅ **LOCKED** | `javap`: enum values include `VEGETAL_DECORATION` (standard for trees, flowers, vegetation) |
| **A30** | `CountPlacement.of(int)` provides simple repetition decorator | ✅ **LOCKED** | `javap`: `CountPlacement.of(int)` and `CountPlacement.of(IntProvider)` — confirmed |

### Tier 1: Binary-Validated (2025-07-13)

| # | Assumption | Status | Evidence |
|---|-----------|--------|----------|
| **A31** | How to register `ConfiguredFeature`/`PlacedFeature` in MC 26.2 | ✅ **LOCKED** | `javap -verbose RegistryDataLoader.class` reveals `REGISTRIES.CONFIGURED_FEATURE` + `ConfiguredFeature.DIRECT_CODEC` and `REGISTRIES.PLACED_FEATURE` + `PlacedFeature.DIRECT_CODEC` are both in `WORLDGEN_REGISTRIES`. This means: **(a)** they are datapack-driven dynamic registries, **(b)** entries are populated from JSON files during datapack loading, **(c)** the JSON files use `DIRECT_CODEC` for deserialization, |(d) `FabricDynamicRegistryProvider$Entries` provides DataGen methods `placedFeatures()` and `add(key, value)` |
| **A32** | `#minecraft:logs` block tag still exists | ✅ **LOCKED** | Core vanilla tag, unchanged. |
| **A33** | `BlockEntity` + `BlockEntityTicker` exist | ✅ **LOCKED** | `javap`: `BlockEntity` abstract class, constructor `(BlockEntityType<?>, BlockPos, BlockState)`, methods `loadAdditional(ValueInput)`, `saveAdditional(ValueOutput)`, `collectComponents()`, `setComponents(DataComponentMap)` |
| **A34** | `BlockEntityType<T>` registration via bootstrap registry | ✅ **LOCKED** | `javap`: `BlockEntityType(BlockEntityType.BlockEntitySupplier<? extends T>, Set<Block>)`. `BuiltInRegistries.BLOCK_ENTITY_TYPE` exists as `Registry<BlockEntityType<?>>` |
| **A35** | BE persistence uses `ValueInput/ValueOutput`, NOT `CompoundTag` | ✅ **LOCKED (BREAKING)** | `javap`: `BlockEntity.loadAdditional(ValueInput)`, `BlockEntity.saveAdditional(ValueOutput)`. No `CompoundTag.load()` or `CompoundTag.save()` override exists |
| **A36** | `AgeableMob` exists and provides breeding | ✅ **LOCKED** | `javap`: abstract `AgeableMob getBreedOffserver()`, `canBreed()`, `isBaby()`, `setBaby()`, `setAge()`, `ageUp()`. `FollowParentGoal(Animal, double)` exists |
| **A37** | `Animal` provides love/breeding mechanics | ✅ **LOCKED** | `javap`: `canMate(Animal)`, `spawnChildFromBreeding()`, `canFallInLove()`, `setInLove(Player)`, `isInLove()` |
| **A38** | `TamableAnimal` inherits Animal breeding without override | ✅ **LOCKED** | `javap`: `TamableAnimal extends Animal`. No override of `canBreed()`, `canMate()`, or `getBreedOffspring()`. All breeding inherited from `Animal` |
| **A39** | `ParticleTypes.HEART` exists | ✅ **LOCKED** | `javap` at `net.minecraft.core.particles.ParticleTypes`: `HEART`, `CRIT`, `HAPPY_VILLAGER`, `NOTE` all exist as `SimpleParticleType` |
| **A40** | `BlockItem(Block, Item.Properties)` constructor | ✅ **LOCKED** | `javap`: standard constructor, `useOn(UseOnContext)`, `place(BlockPlaceContext)` |
| **A41** | `BuiltInRegistries.FEATURE` exists | ✅ **LOCKED** | Verified in previous session. |
| **A42** | CrowEntity inherits breeding from AgeableMob | ✅ **LOCKED** | `javap` chain: `CrowEntity → TamableAnimal → Animal → AgeableMob`. Breeding machinery inherited but `getBreedOffspring()` returns `null` (CrowEntity.java:266) — must be fixed |

### Registration Mechanism (A31 — Detailed)

**`CONFIGURED_FEATURE` and `PLACED_FEATURE` are datapack-driven dynamic registries.** This was confirmed by inspecting `RegistryDataLoader.WORLDGEN_REGISTRIES` bytecode, which references:
- `Registries.CONFIGURED_FEATURE` + `ConfiguredFeature.DIRECT_CODEC`
- `Registries.PLACED_FEATURE` + `PlacedFeature.DIRECT_CODEC`

These registries are:
- Populated from JSON files in `data/<namespace>/worldgen/configured_feature/` and `data/<namespace>/worldgen/placed_feature/`
- Loaded during world datapack initialization
- Frozen after world load (via `RegistryAccess.Frozen`)

**Two approaches exist for mod implementation:**

1. **Datapack JSON files (recommended):** Ship pre-built JSON under `src/main/resources/data/crowbuddy/worldgen/`. Simple, standard, works with world reload, compatible with datapack editors.
2. **Fabric DataGen:** Use `FabricDynamicRegistryProvider$Entries` to generate JSON at build time. Useful for code-to-JSON generation during development, but not used at runtime.

**Custom Feature registration (`CrowNestFeature` in `BuiltInRegistries.FEATURE`):**
- `BuiltInRegistries.FEATURE` is a vanilla bootstrap registry (`Registry<Feature<?>>`)
- Fabric's `ListenableRegistry` mixin wraps it, enabling entry-added callbacks
- `Registry.register()` works for non-datapack registries at mod init time
- The datapack JSON can reference the custom feature by Identifier (e.g., `"feature": "crowbuddy:crow_nest"`)

**Registration order required:**
1. `Registry.register(BuiltInRegistries.FEATURE, id("crow_nest"), CrowNestFeature.INSTANCE)` — mod init, registers Java class
2. Ship `data/crowbuddy/worldgen/configured_feature/crow_nest.json` — datapack JSON, references `"crowbuddy:crow_nest"`
3. Ship `data/crowbuddy/worldgen/placed_feature/crow_nest.json` — datapack JSON, references configured feature
4. `BiomeModifications.addFeature(..., ResourceKey.create(Registries.PLACED_FEATURE, id("crow_nest")))` — mod init, by reference only

**No runtime-programmatic registration of ConfiguredFeature/PlacedFeature is needed or possible.** The MC 26 worldgen pipeline requires these as datapack JSON.

---

## Open Questions

### Resolved (Via Binary Inspection)

| # | Question | Resolution |
|---|----------|------------|
| ~~OQ11~~ | Does MC 26.2 use `Feature`/`ConfiguredFeature`/`PlacedFeature`? | ✅ **CONFIRMED** — All three exist: `Feature<FC>` abstract class, `ConfiguredFeature` record (feature + config), `PlacedFeature` record (configured-feature + placement-modifiers). Both in `net.minecraft.world.level.levelgen.feature/` package and registered via `Registries.CONFIGURED_FEATURE` / `Registries.PLACED_FEATURE`. |
| ~~OQ12~~ | Is `BiomeModifications.addFeature()` viable? | ✅ **CONFIRMED** — Method exists. Caveat: requires `ResourceKey<PlacedFeature>` referencing a pre-registered placed feature in `Registries.PLACED_FEATURE`. |

### Active

| # | Question | Impact | Suggested Resolution |
|---|----------|--------|---------------------|
| **OQ13** | How do we register `ConfiguredFeature`/`PlacedFeature` in MC 26.2? | **Resolved** — Datapack JSON only (see A31 above) | Ship JSON files under `data/crowbuddy/worldgen/configured_feature/` and `data/crowbuddy/worldgen/placed_feature/`. Custom `CrowNestFeature` Java class registered in `BuiltInRegistries.FEATURE` normally. |

### Resolved (Design Decisions)

#### Breeding & Mating

| # | Question | Impact | Decision | Rationale |
|---|----------|--------|----------|-----------|
| **D1** | Tamed-only or wild mating? | High | **Tamed only** | Aligns with `TamableAnimal`. Player-controlled breeding. Prevents uncontrolled crow population. |
| **D2** | Mating trigger pattern? | High | **Dual-feed (vanilla pattern)** | `Animal.isInLove()`, `setInLove()`, `canMate()`, `spawnChildFromBreeding()` all exist. Zero new code — just fix `getBreedOffspring()`. |
| **D3** | Satiation gate? | Medium | **None (vanilla alignment)** | Vanilla `Animal.canFallInLove()` only checks `inLove` timer (bytecode verified: bytecode checks `this.inLove > 0`, nothing else). No vanilla mob uses satiation for breeding. Removed. |
| **D4** | Nest-seek AI priority? | Medium | **Priority 2** (between `TemptGoal` and `FollowOwnerGoal`) | Nest-seeking more important than wandering, less than following owner. Coherent integration into goal stack. |

#### Nest & BlockEntity

| # | Question | Impact | Decision | Rationale |
|---|----------|--------|----------|-----------|
| **D5** | Player placement validation? | Medium | **Log-adjacent only** | Must be placed adjacent to `#minecraft:logs`. Matches world-gen rule. Thematic consistency. |
| **D6** | Nest break drop behavior? | Medium | **State-aware, no partial recovery** | Break nest during incubation → eggs lost. Break during fledgling/baby stages → baby lost. No silk-touch recovery, no partial drops. Mirrors vanilla turtle eggs. Clean state reset to idle. |
| **D7** | Gestational timer values? | Low | **Incubation 12000t (~60s), Growth 24000t (~120s), MaxEggs=1, MaxJuv=1** | Max eggs/juveniles reduced to 1 (simpler, single-offspring cycle). No hatch rate percentage — eggs always hatch after incubation timer. |
| **D8** | Lifecycle stages? | High | **nest → egg → fledgling (in nest) → baby flying → adult** | Revised lifecycle: after hatching, fledgling stays in nest briefly, then becomes flying baby crow with limited non-combat capabilities. After vanilla-based duration, grows to adult. Golden dandelion (MC 26.2 Tiny Takeover) keeps babies in baby form. `AgeableMob` inheritance retained. |
| **D9** | `CrowState` enum additions? | Medium | **Add `NESTING` only** | `MATING` tracked by vanilla `isInLove()`. No `INCUBATING` — handled by BlockEntity tick, not entity state. Baby flying capabilities gated by `isBaby()` check in combat AI goals. |
| **D10** | Sounds for all lifecycle stages? | Medium | **Sounds for ALL stages, placeholders where missing, full implementation deferred to Phase 5** | Need sounds for: mate/hearts, egg-lay, hatch, fledgling-leave-nest, baby-flight, grow-to-adult. Placeholder sound events defined in Phase 4, actual audio assets and implementation deferred to Phase 5. See D8 for additional life stage. |

#### Pre-existing Decisions

| # | Question | Impact | Decision | Rationale |
|---|----------|--------|----------|-----------|
| **OQ14** | Block vs BlockEntity? | High | `BlockEntity` | Lifecycle state, tick management, baby tracking |
| **OQ15/OQ18** | Player-placeable? | Medium-High | `BlockItem` + recipe | Controlled breeding setups |
| **OQ16** | Spawn weight/limits? | Low | Weight 2-3, flock 2-4 | Rare but dramatic flocks |
| **OQ17** | Baby spawning tied to nest? | Very High | Full turtle-precedent lifecycle (D8 revised) | Mating → nest-seeking → enter nest → egg (1x) → incubation → hatch → fledgling (in nest) → baby flying (limited capabilities) → adult |
| **OQ21** | Mating food item? | High | Any valid crow food | Existing food system sufficient |
| **OQ22** | Nest-seek timeout? | Medium | 400 ticks (~20s) | Abort if no nest found |
| **OQ23** | Egg visual? | High | Particles only (Phase 4), BE renderer later | `HEART` for mating, `CRIT` for hatch |
| **OQ24** | Baby crow strategy? | Critical | Inherited `AgeableMob` + baby flying stage (D8) | `CrowEntity` already inherits `AgeableMob` via `TamableAnimal`. Baby flying has limited non-combat capabilities. Golden dandelion (Tiny Takeover) keeps baby form. |

---

## Implementation Plan

### Task Breakdown

#### Phase 4.1: Procedural Spawning
| Order | Task | Files | Dependency |
|-------|------|-------|------------|
| 1 | Create `CrowSpawning.java` | `src/main/java/com/crowbuddy/worldgen/CrowSpawning.java` | None |
| 2 | Implement biome exclusion predicate | `CrowSpawning.java` | 1 |
| 3 | Wire `BiomeModifications.addSpawn()` | `CrowSpawning.java` | 2 |
| 4 | Call `CrowSpawning.initialize()` from `CrowBuddy.onInitialize()` | `CrowBuddy.java` | 3 |
| 5 | Verify build + runtime test | — | 4 |

#### Phase 4.0: Mod Startup (BLOCKER)
| Order | Task | Files | Dependency |
|-------|------|-------|------------|
| 0a | Create `CrowBuddyClient.java` | `src/main/java/com/crowbuddy/client/CrowBuddyClient.java` | **BLOCKER** — `fabric.mod.json` references it as client entrypoint. Mod crashes on load without it. Required before any Phase 4 work. |

**Note:** `CrowEntity.getBreedOffspring()` fix is a Phase 4.4 task (breeding lifecycle), not a Phase 4.0 prerequisite. It's called out here for visibility but belongs with mating implementation.

#### Phase 4.1: Procedural Spawning
| Order | Task | Files | Dependency |
|-------|------|-------|------------|
| 6 | Create `CrowNestBlock.java` | `src/main/java/com/crowbuddy/block/CrowNestBlock.java` | None |
| 7 | Create `CrowNestBlockEntity.java` | `src/main/java/com/crowbuddy/block/entity/CrowNestBlockEntity.java` | 6 |
| 8 | Create `CrowNestBlockItem.java` | `src/main/java/com/crowbuddy/item/CrowNestBlockItem.java` | 6 |
| 9 | Create `ModBlocks.java` (block + BE registration) | `src/main/java/com/crowbuddy/registry/ModBlocks.java` | 7 |
| 10 | Create `ModItems.java` | `src/main/java/com/crowbuddy/registry/ModItems.java` | 8 |
| 11 | Register block + item in `onInitialize()` | `CrowBuddy.java` | 9-10 |
| 12 | Add block model JSON + texture placeholder | `assets/crowbuddy/models/block/crow_nest.json` | 6 |
| 13 | Add blockstate JSON | `assets/crowbuddy/blockstates/crow_nest.json` | 12 |
| 14 | Add item model JSON | `assets/crowbuddy/models/item/crow_nest.json` | 12 |
| 15 | Add crafting recipe JSON | `data/crowbuddy/recipes/crow_nest.json` | 8 |
| 16 | Add lang entries for block + item | `assets/crowbuddy/lang/en_us.json` | 6 |
| 17 | Verify build | — | 16 |

#### Phase 4.3: Nest Feature & Placement
| Order | Task | Files | Dependency |
|-------|------|-------|------------|
| 18 | Create `CrowNestFeature.java` | `src/main/java/com/crowbuddy/worldgen/feature/CrowNestFeature.java` | 7 |
| 19 | Register feature in `ModFeatures.java` | `src/main/java/com/crowbuddy/worldgen/ModFeatures.java` | 18 |
| 20 | Create `data/crowbuddy/worldgen/configured_feature/crow_nest.json` | `src/main/resources/data/crowbuddy/worldgen/configured_feature/crow_nest.json` | 19 |
| 21 | Create `data/crowbuddy/worldgen/placed_feature/crow_nest.json` | `src/main/resources/data/crowbuddy/worldgen/placed_feature/crow_nest.json` | 20 |
| 22 | Wire feature to biomes via `BiomeModifications.addFeature()` | `CrowSpawning.java` or `ModWorldGen.java` | 21 |
| 23 | Verify build + runtime test | — | 22 |

#### Phase 4.4: Breeding Lifecycle (Turtle Precedent)
| Order | Task | Files | Dependency |
|-------|------|-------|------------|
| 24 | Implement mating trigger in `CrowEntity` | `src/main/java/com/crowbuddy/entity/CrowEntity.java` | Phase 2 |
| 25 | Create `CrowNestSeekGoal.java` (AI: pathfind to nest after mating) | `src/main/java/com/crowbuddy/entity/ai/goal/CrowNestSeekGoal.java` | 17 |
| 26 | Implement "enter nest" behavior (particles, animation, state transition) | `CrowEntity.java` + `CrowNestBlockEntity.java` | 25 |
| 27 | Implement egg spawning + incubation tick logic | `CrowNestBlockEntity.java` | 17 |
| 28 | Implement baby crow spawning (hatch) | `CrowNestBlockEntity.java` + `CrowEntity.java` | 27 |
| 29 | Implement juvenile growth timer + fledging | `CrowNestBlockEntity.java` + `CrowEntity.java` | 28 |
| 30 | Add baby crow model variant (smaller scale) | `assets/crowbuddy/models/entity/crow_baby.json` | 28 |
| 31 | Add hatch/leave particle effects | `CrowNestBlockEntity.java` + client | 28-29 |
| 32 | Verify full lifecycle in runtime test | — | 31 |

#### Phase 4.5: Integration & Verification
| Order | Task | Files | Dependency |
|-------|------|-------|------------|
| 33 | Add DataGen support (if needed for tag/loot/recipe) | `src/main/java/com/crowbuddy/datagen/` | 6-32 |
| 34 | Full build + runtime verification | — | 33 |

### Estimated Files Created
**~18-22 new files** (Java + JSON assets + blockstates/models + recipe JSON + BE renderer).

---

## Risk Assessment

### PAWS Review

#### Performance (P1)
- **`BiomeModifications`** — O(1) per-biome at world-load time. Zero runtime impact. ✅
- **Nest feature** — single block placement per feature invocation. With `CountPlacement` set to ~2-5 per chunk, the placement overhead is negligible (comparable to `FLOWERS_DEFAULT_CONFIG`). ✅
- **Log-face detection** — the feature checks adjacent blocks (`getBlockState()` on neighbors). This is a local check within the placement bounding box, not a global scan. ✅

#### Auditability (P2)
- All registrations logged via `CrowBuddy.LOGGER.info()` pattern established in Phase 2. ✅
- Feature placement uses standard MC world-gen trace for debug. ✅
- Biome filter is explicit and visible in code. ✅

#### Workability (P3)
- **Edge case: nest placed on invalid block** — feature validates target is `#minecraft:logs` before placement. ✅
- **Edge case: nest in unloaded chunk** — standard world-gen behavior; no async loading. ✅
- **Edge case: spawning fails silently** — `BiomeModifications` is declarative; failures are logged by Fabric API. ✅
- **Idempotency** — `BiomeModifications` calls are idempotent (additive predicates don't duplicate). ✅
- **Edge case: nest broken mid-lifecycle** — all eggs/babies lost. Acceptable (mirrors turtle eggs). BlockEntity NBT is freed on block break. ✅
- **Edge case: mating crows can't find nest** — timeout on `CrowNestSeekGoal` after N ticks; crows abort and return to normal AI. Needs design decision on timeout value. ⚠️

#### Scalability (P4)
- **Modularity** — each world-gen concern (spawning, features, blocks, breeding) is in its own class. ✅
- **Separation of concerns** — spawning logic independent of nest logic, independent of breeding logic. ✅
- **Mod compatibility** — `BiomeModifications` is the Fabric-standard approach; features use block tags. ✅
- **Future extension** — adding more features/blocks follows the same pattern. ✅
- **Thread safety** — BlockEntity runs on main tick thread; no async operations. AI goals run on entity tick thread. ✅

### Specific Risks

| Risk | Likelihood | Impact | Mitigation | Status |
|------|-----------|--------|------------|-------|
| MC 26.2 world-gen API changed | ~**Zero** | High | ✅ **ELIMINATED** — All types exist: `Feature`, `ConfiguredFeature`(Record), `PlacedFeature`(Record), `BiomeModifications`, `BiomeSelectors`. Signatures confirmed via `javap`. | ✅ |
| MC 26.2 entity/breeding API changed | ~**Zero** | **Critical** | ✅ **ELIMINATED** — `AgeableMob`, `Animal`, `TamableAnimal` chain all verified. `FollowParentGoal`, `ParticleTypes.HEART` exist. `CrowEntity` inherits breeding machinery. (A36-A42) | ✅ |
| BE persistence API changed from CompoundTag | **Guaranteed** | **High** | ✅ **RESOLVED** — MC 26.2 uses `ValueInput`/`ValueOutput`. Documented in A35. Override `loadAdditional()`/`saveAdditional()`. | ✅ |
| Feature pipeline broken | **Low** | High | Mitigated — standard types exist, signatures correct. Unknown: whether Fabric DataGen provides registration helpers for `ConfiguredFeature`/`PlacedFeature`. | ⚠️ |
| Registration timing conflict | **Low** | Medium | `BiomeModifications.addFeature()` requires pre-registered `ResourceKey<PlacedFeature>`. Ensure registration order: Feature → ConfiguredFeature → PlacedFeature → BiomeModifications. | 🛡️ |
| Nest block model work | **Low** | **Low** | Placeholder block model + texture. Final art deferred to Phase 5. | 🛡️ |
| Mod-heavy pack performance | **Low** | **Medium** | `BiomeModification` predicate-based — O(1) per biome at load time. | ✅ |
| Nest generation conflicts | **Low** | **Low** | `#minecraft:logs` tag; additive placement, non-destructive. | ✅ |
| Breeding lifecycle complexity | **Medium** | **High** | New BlockEntity state machine, AI goals, entity lifecycle, model variants. Each sub-system independently testable. | ⚠️ |
| Baby crow entity management (D8 revised) | **Medium** | **Medium** | Spawn tracking, parent-follow AI, growth timer, limited non-combat capabilities for baby flying stage. Use vanilla `FollowParentGoal` pattern. Golden dandelion integration for Tiny Takeover. Fledging logic must not leak entities. | ⚠️ |
| BlockEntity persistence/corruption | **Low** | **Medium** | `ValueInput`/`ValueOutput` serialization. Edge case: nest block broken mid-lifecycle → all state lost. Acceptable behavior (mirrors turtle eggs). | 🛡️ |
| `CrowBuddyClient` missing | **Guaranteed** | **High** | ✅ **RESOLVED** — Created `src/main/java/com/crowbuddy/client/CrowBuddyClient.java`. Client entrypoint now exists. Phase 4 features (BE renderer, baby model, particles) wired in later. | ✅ |
| `getBreedOffspring()` returns null | **Guaranteed** | **Critical** | `CrowEntity.java:266` returns `null`. Must return new `CrowEntity` and call `setBaby(true)` for offspring. Fix before breeding works. | ❌ BLOCKER |

### Overall Confidence: **7/10 — Medium**

**Spawning:** 9.5/10 — `BiomeModifications.addSpawn()` confirmed, one-line call.

**Nest feature + BlockEntity:** 7.5/10 — pipeline is structurally correct (all types exist, signatures verified). Breaking change: BE persistence uses `ValueInput/ValueOutput`, NOT `CompoundTag` (A35). D6: state-aware, no partial recovery on break.

**Breeding lifecycle:** 6.5/10 — all infrastructure APIs verified (A36-A42). `CrowEntity` already inherits `AgeableMob`. Critical fix needed: `getBreedOffspring()` returns `null` (CrowEntity.java:266) — must return new `CrowEntity`. Precedent exists via turtle mechanics. D8 revised: 5-stage lifecycle with baby flying capability gating adds complexity. D3: no satiation gate (vanilla alignment). D7: single offspring simplifies tracking.

### Verification Completed ✅

All 4 JAR-inspection checks performed 2025-07-13:

| Check | Artifact | Result |
|-------|----------|--------|
| `BiomeModifications.addSpawn(addFeature)` | `fabric-biome-api-v1-18.0.6.jar` | ✅ Both methods exist with expected signatures |
| `BiomeSelectors` predicates | `fabric-biome-api-v1-18.0.6.jar` | ✅ `foundInOverworld()`, `excludeByKey()`, `includeByKey()`, `tag()` all present |
| `Feature` + `place()` signature | `minecraft-common-deobf-26.2.jar` | ✅ `abstract boolean place(FeaturePlaceContext<FC>)`, constructor takes `Codec<FC>` |
| `ConfiguredFeature` / `PlacedFeature` | `minecraft-common-deobf-26.2.jar` | ✅ Both are `Record` types. Registries: `Registries.CONFIGURED_FEATURE`, `Registries.PLACED_FEATURE` |

**One remaining check (OQ13):** How `ConfiguredFeature`/`PlacedFeature` are registered in mod code. **RESOLVED** — Datapack JSON only, confirmed via `RegistryDataLoader.WORLDGEN_REGISTRIES` bytecode inspection.

### Tier 1: Entity & BlockEntity API Verification (Performed 2025-07-13)

| Check | Class | Result |
|-------|-------|--------|
| `BlockEntity` + persistence API | `net.minecraft.world.level.block.entity.BlockEntity` | ✅ `loadAdditional(ValueInput)`, `saveAdditional(ValueOutput)` — **NOT CompoundTag** |
| `BlockEntityType<T>` registration | `net.minecraft.world.level.block.entity.BlockEntityType` | ✅ Constructor `(BlockEntitySupplier<? extends T>, Set<Block>)` |
| `BuiltInRegistries.BLOCK_ENTITY_TYPE` | `net.minecraft.core.registries.BuiltInRegistries` | ✅ `Registry<BlockEntityType<?>>` exists |
| `EntityBlock` interface | `net.minecraft.world.level.block.EntityBlock` | ✅ `newBlockEntity(BlockPos, BlockState)`, `getTicker(Level, State, BlockEntityType<T>)` |
| `BaseEntityBlock` convenience | `net.minecraft.world.level.block.BaseEntityBlock` | ✅ Extends Block, implements EntityBlock |
| `BlockEntityTicker<T>` | `net.minecraft.world.level.block.entity.BlockEntityTicker` | ✅ `tick(Level, BlockPos, BlockState, T)` |
| `AgeableMob` + breeding | `net.minecraft.world.entity.AgeableMob` | ✅ `abstract getBreedOffspring()`, `canBreed()`, `isBaby()`, `setBaby()`, `ageUp()` |
| `Animal` love/breeding | `net.minecraft.world.entity.animal.Animal` | ✅ `canMate()`, `spawnChildFromBreeding()`, `canFallInLove()`, `setInLove(Player)`, `isInLove()` |
| `TamableAnimal` inherits breeding | `net.minecraft.world.entity.TamableAnimal` | ✅ Extends `Animal`, no override of breeding — inherits all |
| `FollowParentGoal` | `net.minecraft.world.entity.ai.goal.FollowParentGoal` | ✅ Constructor `(Animal, double speed)`, 3 range constants |
| `ParticleTypes.HEART` | `net.minecraft.core.particles.ParticleTypes` | ✅ `HEART`, `CRIT`, `HAPPY_VILLAGER`, `NOTE` all exist |
| `BlockItem` constructor | `net.minecraft.world.item.BlockItem` | ✅ `BlockItem(Block, Item.Properties)` |
| `CrowBuddyClient` existence | `src/main/java/com/crowbuddy/client/` | ✅ **CREATED** — client entrypoint exists, mod can now load |
| `getBreedOffspring()` implementation | `src/main/java/.../CrowEntity.java:266` | ❌ **Returns null** — must return new CrowEntity |

---

## Summary

| Dimension | Assessment |
|-----------|-----------|
| **Scope** | 3 deliverables: spawning + nest feature + breeding lifecycle (D8 revised) |
| **New files** | ~18-22 (Java + JSON) |
| **New classes** | 12-15 Java classes |
| **Primary risk** | Breeding lifecycle complexity — 5-stage lifecycle: nest→egg→fledgling→baby flying→adult, with capability gating for baby |
| **Confidence** | Spawning: 9.5/10 · Nest feature: 7.5/10 · Breeding lifecycle: 6.5/10 · Overall: 7.5/10 |
| **APIs verified** | All core JAR-inspection checks passed (see Verification Completed table) |
| **Estimated complexity** | High — pipeline structures confirmed, breeding lifecycle is substantial new scope |
