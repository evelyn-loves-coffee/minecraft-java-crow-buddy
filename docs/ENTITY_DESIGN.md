# Crow Buddy: Technical Analysis & Design

## 1. Entity: The Crow

### Core Concept
A vanilla-plus, tameable entity that mirrors real-life crow presence through biome-specific spawning. It shares some behaviors with Parrots (flight, shoulder-sitting) but introduces unique crow-centric mechanics.

### Implementation Strategy
* **Approach:** "From Scratch" (Approach B) via `TamableAnimal` (MC 26.x) to allow custom flight physics, unique AI goals, and optimized performance while inheriting vanilla taming/ownership mechanics.
* **EntityData Tracking (synced, 5 custom + 1 inherited):**
    * `SITTING` (inherited from `TamableAnimal` via `setOrderedToSit()`/`isOrderedToSit()`): Sit command suppresses all behaviors.
    * `PERCHED` (boolean): Shoulder-perch toggle; when perched, all goals disabled, crow follows owner.
    * `STATE` (int): Maps to `CrowState` enum (`IDLE`, `SEARCHING`, `CARRYING`, `COMBAT`, `DISTRESS`, `SWARM`).
    * `CARRIED_ITEM` (ItemStack): Mouth-held item for scavenging.
    * `SATIATION` (float): Tracks health/food levels, clamped 0.0–1.0.
    * `RELATIONSHIP` (float): Tracks player aggression.
* **Taming API (inherited from `TamableAnimal`):**
    * `isTame()` / `setTame(boolean)` - tame state
    * `setOwner(LivingEntity)` / `getOwner() / `getOwnerReference()` - owner tracking
    * `isOwnedBy(LivingEntity)` - owner identity check
    * `wantsToAttack(LivingEntity attacker, LivingEntity target)` - owner vs non-owner differentiation
    * `tryToTeleportToOwner()` - teleport recall for distant owner
    * `canAttack(LivingEntity)` - attack permission
* **NBT Persistence:** Replaced with EntityData (MC 26.2 replaced NBT I/O with `ValueInput`/`ValueOutput`; all accessors auto-persist via `SynchedEntityData`).

### Behavior & AI
* **Tamability:** 
    * **Taming Item:** Black oil sunflower seeds (crafted from Sunflowers).
    * **Healing/Satiation:** Uses new item `crowbuddy:black_oil_sunflower_seeds` and `minecraft:parrot_food` tags.
    * **Poison:** `minecraft:parrot_poisonous_food` and `minecraft:cocoa_beans`.
* **Item Retrieval (Scavenging):**
    * **Mechanic:** Proximity-based acquisition. If the crow is within 0.5 to 1.0 blocks of a dropped item, the item is immediately added to its mouth.
    * **Drop Logic:** Items are dropped at the player's location as a trade if the player feeds them, or dropped at the crow's current location if the crow enters combat or a distress event.
    * **Priority (Weighted):** 
        1. `minecraft:beacon_payment_items`
        2. `minecraft:piglin_loved`
        3. `minecraft:trim_materials`
    * **Frequency:** Based on health/satiation:
        * *Low Health:* No searching.
        * *Mid Health:* Infrequent searching.
        * *Full Health:* Searching with a ~12-second cooldown.
* **Swarm Intelligence (The "Distress" System):**
    * **Trigger (Untamed):** Attacking a crow triggers a 32-block radius distress event (6 total: source + 5 nearest).
    * **Trigger (Tamed/Hostile):** 
        * *Single Hit onto the tamed crow from player:* Full `TamableAnimal` retaliation (~2s engagement via `wantsToAttack()`/`canAttack()`).
        * *3 Total Hits onto the tamed crow within 30s sliding window:* Escalates to full swarm.
        * *Defending Player:* Tamed crow triggers swarm only when owner attacks a **hostile mob** (not players, not neutrals).
    * **Swarm Rules:**
        * **Distance Check:** Uses `distanceSquared ≤ 1024` (32 blocks) for proximity checks.
        * **Cap:** 6 total (source always participates + up to 5 nearest responders, sorted by `distanceSquared`).
        * **Networking:** `DistressPayload` (S→C) broadcasts entity ID + BlockPos + source ID via `ServerPlayNetworking`.
        * **Single Emission:** Only the original source emits distress; responders do not relay.
        * **Cooldown:** Per-crow 15-second (300-tick) participation cooldown prevents cascading spam.
        * **Audio:** All 6 participating crows play distress sound with dual-layer variance (code: `pitch = rand(0.85–1.15)`, `volume = rand(0.9–1.1)`; JSON baseline: `"pitch": {"min": 0.85, "max": 1.15}`). Repeated every 20 ticks.
        * **Targeting (Hostile Mobs):** Attack indefinitely until target dies.
        * **Targeting (Players):** Attack for a 4-second sliding window (resets on each successful hit).
        * **Cross-Dimension:** Swarm is restricted to the same dimension as the source.
        * **Control:** "Sit" command (`isOrderedToSit()`) cancels all active swarm behavior immediately.
    * **Sitting:** "Sit" command suppresses all active behaviors, including search, flight, combat, and distress.
    * **Distress Detection:** Triggered via `ServerLivingEntityEvents.AFTER_DAMAGE` (Fabric event API) — fires before entity death, no custom mixin required.
    * **Navigation:** Standard `PathNavigation` pathfinding for Phase 3. Ground-based; 3D flight + `FlyingPathNavigation` deferred to Phase 5.
* **Shoulder-Perch Toggle (Resolved Decision D):**
    * **Toggle:** Right-click on tamed crow toggles `PERCHED` state (synced EntityData boolean).
    * **Perched:** All AI goals (`ScavengeGoal`, `SwarmDistressGoal`, etc.) disabled. Crow follows owner everywhere, rendered at shoulder position.
    * **Unperched:** Full AI autonomy restored. If owner exceeds recall range, crow flies toward owner until within range or perched again.
    * **State Hierarchy:** `SITTING` overrides `PERCHED`. When sitting, perch state is preserved and restored on stand.

---

## 2. World & Environment

### Spawning
* **Strategy:** Procedural analysis of temperature, humidity, and forest density, followed by a performance-optimized biome whitelist.
* **Implementation:** Uses `BiomeModifications` to ensure high compatibility with other world-gen mods.

### Crow Nests
* **Type:** Procedural **Features** (for high mod compatibility).
* **Function:** Decorative "home base" that allows up to two adult crows and one baby crow to perch.
* **Compatibility:** Uses Block Tags to ensure attachment to any "tree-like" blocks (e.g., Biomes O' Plenty, Terralith).

---

## 3. Technical Standards
* **Development Standard:** PAWS (Performance, Auditability, Workability, Scalability).
* **Entity Standards:** Adheres to "Tiny Takeover" (Minecraft 26.1+) overhaul patterns.
