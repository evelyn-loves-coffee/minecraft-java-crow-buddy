# Design Decisions & Open Questions

## Assumptions
| # | Assumption | Risk | Verification Needed |
|---|-----------|------|---------------------|
| 1 | GeckoLib artifact coordinate is `software.bernie.geckolib:geckolib-fabric-${minecraft_version}:${geckolib_version}` | Low | Confirmed for 5.5.3 |
| 2 | `ModItems`/`ModEntities` registration pattern is correct for MC 26.2 | Low | Confirmed: Use Fabric `EventRegistry` |
| 3 | GeckoLib should use `modApi` rather than `modImplementation` | Low | Confirmed: Use `modApi` |
| 4 | Loom mod name `"modid"` $\to$ `"crowbuddy"` change is necessary | Low | May be purely internal to Loom with no runtime effect |
| 5 | DataGen uses simplified `FabricDataPack` interface | Low | Confirmed: `createPack()` called with no argument |
| 6 | Networking uses `ServerPlayNetworking` from Fabric API | Low | Confirmed: Standard channel registration |

## Open Questions
| # | Question | Impact | Decision Needed |
|---|----------|--------|-----------------|

## Resolved Decisions
| # | Decision | Detail |
|---|----------|--------|
| 1 | **GeckoLib Integration** | Add as a prerequisite dependency immediately. |
| 2 | **Registration API** | Use the Minecraft 26.2 modern registration patterns. |
| 3 | **Dependency Scope** | Use `modImplementation` (internal to mod). |
| 4 | **DataGen Scope** | Implement full providers (Items, Entities, Tags) from start. |
| 5 | **Asset Readiness** | Implement minimal structure (lang files, dummy textures) in Phase 1. |
| 6 | **Mixin Strategy** | Use a `CrowBuddyMixin` as a placeholder; implement via Dispatcher Pattern in Phase 3 to maintain modularity/testability. |

