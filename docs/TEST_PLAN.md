# Crow Buddy: Validation & Testing Plan

This document outlines the verification steps required to ensure compliance with the PAWS (Performance, Auditability, Workability, Scalability) standard.

## 1. Functional Verification
* [ ] **Entity Behavior:** Verify scavenging (item acquisition), sitting, and flight mechanics.
* [ ] **Tamability:** Verify seed usage, healing, and poisoning logic.
* [ ] **Swarm System:** Verify distress trigger, radius check, and networking sync.
* [ ] **World Gen:** Verify procedural nest and biome-specific spawning.

## 2. Technical Standards (PAWS)
* **Performance:**
    * Verify use of `distanceSquared` for all proximity checks.
    * Profile AI cycles to ensure no significant impact on TPS.
* **Auditability:**
    * Ensure all critical state changes are logged in dev environment.
    * Verify error handling for networking packets.
* **Workability:**
    * Test edge cases (e.g., entity dies during distress, owner sits during combat).
    * Verify idempotency of network packets.
* **Scalability:**
    * Ensure modularity of DataGen providers.
    * Verify compatibility with common world-gen mods via Block Tags.

## 3. Regression & Build
* [x] Full `./gradlew build` (no failures; verified 2026-07-21).
* [x] Java compilation with deprecation linting (verified 2026-07-21; two upstream Fabric renderer-registry warnings remain).
* [ ] Verify all assets (`.geo.json`, `.animation.json`, `.png`, `.ogg`) are present in resources.
