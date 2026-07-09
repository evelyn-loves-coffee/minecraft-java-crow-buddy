# Crow Buddy: Technical Analysis & Design

## 1. Entity: The Crow

### Core Concept
A vanilla-plus, tameable entity that mirrors real-life crow presence through biome-specific spawning. It shares some behaviors with Parrots (flight, shoulder-sitting) but introduces unique crow-centric mechanics.

### Implementation Strategy
* **Approach:** "From Scratch" (Approach B) to allow for custom flight physics, unique AI goals, and optimized performance.
* **NBT Tracking:**
    * `Crow_Satiation`: Tracks health/food levels to determine behavior frequency.
    * `Crow_Relationship`: Tracks player aggression (noted as a temporary novelty).

### Behavior & AI
* **Tamability:** 
    * **Taming Item:** Black oil sunflower seeds (crafted from Sunflowers).
    * **Healing/Satiation:** Uses `minecraft:parrot_food` tags.
    * **Poison:** `minecraft:parrot_poisonous_food` and `minecraft:cocoa_beans`.
* **Item Retrieval (Scavenging):**
    * **Mechanic:** Crows grab dropped items and carry them in their mouths.
    * **Drop Logic:** Items are dropped at the player's location if the crow enters combat or a distress event.
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
        * *Single Hit:* Crow hits back once.
        * *Repeated Hits (30s window):* Triggers full swarm.
        * *Defending Player:* Tamed crow can trigger a swarm on a target (mob or player) the player attacks.
    * **Swarm Rules:**
        * Only the original source emits the distress event; others react but do not relay.
        * **Sound:** Screeching continues for the duration of the event.
        * **Targeting (Hostile Mobs):** Attack indefinitely.
        * **Targeting (Players):** Attack for a 4-second sliding window.
        * **Control:** Players can command the crow to "sit" to cancel a distress event.
* **Sitting:** "Sit" command suppresses all active behaviors, including search, flight, combat, and distress.

---

## 2. World & Environment

### Spawning
* **Strategy:** Procedural analysis of temperature, humidity, and forest density, followed by a performance-optimized biome whitelist.

### Crow Nests
* **Type:** Procedural **Features** (for high mod compatibility).
* **Function:** Decorative "home base" that allows up to two adult crows and one baby crow to perch.
* **Compatibility:** Uses Block Tags to ensure attachment to any "tree-like" blocks (e.g., Biomes O' Plenty, Terralith).

---

## 3. Technical Standards
* **Development Standard:** PAWS (Performance, Auditability, Workability, Scalability).
* **Entity Standards:** Adheres to "Tiny Takeover" (Minecraft 26.1+) overhaul patterns.
