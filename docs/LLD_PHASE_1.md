# Crow Buddy: Phase 1 Low-Level Design (LLD)

## 1. Build and Toolchain

- The project used the checked-in Gradle Wrapper, which pinned Gradle 9.6.1 and provided `gradlew` and `gradlew.bat` entry points.
- Fabric Loom supplied Minecraft development, remapping, run configurations, and source/JAR packaging.
- Java source and target compatibility were pinned to Java 25. Compilation enabled deprecation linting.
- JUnit Jupiter provided pure unit testing through Gradle's `test` task.

Version coordinates were centralized in `gradle.properties`:

| Component | Version |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric Loom | 1.17-SNAPSHOT |
| Fabric API | 0.155.2+26.2 |
| GeckoLib | Modrinth artifact `L6bn4TS8` |
| Crow Buddy | 1.0.0 |

## 2. Dependency and Repository Layout

- Fabric plugins resolved through Fabric Maven, Maven Central, and the Gradle Plugin Portal.
- GeckoLib resolved from the restricted Modrinth Maven repository.
- Minecraft, Fabric Loader, Fabric API, and GeckoLib were added as implementation dependencies.
- Main and client code used Loom's split environment source sets:
  - `src/main` contained common/server-safe code and resources.
  - `src/client` contained renderers, models, client networking, and client-only resources.
- The `crowbuddy` Loom mod combined both source sets for development and packaging.

## 3. Metadata and Resources

- `fabric.mod.json` declared the common and client entry points, Minecraft/Java requirements, Fabric API, GeckoLib, and mixin configurations.
- `processResources` expanded the Gradle project version into `fabric.mod.json`.
- Common and client mixin JSON files were retained with empty mixin lists to preserve explicit environment boundaries.
- GeckoLib models and animations were stored below `assets/crowbuddy/geckolib/`; textures, language files, sounds, block states, and JSON models used the standard resource-pack layout.
- `withSourcesJar()` produced a source artifact alongside the remapped mod JAR.

## 4. Enacted Build Flow

The build was designed and enacted in the following order:

```text
gradlew build
  → compiled common Java
  → processed common resources
  → compiled client Java
  → processed client resources
  → compiled and ran unit tests
  → validated access wideners
  → created binary and source JARs
```

Generated state was written to `.gradle/`, `build/`, and `run/`; these directories were not treated as source inputs and were excluded from version control.

## 5. PAWS Verification

| Pillar | Phase 1 rule |
|---|---|
| Performance | Parallel Gradle execution was enabled; build output and caches were reusable and untracked. |
| Auditability | Dependency and tool versions were centralized; deprecation warnings remained visible. |
| Workability | The Wrapper made local and CI builds use the same Gradle distribution. |
| Scalability | Client-only code was isolated from common code through split source sets. |

The phase required `./gradlew clean build --warning-mode all` to complete without errors.
