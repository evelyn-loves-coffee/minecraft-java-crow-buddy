# Crow Buddy: Technical Analysis & Design

## 1. Entity: The Crow

### Core Concept
A vanilla-plus, tameable entity that mirrors real-life crow presence through biome-specific spawning. It shares some behaviors with Parrots (flight, shoulder-sitting) but introduces unique crow-centric mechanics.

### Implementation Strategy
* **Approach:** "From Scratch" (Approach B) to allow for custom flight physics, unique AI goals, and optimized performance.
* **EntityData Tracking (synced):**
    * `SITTING` (boolean): Sit command suppresses all behaviors.
    * `PERCHED` (boolean): Shoulder-perch toggle; when perched, all goals disabled, crow follows owner.
    * `STATE` (int): Maps to `CrowState` enum (`IDLE`, `SEARCHING`, `CARRYING`, `COMBAT`, `DISTRESS`).
    * `CARRIED_ITEM` (ItemStack): Mouth-held item for scavenging.
* **NBT Persistence:**
    * `Crow_Satiation`: Tracks health/food levels to determine behavior frequency.
    * `Crow_Relationship`: Tracks player aggression.

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
    * **Trigger (Untamed):** Attacking a crow triggers a 32-block radius distress event (max 8 crows).
    * **Trigger (Tamed/Hostile):** 
        * *Single Hit onto the tamed crow from player:* Crow hits back once on player.
        * *Repeated Hits onto the tamed crow from player (30s window):* Triggers full swarm.
        * *Defending Player:* Tamed crow can trigger a swarm on a target (mob or player) the player attacks.
    * **Swarm Rules:**
        * **Distance Check:** Uses `distanceSquared` for high-performance proximity checks during distress events.
        * **Networking:** Custom `ServerPlayNetworking` packets to broadcast the `Entity ID` and `BlockPos` of the distress source.
        * **Communication:** Only the original source emits the distress event; others react but do not relay.
        * **Sound:** Screeching continues for the duration of the event.
        * **Targeting (Hostile Mobs):** Attack indefinitely.
        * **Targeting (Players):** Attack for a 4-second sliding window.
        * **Control:** Players can command the crow to "sit" to cancel a distress event.
    * **Sitting:** "Sit" command suppresses all active behaviors, including search, flight, combat, and distress.
    * **Distress Detection:** Triggered via `ServerLivingEntityEvents.AFTER_DAMAGE` (Fabric event API) — no custom mixin required.
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
