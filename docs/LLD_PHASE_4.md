# Crow Buddy: Phase 4 Low-Level Design (LLD)

## 1. Crow Spawning

- Fabric `BiomeModifications` adds crows to overworld creature spawns.
- Spawn weight is 2 with groups of 2–4.
- Crow nests are never added during chunk generation.

## 2. Breeding Nest Construction

- Vanilla breeding selects one parent to enter `inMatingState`; the other receives its breeding cooldown but does not build.
- `CrowNestBuildGoal` searches exposed canopy positions in expanding rings up to 16 blocks from the parent.
- A site is valid only when the support is in `#minecraft:leaves`, the block above is air, the sky is visible, and the position is inside the world border.
- Search complexity is bounded to 1,089 heightmap columns (`O(r²)` at radius 16). Invalidated targets are rechecked every 100 ticks for up to 1,200 ticks.
- The parent walks or flies to the site, revalidates it, places the internal-only nest block, and starts incubation.
- No player-obtainable nest item is registered.

## 3. Lifecycle

1. `IDLE` is the safe unloaded/default state.
2. `EGGS` incubates for 12,000 ticks.
3. `HATCHING` animates for 100 ticks.
4. Hatch completion spawns exactly one baby and removes the nest without a drop.

Legacy saved post-hatch stages are removed on their next server tick without spawning another baby. Removing or decaying the supporting leaves also removes the nest.

## 4. Trampling

- The block has a shallow collision shape matching its model.
- Non-careful walking has a 1-in-100 break chance; landing has a 1-in-3 chance.
- Players and living mobs can trample; crows and sneaking entities cannot.
- Mob trampling respects `mobGriefing`; player trampling does not.
