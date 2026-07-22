# Crow Buddy: Phase 3 Low-Level Design (LLD)

> Historical design record. Shoulder perching was removed after Phase 3; current behavior uses the inherited sitting pose. Refer to `README.md` and `HLD.md` for the current feature set.

## 1. Entity Implementation (Core Mechanics)

### 1.1. Entity Data (SynchedEntityData)
All critical states are tracked via `SynchedEntityData` to ensure automatic persistence (MC 26.2 `ValueInput`/`ValueOutput` pattern).
- `SITTING` (inherited from `TamableAnimal` via `setOrderedToSit()`/`isOrderedToSit()`): Suppresses all behaviors.
- `PERCHED` (boolean): Toggled via right-click on tamed crows. When active, all AI goals are disabled and the crow follows the owner at shoulder height.
- `STATE` (int): Maps to the `CrowState` enum: `IDLE`, `SEARCHING`, `CARRYING`, `COMBAT`, `DISTRESS`, `SWARM`.
- `CARRIED_ITEM` (ItemStack): Tracks the item currently held in the crow's mouth during scavenging.
- `SATIATION` (float): Tracks food levels (0.0–1.0). Affects scavenging frequency.
- `RELATIONSHIP` (float): Tracks player aggression/affinity.

### 1.2. Taming & Breeding
- **Taming:** Uses `TamableAnimal` mechanics. The primary item is `black_oil_sunflower_seeds`.
- **Breeding Trigger:** Dual-feed pattern (vanilla alignment). Players right-click with food to trigger the "in love" state. No satiation gate is used for breeding.
- **Offspring:** Breeding triggers a mating sequence (hearts/particles) followed by a nest-seeking goal.

### 1.3. AI & Behavior Systems

#### Scavenging
- **Mechanism:** Proximity-based acquisition. If within 0.5 to 1.0 blocks of a dropped item, it is added to the `CARRIED_ITEM` slot.
- **Drop Logic:** Items are dropped at the player's location when fed, or at the crow's location during combat/distress events.
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
    - **Targeting:** Hostile mobs are attacked indefinitely. Players are targeted for a 4-second sliding window.
    - **Navigation:** Uses standard ground-based `PathNavigation`.

#### Perching
- **Toggle:** Right-clicking a tamed crow toggles the `PERCHED` state.
- **Behavior:** While perched, the crow disables all AI goals and follows the owner's movement/position.

### 1.4. Networking
- **Payloads:**
    - `DistressPayload`: Broadcasts distress events (Entity ID, BlockPos, Source ID).
    - `ScavengePayload`: Synchronizes item acquisition/drops (Crow ID, ItemStack).
- **Pattern:** Uses modern MC 26.2 `CustomPacketPayload` + `PayloadTypeRegistry`.
