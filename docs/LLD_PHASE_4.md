# Crow Buddy: Phase 4 Low-Level Design (LLD)

## 1. Procedural Spawning

### 1.1. Implementation Strategy
- **Mechanism:** Uses Fabric's `BiomeModifications` for high compatibility.
- **Biome Selection:** Uses `BiomeSelectors.foundInOverworld()` to ensure crows spawn in all overworld biomes (vanilla and modded), reflecting their adaptable nature.
- **Spawn Parameters:**
    - **Weight:** 2-3 (Rarer than wolves/foxes, but noticeable).
    - **Group Size:** Min 2, Max 4 (To create social flocks).

## 2. Crow Nest Implementation

### 2.1. Nest Block & Entity
- **Type:** `CrowNestBlock` implementing `EntityBlock` with a `CrowNestBlockEntity`.
- **Placement Restriction:** Nests must be placed adjacent to `#minecraft:logs` to ensure thematic consistency.
- **Lifecycle State Machine:** The `BlockEntity` manages the following states:
    1. `Idle`
    2. `Eggs`
    3. `Hatching`
    4. `Fledgling` (briefly in nest)
    5. `Baby Flying`
    6. `Adult`
- **Persistence:** Uses MC 26.2 `ValueInput`/`ValueOutput` for state and timers.

### 2.2. Procedural Feature Pipeline
- **Feature Type:** Custom `CrowNestFeature` using a `ConfiguredFeature`/`PlacedFeature` pipeline.
- **Placement Rule:**
    - Validates target is a log block via `#minecraft:logs` tag.
    - Places at Y-levels $\ge$ Sea Level.
    - Placed as `VEGETAL_DECORATION` during world generation.

### 2.3. Nest Lifecycle & Breeding
The nest enables a full breeding lifecycle modeled after vanilla turtle mechanics:
1. **Mating:** Two adults enter the nest (teleported to perch position) after a dual-feed trigger.
2. **Egg Stage:** A single egg is placed on/in the nest.
3. **Incubation:** A 12,000 tick (~60s) timer runs within the `BlockEntity`.
4. **Hatching:** Upon timer completion, an animation (particle burst) plays and a fledgling `CrowEntity` is spawned.
5. **Juvenile/Baby Stage:** The fledgling transitions to a "Baby Flying" stage with limited combat capabilities and a 24,000 tick (~120s) growth timer.
6. **Adulthood:** After growth, the crow becomes a full adult and the nest resets to `Idle`.

### 2.4. Destruction & Edge Cases
- **Nest Breakage:**
    - If broken during **Incubation**: Eggs are lost.
    - If broken during **Fledgling/Baby** stages: The baby crow is lost.
- **No Partial Recovery:** Breaking the nest does not drop the contents; it resets the state to `Idle`.
