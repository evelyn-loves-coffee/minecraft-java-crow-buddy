# Design Decisions & Open Questions

## Assumptions
| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| 1 | GeckoLib Fabric version uses Modrinth Maven coordinates `maven.modrinth:8BmcQJ2H:${geckolib_version}` (ID pinned in `gradle.properties`) | Low | Confirmed for 5.5.3 via `L6bn4TS8` |
| 2 | `ModItems`/`ModEntities` registration pattern is correct for MC 26.2 | Low | Confirmed: Use Fabric `EventRegistry` |
| 3 | Loom 1.17 `mods` block uses Gradle's `implementation` scope (not `modImplementation`/`modApi`) | Low | Confirmed during Phase 1 build |
| 4 | Loom mod name `"modid"` $\to$ `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| 5 | DataGen uses `FabricDataGenerator.Pack` via `fabricApi { configureDataGeneration() }` | Low | Confirmed: requires Fabric Maven (`net.fabricmc.fabric-api:fabric-api`), not Modrinth Maven |
| 6 | Networking uses `ServerPlayNetworking` from Fabric API | Low | Confirmed: Standard channel registration |
| 7 | GeckoLib `.geo.json` models use Bedrock Edition Behavior Pack `minecraft:geometry` format (`format_version: "1.12.0"`) | Low | Confirmed via Blockbench GeckoLib plugin export |
| 8 | GeckoLib `.animation.json` files use Bedrock Edition `animations` format (`format_version: "1.8.0"`) | Low | Confirmed via Blockbench GeckoLib plugin export |
| 9 | GeckoLib 5 assets use `geckolib/models/` and `geckolib/animations/` paths (not `geo/` and `animations/`) | Low | Confirmed via wiki.geckolib.com |

## Open Questions
| # | Question | Impact | Decision Needed |
|---|----------|--------|-----------------|

## Resolved Decisions
| # | Decision | Detail |
|---|----------|--------|
| 1 | **GeckoLib Integration** | Add as a prerequisite dependency via `implementation` (Loom 1.17). |
| 2 | **Registration API** | Use the Minecraft 26.2 modern registration patterns. |
| 3 | **Dependency Scope** | Use `implementation` — Loom 1.17 `mods` block replaces `modImplementation`. |
| 4 | **DataGen Scope** | Implement full providers (Items, Entities, Tags) from start. |
| 5 | **Asset Readiness** | Implement minimal structure (lang files, dummy textures) in Phase 1. |
| 6 | **Mixin Strategy** | Use a `CrowBuddyMixin` as a placeholder; implement via Dispatcher Pattern in Phase 3 to maintain modularity/testability. |
| 7 | **GeckoLib Format** | `.geo.json` and `.animation.json` confirmed via Blockbench/GeckoLib export (format_version 1.12.0/1.8.0). |
| 8 | **CrowGeoModel.java Reference** | A template `CrowGeoModel.java` exists at `/home/evelyn/Downloads/crow-model/geckolib5/` for Phase 2. Needs `yourmodule` → `crowbuddy` replacement and `CrowEntity` import before use. |
| 9 | **Fabric API Maven Source** | Use Fabric Maven (`net.fabricmc.fabric-api:fabric-api:${fabric_api_version}`) for DataGen to work. Modrinth Maven artifact omits `fabric-data-generation-api-v1` nested jar from compile classpath. |
