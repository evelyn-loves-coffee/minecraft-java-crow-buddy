# Crow Buddy: Phase 3 Low-Level Design (LLD)

> Updated design record. Shoulder perching was removed; current behavior uses the inherited sitting pose.

## 1. Entity Implementation (Core Mechanics)

### 1.1. Entity Data and Persistence
Runtime state shared with clients is tracked through `SynchedEntityData`. Persistent crow data is written and loaded separately through the MC 26.2 `ValueOutput`/`ValueInput` hooks.
- `SITTING` (inherited from `TamableAnimal` via `setOrderedToSit()`/`isOrderedToSit()`): Suppresses all behaviors.
- `STATE` (int): Maps to the `CrowState` enum: `IDLE`, `SEARCHING`, `CARRYING`, `COMBAT`, `DISTRESS`, `SWARM`, `NESTING`.
- `CARRIED_ITEM` (ItemStack): Tracks the item currently held in the crow's mouth during scavenging.
- `SATIATION` (float): Tracks food levels (0.0–1.0). Affects scavenging frequency.
- `RELATIONSHIP` (float): Tracks player aggression/affinity.

### 1.2. Taming & Breeding
- **Taming:** Uses `TamableAnimal` mechanics. The primary item is `black_oil_sunflower_seeds`.
- **Breeding Trigger:** Dual-feed pattern (vanilla alignment). Players right-click with food to trigger the "in love" state. No satiation gate is used for breeding.
- **Offspring:** Breeding triggers a mating sequence followed by one parent's leaf-canopy nest-building goal.

### 1.3. AI & Behavior Systems

#### Scavenging
- **Mechanism:** Proximity-based acquisition. If within 0.5 to 1.0 blocks of a dropped item, it is added to the `CARRIED_ITEM` slot.
- **Drop Logic:** Feeding or combat/distress can trigger a carried-item drop; the item entity is always created at the crow's current position.
- **Priority (Weighted):**
    1. `minecraft:beacon_payment_items`
    2. `minecraft:piglin_loved`
    3. `minecraft:trim_materials`
    4. Fallback: Nearest item.
- **Frequency:** Frequency of searching is inversely proportional to satiation levels.

#### Swarm Intelligence (The "Distress" System)
- **Trigger (Untamed):** Attacking an untamed crow triggers a 32-block radius distress event (6 total: source + 5 nearest).
- **Trigger (Tamed):**
    - **Single Hit:** Full retaliation engagement (~2s).
    - **Escalation:** 3 hits within a 30s sliding window triggers full swarm escalation.
    - **Defending Player:** Swarm triggers only when the owner attacks a **hostile mob**.
- **Swarm Rules:**
    - **Cap:** 6 total crows (Source + 5 nearest via `distanceSquared` sort).
    - **Networking:** `DistressPayload` (S→C) broadcasts entity ID, position, and source.
    - **Cooldown:** 15-second (300-tick) per-crow cooldown between swarm participations.
    - **Targeting:** Player targets use a 4-second sliding window; each swarm engagement is capped at 200 ticks (10 seconds).
    - **Navigation:** Uses standard ground-based `PathNavigation`.

#### Neutral Sitting
- **Toggle:** Any non-food owner right-click toggles the inherited ordered-to-sit state.
- **Behavior:** Sitting clears navigation, airborne motion, vanilla/swarm targets, and per-crow retaliation state. Standing never restores an old target.
- **Swarm isolation:** Sitting crows neither initiate nor join swarm responses.

### 1.4. Networking
- **Payloads:**
    - `DistressPayload`: Broadcasts distress events (Entity ID, BlockPos, Source ID).
    - `ScavengePayload`: Synchronizes item acquisition/drops (Crow ID, ItemStack).
- **Pattern:** Uses modern MC 26.2 `CustomPacketPayload` + `PayloadTypeRegistry`.
