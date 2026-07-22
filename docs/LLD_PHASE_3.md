# Crow Buddy: Phase 3 Low-Level Design (LLD)

## 1. Entity Implementation (Core Mechanics)

### 1.1. Entity Data and Persistence
Runtime state shared with clients was tracked through `SynchedEntityData`. Persistent crow data was written and loaded separately through the MC 26.2 `ValueOutput`/`ValueInput` hooks.
- `SITTING` was inherited from `TamableAnimal` through `setOrderedToSit()` and `isOrderedToSit()` and suppressed all behaviors.
- `STATE` mapped an integer to the `CrowState` values `IDLE`, `SEARCHING`, `CARRYING`, `COMBAT`, `DISTRESS`, `SWARM`, and `NESTING`.
- `CARRIED_ITEM` tracked the `ItemStack` held in the crow's mouth during scavenging.
- `SATIATION` tracked food level from 0.0 to 1.0 and affected scavenging frequency.
- `RELATIONSHIP` tracked player aggression and affinity.

### 1.2. Taming & Breeding
- **Taming:** The design used `TamableAnimal` mechanics, with `black_oil_sunflower_seeds` as the primary taming item.
- **Breeding Trigger:** The enacted dual-feed pattern aligned with vanilla behavior. Player food interactions triggered the "in love" state, and no satiation gate was applied.
- **Offspring:** Breeding triggered a mating sequence followed by one parent's leaf-canopy nest-building goal.

### 1.3. AI & Behavior Systems

#### Scavenging
- **Mechanism:** Proximity-based acquisition added a dropped item to `CARRIED_ITEM` when the crow moved within the configured 0.5-to-1.0-block range.
- **Drop Logic:** Feeding or combat/distress could trigger a carried-item drop; the item entity was always created at the crow's position.
- **Priority:** The search applied the following order:
    1. `minecraft:beacon_payment_items`
    2. `minecraft:piglin_loved`
    3. `minecraft:trim_materials`
    4. The nearest item served as the fallback.
- **Frequency:** Search frequency was inversely proportional to satiation.

#### Swarm Intelligence (The "Distress" System)
- **Trigger (Untamed):** Attacking an untamed crow triggered a 32-block-radius distress event with the source and up to five nearby responders.
- **Trigger (Tamed):**
    - **Single Hit:** A single hit produced an approximately two-second retaliation engagement.
    - **Escalation:** Three hits within a 30-second sliding window triggered full swarm escalation.
    - **Defending Player:** A swarm response was triggered only when the owner attacked a hostile mob.
- **Swarm Rules:**
    - **Cap:** The response was capped at six crows, selected as the source plus five nearest crows through squared-distance sorting.
    - **Networking:** `DistressPayload` broadcast the entity ID, position, and source from server to client.
    - **Cooldown:** Each crow received a 300-tick cooldown between swarm participations.
    - **Targeting:** Player targets used a four-second sliding window, and each swarm engagement was capped at 200 ticks.
    - **Navigation:** Combat used standard ground-based `PathNavigation`.

#### Neutral Sitting
- **Toggle:** Any non-food owner right-click toggled the inherited ordered-to-sit state.
- **Behavior:** Sitting cleared navigation, airborne motion, vanilla and swarm targets, and per-crow retaliation state. Standing did not restore an old target.
- **Swarm isolation:** Sitting crows neither initiated nor joined swarm responses.

### 1.4. Networking
- **Payloads:**
    - `DistressPayload` broadcast distress events with entity ID, block position, and source ID.
    - `ScavengePayload` synchronized item acquisition and drops with crow ID and item stack.
- **Pattern:** The implementation used the MC 26.2 `CustomPacketPayload` and `PayloadTypeRegistry` APIs.

## 2. PAWS Verification

| Pillar | Phase 3 rule |
|---|---|
| Performance | Proximity and swarm selection used squared distances; responder count, engagement duration, and cooldown work were bounded. |
| Auditability | Synced state, persistent state, typed payloads, and per-crow swarm bookkeeping provided traceable behavior boundaries. |
| Workability | Sitting cleared active aggression, baby crows were excluded from combat, and server-authoritative handlers controlled gameplay changes. |
| Scalability | Behavior policy, AI goals, swarm coordination, events, and networking were separated into focused modules. |

Verification covered pure behavior-policy tests, state-transition tests, payload registration, and a clean Gradle build.
