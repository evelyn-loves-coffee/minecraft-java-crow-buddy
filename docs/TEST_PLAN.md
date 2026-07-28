# Crow Buddy: Validation & Testing Plan

This document outlines the verification steps required to ensure compliance with the PAWS (Performance, Auditability, Workability, Scalability) standard.

## 1. Functional Verification
* [x] **Entity Behavior:** Scavenging, neutral sit/stand target clearing, and flight mechanics verified.
* [x] **Tamability:** Seed usage, healing, and poisoning logic verified.
* [x] **Swarm System:** Distress trigger, radius check, and networking sync verified.
* [x] **Spawning:** Crow spawning in non-ocean, non-river, non-desert, non-underground overworld biomes (weight 5, groups 1-2) verified; no generated nests.
* [x] **Nest Building:** One parent builds on exposed `#minecraft:leaves` within 48 blocks; invalid or enclosed sites are rejected.
* [x] **Nest Lifecycle:** Incubation, one baby spawn, hatch removal, leaf-support removal, and no item drops verified.
* [x] **Trampling:** Turtle-egg probabilities, sneaking/crow immunity, and `mobGriefing` behavior verified.

## 2. Technical Standards (PAWS)
* **Performance:**
    * [x] Use of `distanceSquared` for all proximity checks verified.
    * [x] Bounded `O(r²)` canopy search (radius 48, max 10,125 columns) profiled during simultaneous breeding.
* **Auditability:**
    * [x] Critical state changes logged in dev environment.
    * [x] Error handling for networking packets verified.
* **Workability:**
    * [ ] Test edge cases (e.g., entity dies during distress, owner sits during combat).
    * [x] Idempotency of network packets verified.
* **Scalability:**
    * [x] Modularity of DataGen providers verified.
    * [x] Modded leaves participate through `#minecraft:leaves` verified.

## 3. Regression & Build
* [x] Full `./gradlew clean build --warning-mode all` (38 tests, no failures; verified 2026-07-26).
* [x] Java compilation with deprecation linting (verified 2026-07-26; two upstream Fabric renderer-registry warnings remain).
* [x] Required `.geo.json`, `.animation.json`, `.png`, and `.ogg` resources are present; custom `.ogg` files remain placeholders pending final audio selection.
