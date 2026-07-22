# Crow Buddy: Validation & Testing Plan

This document outlines the verification steps required to ensure compliance with the PAWS (Performance, Auditability, Workability, Scalability) standard.

## 1. Functional Verification
* [ ] **Entity Behavior:** Verify scavenging, neutral sit/stand target clearing, and flight mechanics.
* [ ] **Tamability:** Verify seed usage, healing, and poisoning logic.
* [ ] **Swarm System:** Verify distress trigger, radius check, and networking sync.
* [ ] **Spawning:** Verify biome-specific crow spawning and absence of generated nests.
* [ ] **Nest Building:** Verify one parent builds on exposed `#minecraft:leaves` within 16 blocks; invalid or enclosed sites are rejected.
* [ ] **Nest Lifecycle:** Verify incubation, one baby spawn, hatch removal, leaf-support removal, and no item drops.
* [ ] **Trampling:** Verify turtle-egg probabilities, sneaking/crow immunity, and `mobGriefing` behavior.

## 2. Technical Standards (PAWS)
* **Performance:**
    * Verify use of `distanceSquared` for all proximity checks.
    * Profile the bounded `O(r²)` canopy search during simultaneous breeding.
* **Auditability:**
    * Ensure all critical state changes are logged in dev environment.
    * Verify error handling for networking packets.
* **Workability:**
    * Test edge cases (e.g., entity dies during distress, owner sits during combat).
    * Verify idempotency of network packets.
* **Scalability:**
    * Ensure modularity of DataGen providers.
    * Verify modded leaves participate through `#minecraft:leaves`.

## 3. Regression & Build
* [x] Full `./gradlew clean build --warning-mode all` (29 tests, no failures; verified 2026-07-22).
* [x] Java compilation with deprecation linting (verified 2026-07-22; two upstream Fabric renderer-registry warnings remain).
* [x] Required `.geo.json`, `.animation.json`, `.png`, and `.ogg` resources are present; custom `.ogg` files remain placeholders pending final audio selection.
