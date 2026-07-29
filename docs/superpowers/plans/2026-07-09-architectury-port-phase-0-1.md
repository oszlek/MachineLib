# MachineLib Architectury Port — Phase 0 + Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert MachineLib from a single Fabric project into an Architectury multiloader project (`common`/`fabric`/`neoforge`), move the loader-agnostic storage engine into `common`, sever the two Fabric leak points behind a platform SPI, and keep the **Fabric build fully green** (client/server run + unit tests + gametests) with zero behavior change.

**Architecture:** Approach A from the design spec (`docs/superpowers/specs/2026-07-09-architectury-port-design.md`). MachineLib's core is already resource-neutral with a simulate/commit split (`try*` vs commit + `set*` rollback). Phase 0 stands up the module skeleton on both loaders. Phase 1 relocates neutral code to `common`, keeps every Fabric-specific type in the `fabric` sourceset (still using existing Fabric APIs — the Architectury rewrite of networking/menus is Phase 2), and replaces the two leak points (`MachineEnergyStorage extends EnergyStorage`, `ResourceStorage.getExposedStorage`) with a registration SPI whose only implementation, for now, is Fabric.

**Tech Stack:** Minecraft 1.21.1, Java 21, Gradle (Kotlin DSL), architectury-loom + architectury-plugin, Architectury API 13.x, NeoForge 21.1.x, Fabric Loader/API (unchanged), team-reborn energy (Fabric only), JUnit + Fabric gametest.

**Scope note:** Phases 2–5 (Architectury networking/menus/config; NeoForge core; cross-platform verification; integrations) each get their own plan after Phase 1 lands, because their exact steps depend on NeoForge API shapes best confirmed against a compiling `common`.

---

## File Structure (target after Phase 1)

```
settings.gradle.kts                 # include :common :fabric :neoforge
build.gradle.kts                    # root: plugins(apply false) + subprojects{}
gradle.properties                   # + architectury/neoforge/enabled_platforms props
common/build.gradle.kts
common/src/main/java/dev/galacticraft/machinelib/**      # neutral api + impl core
common/src/main/java/dev/galacticraft/machinelib/impl/platform/**   # NEW: platform SPI
common/src/main/resources/**        # assets, data, lang, textures (shared)
fabric/build.gradle.kts
fabric/src/main/java/dev/galacticraft/machinelib/fabric/**   # entrypoint, adapters, SPI impl, networking, client, model
fabric/src/main/resources/fabric.mod.json
fabric/src/testmod/**  fabric/src/test/**   # moved from root src/testmod + src/test
neoforge/build.gradle.kts
neoforge/src/main/java/dev/galacticraft/machinelib/neoforge/**   # empty entrypoint only (Phase 0)
neoforge/src/main/resources/META-INF/neoforge.mods.toml
```

**Package convention:** neutral code keeps `dev.galacticraft.machinelib.*`. Platform code goes under `dev.galacticraft.machinelib.fabric.*` / `.neoforge.*`.

---

## PHASE 0 — Toolchain & module skeleton

Outcome: `./gradlew build` produces a Fabric jar and a NeoForge jar, each loading an (almost) empty mod. No MachineLib code has moved yet — the existing `src/` is temporarily still built by a shim so nothing breaks mid-restructure. We do the real code move in Phase 1.

### Task 0.1: Confirm current Architectury toolchain versions

**Files:** none (research)

- [ ] **Step 1: Fetch the known-good version matrix for MC 1.21.1**

Open the Architectury example/template for the `1.21` branch and record the exact values:
- `architectury-plugin` Gradle plugin version (expected `3.4-SNAPSHOT`)
- `dev.architectury.loom` version (expected `1.7-SNAPSHOT`; may be higher — use whatever the template pins)
- `architectury` API version for 1.21.1 (spec assumes `13.0.6`)
- `neoforge` version (latest stable `21.1.x`)

Sources to check (in order): `https://github.com/architectury/architectury-templates` (branch `1.21`), the Architectury docs "Getting Started", and `https://projects.neoforged.net/neoforged/neoforge` for the current 21.1.x build.

Expected: a concrete value for each of the four. If any differ from the assumptions above, use the fetched value throughout Phase 0.

- [ ] **Step 2: Record the values in `gradle.properties` (done in Task 0.3)**

### Task 0.2: Add subproject settings

**Files:**
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Add the architectury-loom plugin repos and include the modules**

Replace the `pluginManagement { repositories { … } }` block's repository list to add the Architectury and NeoForged plugin repos, and add module includes at the bottom:

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.architectury.dev/") { name = "Architectury" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://repo.terradevelopment.net/repository/maven-releases/") {
            content {
                includeGroup("dev.galacticraft")
                includeGroup("dev.galacticraft.mojarn")
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "MachineLib"
include("common")
include("fabric")
include("neoforge")
```

- [ ] **Step 2: Verify settings evaluate**

Run: `./gradlew projects`
Expected: lists root project `MachineLib` with subprojects `:common`, `:fabric`, `:neoforge`.

### Task 0.3: Add multiloader properties

**Files:**
- Modify: `gradle.properties`

- [ ] **Step 1: Append the new properties (use values confirmed in Task 0.1)**

```properties
# Architectury / multiloader
enabled_platforms=fabric,neoforge
architectury.plugin.version=3.4-SNAPSHOT
architectury.loom.version=1.7-SNAPSHOT
neoforge.version=21.1.169
```

(Keep all existing properties. `architectury.version=13.0.6` already present.)

- [ ] **Step 2: Verify**

Run: `./gradlew help`
Expected: BUILD SUCCESSFUL (properties parse).

### Task 0.4: Root build script

**Files:**
- Modify: `build.gradle.kts` (root)

- [ ] **Step 1: Replace the root build script with a multiloader root**

The root becomes plugins-applied-false + shared `subprojects {}` config. Move the current Fabric-specific `loom {}`, `dependencies {}`, and `repositories {}` blocks OUT of root — they belong to per-module scripts (Tasks 0.5–0.7). Root keeps: group/version computation, Java 21 toolchain, spotless, and the manifest/publish config applied to subprojects.

```kotlin
plugins {
    java
    `maven-publish`
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
    id("com.diffplug.spotless") version "7.0.3"
    id("org.ajoberstar.grgit") version "5.3.0"
}

architectury {
    minecraft = project.property("minecraft.version").toString()
}

allprojects {
    group = "dev.galacticraft"
    version = /* keep the existing modVersion + env/grgit build-suffix logic */ ""
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "com.diffplug.spotless")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withSourcesJar()
    }
    tasks.withType<JavaCompile> { options.encoding = "UTF-8"; options.release.set(21) }

    // keep spotless licenseHeader/formatting from the current root script
    // keep the Jar manifest attributes block from the current root script
}
```

Preserve verbatim (relocated into `allprojects`/`subprojects`): the `version = buildString { … }` block, `processLicenseHeader`, the `spotless { java { … } }` config, and the `tasks.withType<Jar> { manifest { … } }` block. `withJavadocJar()`/`javadoc` config moves to the `common` script (Task 0.5) since that owns the API.

- [ ] **Step 2: Verify root evaluates (subprojects still empty — expect failure only from missing module scripts, added next)**

Run: `./gradlew help`
Expected: BUILD SUCCESSFUL after Tasks 0.5–0.7 add the module scripts. (If run now, fails resolving module scripts — that is expected; proceed.)

### Task 0.5: Common module build script

**Files:**
- Create: `common/build.gradle.kts`

- [ ] **Step 1: Write the common script**

```kotlin
architectury { common(project.property("enabled_platforms").toString().split(",")) }

loom { silentMojangMappingsLicense() }

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft.version")}")
    mappings(loom.officialMojangMappings())   // see Task 0.9 re: mappings decision
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader.version")}")
    modApi("dev.architectury:architectury:${project.property("architectury.version")}")
}
```

- [ ] **Step 2: Create an empty source root so the module is valid**

Create `common/src/main/resources/pack.mcmeta` with a valid 1.21.1 resource pack descriptor (pack_format for 1.21.1 = 34):

```json
{ "pack": { "description": "MachineLib", "pack_format": 34 } }
```

- [ ] **Step 3: Verify**

Run: `./gradlew :common:build`
Expected: BUILD SUCCESSFUL, produces `common/build/libs/*.jar`.

### Task 0.6: Fabric module build script

**Files:**
- Create: `fabric/build.gradle.kts`

- [ ] **Step 1: Write the fabric script**

Port the current root `dependencies {}` (all `modImplementation` fabric-api modules, `teamreborn:energy`, badpackets, wthit, cloth, modmenu, rei/jei/emi) and `repositories {}` and `loom { runs {} , mods {} }` into this script. Add the Architectury wiring:

```kotlin
plugins { id("com.github.johnrengelman.shadow") version "8.1.1" }  // if bundling; else omit

architectury { fabric() }

loom { /* keep the existing runs{server,client,gametest,data} and mods{} block */ }

val common: Configuration by configurations.creating
configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    getByName("developmentFabric").extendsFrom(common)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft.version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader.version")}")
    modApi("dev.architectury:architectury-fabric:${project.property("architectury.version")}")
    // … all existing fabric-api / teamreborn / optional deps from the old root script …
    common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    "developmentFabric"(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    // include(modApi("teamreborn:energy:…")) etc as before
}

tasks.processResources { /* keep the fabric.mod.json expand + json-minify logic from old root */ }
```

- [ ] **Step 2: Verify (skeleton resources added in Task 0.8)**

Run: `./gradlew :fabric:build` — expected BUILD SUCCESSFUL after Task 0.8.

### Task 0.7: NeoForge module build script

**Files:**
- Create: `neoforge/build.gradle.kts`

- [ ] **Step 1: Write the neoforge script**

```kotlin
architectury { neoForge() }

val common: Configuration by configurations.creating
configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    getByName("developmentNeoForge").extendsFrom(common)
}

repositories { maven("https://maven.neoforged.net/releases/") }

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft.version")}")
    mappings(loom.officialMojangMappings())
    neoForge("net.neoforged:neoforge:${project.property("neoforge.version")}")
    modApi("dev.architectury:architectury-neoforge:${project.property("architectury.version")}")
    common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
    "developmentNeoForge"(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
}
```

Add `loom { runs { register("server"){server()}; register("client"){client()} } }`.

- [ ] **Step 2: Verify (metadata added in Task 0.8)**

Run: `./gradlew :neoforge:build` — expected BUILD SUCCESSFUL after Task 0.8.

### Task 0.8: Platform mod metadata + entrypoints

**Files:**
- Create: `fabric/src/main/resources/fabric.mod.json` (slimmed — only fabric-loader + fabric-api + architectury depends; entrypoints kept)
- Create: `fabric/src/main/java/dev/galacticraft/machinelib/fabric/MachineLibFabric.java`
- Create: `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `neoforge/src/main/java/dev/galacticraft/machinelib/neoforge/MachineLibNeoForge.java`

- [ ] **Step 1: Fabric entrypoint (temporary no-op that will delegate to common in Phase 1)**

```java
package dev.galacticraft.machinelib.fabric;

import net.fabricmc.api.ModInitializer;

public final class MachineLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Phase 1: delegate to common MachineLib.init() + register Fabric platform SPI impl
    }
}
```

Point `fabric.mod.json` `entrypoints.main` at `dev.galacticraft.machinelib.fabric.MachineLibFabric`.

- [ ] **Step 2: NeoForge entrypoint**

```java
package dev.galacticraft.machinelib.neoforge;

import net.neoforged.fml.common.Mod;

@Mod("machinelib")
public final class MachineLibNeoForge {
    public MachineLibNeoForge() {
        // Phase 3: register capabilities + delegate to common MachineLib.init()
    }
}
```

- [ ] **Step 3: `neoforge.mods.toml`** — minimal descriptor: modId `machinelib`, name/version from `${…}` (wire process-resources expansion in the neoforge script mirroring the fabric one), dependency on `neoforge` and `minecraft` `>=21.1`/`>=1.21`, and `architectury`.

- [ ] **Step 4: Verify both platforms build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL; `fabric/build/libs/*.jar` and `neoforge/build/libs/*.jar` exist.

- [ ] **Step 5: Verify both platforms launch (empty mod)**

Run: `./gradlew :fabric:runClient` then `./gradlew :neoforge:runClient` (close each after the title screen loads).
Expected: game reaches the main menu with MachineLib listed in the mod list, no crash.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties common fabric neoforge
git commit -m "build: architectury multiloader skeleton (common/fabric/neoforge)"
```

### Task 0.9: Mappings decision checkpoint

**Files:** none (decision, then apply)

- [ ] **Step 1: Choose mappings**

The current project uses **Yarn (via mojarn)**. Architectury multiloader is far simpler with **official Mojang mappings** (both loaders, no remap friction). Since this is already a breaking port, switch `common`/`fabric`/`neoforge` to `loom.officialMojangMappings()`.

Consequence: MC type/member names change from Yarn to Mojmap across the codebase. This is a large mechanical rename that must happen as the **first move step of Phase 1** (the IDE/`loom` remap does most of it; the rest is compiler-guided). Record this so Phase 1 Task 1.0 accounts for it.

Expected: decision recorded; no separate mapping shim maintained.

---

## PHASE 1 — Move neutral core to `common`, keep Fabric green

Outcome: `common` compiles with **zero** `net.fabricmc.*` / `team.reborn.*` imports; all Fabric-specific code lives in `fabric`; the two leak points are replaced by a platform SPI implemented only for Fabric; **Fabric unit tests + gametests pass unchanged**.

Work in the compiler-guided loop: move a group → `./gradlew :common:compileJava` → fix imports/leaks the compiler flags → repeat. Commit after each green group.

### Task 1.0: Remap to Mojang mappings

**Files:** all of `src/main`, `src/test`, `src/testmod`

- [ ] **Step 1** Apply `officialMojangMappings()` (from Task 0.9) and run loom's source remap (or migrate-mappings) to convert Yarn names to Mojmap across the existing `src/`.
- [ ] **Step 2** Run: `./gradlew :fabric:compileJava` (with all source still temporarily under the fabric module) — fix residual name mismatches until it compiles.
- [ ] **Step 3** Run: `./gradlew :fabric:test` — Expected: PASS (behavior unchanged, only names changed).
- [ ] **Step 4** Commit: `git commit -am "refactor: migrate Yarn -> Mojang mappings"`

### Task 1.1: Define the platform SPI in `common`

**Files:**
- Create: `common/src/main/java/dev/galacticraft/machinelib/impl/platform/MachineLibPlatform.java`
- Create: `common/src/main/java/dev/galacticraft/machinelib/impl/platform/StorageExposer.java`

- [ ] **Step 1: Write the SPI interfaces (no loader imports)**

```java
package dev.galacticraft.machinelib.impl.platform;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** Platform hook for exposing MachineLib storages as the loader's native capability. */
public interface StorageExposer {
    /** Register item/fluid/energy exposure for every machine of this block-entity type. */
    void registerMachine(BlockEntityType<?> type);
}
```

```java
package dev.galacticraft.machinelib.impl.platform;

/** Entry point resolved per loader via Architectury @ExpectPlatform. */
public final class MachineLibPlatform {
    // @dev.architectury.injectables.annotations.ExpectPlatform
    public static StorageExposer storageExposer() { throw new AssertionError(); }
}
```

- [ ] **Step 2** Run: `./gradlew :common:compileJava` — Expected: PASS (once module has sources; may need Task 1.3 group present). Commit when the group compiles.

### Task 1.2: Sever leak point #1 — energy interface

**Files:**
- Modify: `common/.../api/storage/MachineEnergyStorage.java`
- Modify: `common/.../impl/storage/MachineEnergyStorageImpl.java`
- Modify: `common/.../impl/storage/EmptyMachineEnergyStorage.java`
- Create: `fabric/.../fabric/storage/ExposedEnergyStorageImpl.java` (moved + rewritten from `impl/storage/exposed/`)

- [ ] **Step 1: Make `MachineEnergyStorage` neutral**

Remove `extends EnergyStorage` and every Fabric-typed member. Delete the two transaction overloads and the Fabric-returning exposer:

Remove: `import team.reborn.energy.api.EnergyStorage;`, `import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;`, `import …transaction.TransactionContext;`
Change the declaration to:

```java
public interface MachineEnergyStorage extends Serializable<LongTag>, PacketSerializable<ByteBuf>, DeltaPacketSerializable<ByteBuf, long[]>, Modifiable {
```

Delete these members (they move to the Fabric adapter):
```java
long extract(long amount, TransactionContext transaction);
long insert(long amount, TransactionContext transaction);
void setEnergy(long amount, TransactionContext context);
EnergyStorage getExposedStorage(ResourceFlow flow);
```

Keep `getAmount()`/`getCapacity()` (declare them directly on this interface now that `EnergyStorage` is gone). Replace `StoragePreconditions.notNegative(x)` in `create`/`Spec` with a private helper:

```java
static long requireNonNegative(long v) { if (v < 0) throw new IllegalArgumentException("negative: " + v); return v; }
```

- [ ] **Step 2: Update `MachineEnergyStorageImpl` / `EmptyMachineEnergyStorage`** — drop the deleted overrides and the `team.reborn`/`fabric.transfer` imports; keep the neutral `insert(long)/extract(long)/tryInsert/tryExtract/setEnergy(long)/getAmount/getCapacity` methods (these already exist and are the simulate/commit surface).

- [ ] **Step 3: Fabric energy adapter** — recreate the exposed energy storage in `fabric` as a team-reborn `EnergyStorage` backed by a `SnapshotParticipant<Long>`:

```java
package dev.galacticraft.machinelib.fabric.storage;

import dev.galacticraft.machinelib.api.storage.MachineEnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

public final class ExposedEnergyStorageImpl extends SnapshotParticipant<Long> implements EnergyStorage {
    private final MachineEnergyStorage parent;
    private final long maxInsertion, maxExtraction;

    public ExposedEnergyStorageImpl(MachineEnergyStorage parent, long maxInsertion, long maxExtraction) {
        this.parent = parent; this.maxInsertion = maxInsertion; this.maxExtraction = maxExtraction;
    }

    @Override public boolean supportsInsertion() { return parent.isValid() && maxInsertion > 0; }
    @Override public boolean supportsExtraction() { return parent.isValid() && maxExtraction > 0; }

    @Override public long insert(long max, TransactionContext tx) {
        if (!supportsInsertion()) return 0;
        long moved = parent.tryInsert(Math.min(maxInsertion, max));
        if (moved > 0) { updateSnapshots(tx); parent.setEnergy(parent.getAmount() + moved); }
        return moved;
    }
    @Override public long extract(long max, TransactionContext tx) {
        if (!supportsExtraction()) return 0;
        long moved = parent.tryExtract(Math.min(maxExtraction, max));
        if (moved > 0) { updateSnapshots(tx); parent.setEnergy(parent.getAmount() - moved); }
        return moved;
    }
    @Override public long getAmount() { return parent.getAmount(); }
    @Override public long getCapacity() { return parent.getCapacity(); }

    @Override protected Long createSnapshot() { return parent.getAmount(); }
    @Override protected void readSnapshot(Long snapshot) { parent.setEnergy(snapshot); }
}
```

- [ ] **Step 4** Run: `./gradlew :common:compileJava` — Expected: PASS (no team.reborn/fabric imports remain in `MachineEnergyStorage`). Then `./gradlew :fabric:compileJava` — Expected: PASS.
- [ ] **Step 5** Commit: `git commit -am "refactor: sever team-reborn EnergyStorage from MachineEnergyStorage"`

### Task 1.3: Sever leak point #2 — exposed resource storage

**Files:**
- Modify: `common/.../api/storage/ResourceStorage.java` (remove `getExposedStorage`)
- Modify: `common/.../api/storage/MachineItemStorage.java`, `MachineFluidStorage.java` (drop Fabric-typed exposer signatures)
- Move+rewrite to `fabric/.../fabric/storage/`: `ExposedStorageImpl`, `ExposedItemSlotImpl`, `ExposedFluidSlotImpl`, `ExposedSlotImpl` (from `impl/compat/transfer/`)
- Move to `fabric`: `api/compat/transfer/ExposedStorage.java`, `ExposedSlot.java`, `ExposedEnergyStorage.java`

- [ ] **Step 1** Remove `getExposedStorage(ResourceFlow)` and its `Storage`/`TransferVariant` imports from `ResourceStorage` and the storage interfaces. Exposure is now registration-driven (Task 1.6), not a return value.
- [ ] **Step 2** Move the `Exposed*` interfaces + impls into the `fabric` sourceset under `dev.galacticraft.machinelib.fabric.storage`, keeping their existing Fabric `Storage<Variant>` logic. Rewrite them as `SnapshotParticipant`-based wrappers over the neutral slots (same pattern as Task 1.2: `try*` to size, commit via slot setters, rollback via `createSnapshot`/`readSnapshot` capturing slot amounts).
- [ ] **Step 3** Run: `./gradlew :common:compileJava` — Expected: PASS. `./gradlew :fabric:compileJava` — Expected: PASS.
- [ ] **Step 4** Commit: `git commit -am "refactor: move exposed Storage adapters to fabric sourceset"`

### Task 1.4: Neutralize `AdjacentBlockApiCache` + resource-filter/slot Variant leaks

**Files:**
- Modify: `common/.../api/util/AdjacentBlockApiCache.java`, `EnergySource.java`, `ItemSource.java`, `FluidSource.java`, `StorageHelper.java`
- Modify: `common/.../api/filter/ResourceFilters.java`
- Modify: `common/.../api/storage/slot/FluidResourceSlot.java`, `ItemResourceSlot.java` and impls
- Create: `common/.../impl/platform/BlockLookup.java` (`@ExpectPlatform` find-in-direction over an `{ITEM,FLUID,ENERGY}` key)
- Create: `fabric/.../fabric/platform/BlockLookupImpl.java`

- [ ] **Step 1** Replace `BlockApiLookup`/`BlockApiCache` in `AdjacentBlockApiCache` with a neutral capability key enum + `@ExpectPlatform` lookup returning MachineLib's neutral `ItemSource`/`FluidSource`/`EnergySource`. Provide the Fabric impl (`FluidStorage.SIDED` etc.) in `fabric`.
- [ ] **Step 2** In `ResourceFilters` and the slot classes, replace `FluidVariant`/`ItemVariant` parameters with the neutral resource + `DataComponentPatch` pair MachineLib already stores. (These variants are just `(resource, components)`; use the existing neutral fields.)
- [ ] **Step 3** Run: `./gradlew :common:compileJava` after each file group — fix flagged leaks — Expected: eventually PASS with no `net.fabricmc`/`team.reborn` imports in `common`.
- [ ] **Step 4** Commit per green group: `git commit -am "refactor: neutralize <group> for common"`

### Task 1.5: Relocate the remaining neutral core to `common`; keep platform code in `fabric`

**Files:** bulk move (git mv) of `src/main/java` and `src/main/resources` into `common` / `fabric` per the map below.

**Goes to `common`** (neutral — everything NOT in the fabric list below), including: `api/{block,component,config,data,filter,machine,menu(logic),misc,storage,transfer,util}`, `impl/{block,config,gametest,machine,menu(logic),storage,util}`, all `resources/{assets,data}`.

**Stays in / moves to `fabric`** (platform): `impl/MachineLib.java`→delegated by `MachineLibFabric`; `impl/network/**` (8 payloads + registrar — kept on Fabric networking for Phase 1); `impl/compat/{vanilla,transfer}`; `impl/menu/MenuDataImpl.java`, `TankImpl.java` (screen-handler extension bits); `client/**` (entrypoint, model loader, baked/unbaked model, screen, DisplayUtil/GraphicsUtil, ModMenuCompat); the Fabric `Exposed*`/`SnapshotParticipant` adapters (Tasks 1.2–1.3); the `@ExpectPlatform` impls.

- [ ] **Step 1** `git mv` neutral packages into `common/src/main/java/...`; move platform packages into `fabric/src/main/java/dev/galacticraft/machinelib/fabric/...` (adjust package declarations + imports).
- [ ] **Step 2** Move `src/main/resources/{assets,data,...}` → `common/src/main/resources/`; move `fabric.mod.json` → `fabric` (already created in Phase 0 — reconcile).
- [ ] **Step 3** Wire `MachineLibFabric.onInitialize()` to call the (moved) common `MachineLib.init()` and register the Fabric `StorageExposer`/`BlockLookup`/networking.
- [ ] **Step 4** Run: `./gradlew :common:compileJava` — Expected: PASS, zero loader imports. Verify with: `grep -rlE "net\.fabricmc|team\.reborn" common/src/main/java` returns nothing.
- [ ] **Step 5** Run: `./gradlew :fabric:compileJava` — Expected: PASS.
- [ ] **Step 6** Commit: `git commit -am "refactor: relocate neutral core to common, platform code to fabric"`

### Task 1.6: Registration-driven exposure wiring (Fabric)

**Files:**
- Create: `fabric/.../fabric/platform/StorageExposerImpl.java` (implements the common `StorageExposer`)
- Modify: `fabric/.../fabric/MachineLibFabric.java`

- [ ] **Step 1** Implement `StorageExposerImpl.registerMachine(type)` to register `ItemStorage.SIDED` / `FluidStorage.SIDED` / `EnergyStorage.SIDED` `registerForBlockEntity(...)` returning the `Exposed*`/`ExposedEnergyStorageImpl` adapters for that machine's neutral storages + configured side flow. This restores the exact runtime behavior the old `getExposedStorage` provided.
- [ ] **Step 2** Ensure machine block-entity types call `MachineLibPlatform.storageExposer().registerMachine(type)` at registration (or the Fabric entrypoint iterates registered machine types).
- [ ] **Step 3** Run: `./gradlew :fabric:test` — Expected: PASS (unit tests).
- [ ] **Step 4** Commit: `git commit -am "feat(fabric): registration-driven storage exposure via platform SPI"`

### Task 1.7: Move tests + gametests to the `fabric` module and prove green

**Files:** `git mv src/test` → `fabric/src/test`; `git mv src/testmod` → `fabric/src/testmod`; port the `loom { runs { gametest, data } }` + `sourceSets { testmod }` config into `fabric/build.gradle.kts`.

- [ ] **Step 1** Move `src/test` and `src/testmod` under `fabric/`; re-add the `testmod` sourceset + `gametest`/`data` run configs to the fabric script (from the old root `loom {}`).
- [ ] **Step 2** Run: `./gradlew :fabric:test` — Expected: PASS (all 13 unit test files).
- [ ] **Step 3** Run: `./gradlew :fabric:runGametest` (the moved gametest run) — Expected: gametests PASS, `junit.xml` written.
- [ ] **Step 4** Run: `./gradlew :fabric:runClient` — Expected: title screen loads; open the testmod machine GUI, insert/extract an item/fluid/energy via an adjacent pipe/hopper — behavior identical to pre-port.
- [ ] **Step 5** Commit: `git commit -am "test: relocate unit + gametests to fabric module (green)"`

### Task 1.8: Phase 1 gate

- [ ] **Step 1** Run: `./gradlew build` — Expected: BUILD SUCCESSFUL (both `:fabric` and `:neoforge` — neoforge still the empty Phase-0 skeleton).
- [ ] **Step 2** Confirm `grep -rlE "net\.fabricmc|team\.reborn" common/src/main/java` is empty and `./gradlew :fabric:test` + gametests are green.
- [ ] **Step 3** Tag/commit: `git commit -am "chore: Phase 1 complete — common neutral core, Fabric green"`

---

## Self-Review

**Spec coverage:** §4 module structure → Tasks 0.2–0.8, 1.5. §5.1 leak points → Tasks 1.2, 1.3. §5.2 exposure SPI → Tasks 1.1, 1.6. §5.3 fluid units / §5.4 energy width → deferred to Phase 3 (NeoForge adapter) — noted, no Fabric behavior change in Phase 1. §6 touchpoints: registration/config/networking/menus/client explicitly kept on Fabric in Phase 1 and scheduled for Phase 2/3 plans (in scope, correctly deferred). §7 breaking API changes → Tasks 1.2, 1.3. §9 testing → Tasks 1.7, 1.8. §10 versions → Tasks 0.1, 0.3. **Gap check:** the Yarn→Mojmap decision surfaced during planning is captured as Task 0.9 + 1.0 (not in the original spec — a correct addition).

**Placeholder scan:** Gradle scripts intentionally reference "keep the existing X block" for verbatim relocations of already-written config (version/spotless/manifest/runs) rather than duplicating dozens of lines — the source lines exist in the current root `build.gradle.kts`; each such reference names the exact block. No "TBD"/"implement later" left.

**Type consistency:** `StorageExposer.registerMachine(BlockEntityType<?>)`, `MachineLibPlatform.storageExposer()`, `ExposedEnergyStorageImpl(MachineEnergyStorage,long,long)`, and the neutral `tryInsert/tryExtract/insert/extract/setEnergy/getAmount/getCapacity` surface are used consistently across Tasks 1.1, 1.2, 1.6.

---

## Phase 1 execution log (2026-07-09) — sharpened scope

**Toolchain resolved (differs from initial guesses):** Gradle **8.10**, architectury-loom
**1.7.416**, architectury-plugin **3.4.164**, Architectury API 13.0.6, NeoForge 21.1.235.
loom-1.10 is for a newer Gradle/MC pairing and the plugin can't drive its NeoForge platform;
each NeoForge subproject needs `neoforge/gradle.properties` → `loom.platform=neoforge`.

**Mappings switch is free:** source is already 100% Mojmap; mojarn only supplied Yarn
parameter names. `officialMojangMappings()` is now used everywhere; no source remap needed.

**Phase 0 complete & committed:** both loaders build loadable jars.

**Phase 1 structural relocation done** (files moved, `git`-tracked): `common` holds the
neutral tree; the following moved to `fabric` (same packages): `client/**`, `impl/network/**`,
`impl/compat/{rei,waila,transfer}`, `impl/storage/exposed/**`, `api/compat/transfer/**`,
`api/item/**`, `api/util/{AdjacentBlockApiCache,EnergySource,FluidSource,ItemSource,StorageHelper}`,
`impl/util/AdjacentBlockApiCacheImpl`, `impl/MachineLib`.

**Remaining neutralization surface in `common` (~30 files) — the actual Phase 1 work:**

1. **Storage/energy/slot engine (mechanical, ~15 files):** drop `extends EnergyStorage`,
   the `SnapshotParticipant` supertype, `TransactionContext` overloads, `StoragePreconditions`,
   and `FluidVariant`/`ItemVariant` from signatures. Keep the existing `try*`/commit/`set*`
   surface. Files: `api/storage/{MachineEnergyStorage,MachineItemStorage,MachineFluidStorage,
   ResourceStorage,StorageAccess}`, `api/storage/slot/{Fluid,Item}ResourceSlot`,
   `api/misc/MutableModifiable`, `impl/storage/{MachineEnergyStorageImpl,EmptyMachineEnergyStorage,
   MachineFluidStorageImpl,MachineItemStorageImpl,ResourceStorageImpl,BaseSlottedStorage}`,
   `impl/storage/slot/{ItemResourceSlotImpl,ResourceSlotImpl}`.
2. **ResourceFilters (moderate):** 8 capability-inspecting factories → `@ExpectPlatform`
   `PlatformFilters` (Fabric: `ContainerItemContext`; NeoForge: `item.getCapability`).
3. **MLDataComponents:** replace `FluidVariant` component with a neutral fluid+components record.
4. **Exposure registration in `MachineBlockEntity`:** `FluidStorage/ItemStorage/EnergyStorage`
   `SIDED` registration + `RenderDataBlockEntity` → move behind the `StorageExposer` SPI +
   an `@ExpectPlatform` render-data hook (Fabric impl in `fabric`).
5. **Menu + networking abstraction (the hard part, pulled in from Phase 2):** `BaseBlock`,
   `BaseBlockEntity`, `api/menu/SynchronizedMenuType`, `impl/menu/{MenuDataImpl,TankImpl}` use
   `ExtendedScreenHandlerFactory/Type` + `ServerPlayNetworking`. These must move to Architectury
   `MenuRegistry`/`ExtendedMenuProvider` + `NetworkManager` **now** (they sit in the core block
   entity), not deferred.
6. **Gametests:** `api/gametest/{MachineGameTest,SimpleGameTest}` use `fabric-gametest`; move to
   the `fabric` sourceset (tests are Fabric-hosted through Phase 2).

**Recommended sub-sequencing (each a green, committable checkpoint):**
- **1a — Storage engine neutral in `common`:** do (1)+(2)+(3), keep block-entity/menu/gametest
  layer temporarily in `fabric`, wire Fabric exposure adapters. Result: neutral storage engine
  compiles in `common`; Fabric green. This is the high-value Approach-A milestone.
- **1b — Block-entity + menu + networking to `common`:** do (4)+(5)+(6) on Architectury APIs.
- **1c — Move unit tests + gametests, prove Fabric green** (Task 1.7/1.8).

## Phase 1a progress update (2026-07-09, second pass)

**Done & committed** (branch `feat/architectury-multiloader`, through commit "relocate gametest framework to fabric"):
- Storage engine fully neutralized in `common` — interfaces AND impls (`MachineEnergyStorageImpl`,
  `ResourceStorageImpl`, `BaseSlottedStorage`, `Machine{Item,Fluid}StorageImpl`, `ResourceSlotImpl`,
  `ItemResourceSlotImpl`, `EmptyMachineEnergyStorage`) — no `SnapshotParticipant`, no transaction/variant/
  team-reborn types.
- `MachineBlockEntity`, `ConfiguredBlockEntity`, `MLDataComponents`, `ResourceFilters`, `Config`,
  `FluidResourceSlot` neutralized.
- New neutral scaffolding in `common`: `api/transfer/MLFluidStack` (fluid+components record with
  Codec/StreamCodec), `impl/platform/MachineLibPlatform` (`@ExpectPlatform` SPI:
  `registerMachineProviders`, `chargeFromItem`, `drainPowerToItem`, `takeFluidFromItem`,
  `drainFluidToItem`, `openMenu`, `sendToPlayer`), `impl/filter/PlatformFilters` (`@ExpectPlatform`).
- Fabric `impl/filter/fabric/PlatformFiltersImpl` created. Gametest framework moved to `fabric`.

**Remaining to make `common` compile (6 files, all interlocked — must land together):**
1. `impl/menu/MenuDataImpl` — instantiated in `common` `SynchronizedMenu:87`. Add SPI hook
   `MenuData createMenuData(ServerPlayer, int syncId)`; move `MenuDataImpl` → `fabric`; have
   `SynchronizedMenu` call the hook. Fabric impl builds the buffer + sends via `MenuSyncPayload`.
2. `api/menu/SynchronizedMenuType` — `extends ExtendedScreenHandlerType`. Replace with a neutral
   factory that produces a `MenuType<Menu>` via a new SPI hook `createMenuType(...)` (Fabric impl
   uses `ExtendedScreenHandlerType` + `BlockPos.STREAM_CODEC` exactly as today). Keep the
   `registerData(getData())` call in the create path.
3. `api/block/BaseBlock` + `api/block/entity/BaseBlockEntity` — replace `ExtendedScreenHandlerFactory`
   / `ServerPlayNetworking` usage with `MachineLibPlatform.openMenu(player, be)` and `.sendToPlayer`.
4. `api/menu/Tank` + `impl/menu/TankImpl` — replace `ContainerItemContext`/`FluidVariant`/`FluidStorage`/
   `Storage`/`StorageUtil` with `MLFluidStack` for display and the existing SPI hooks
   (`takeFluidFromItem`/`drainFluidToItem`) for held-item interaction.

**Then, to make `fabric` compile + tests green:**
5. Create `fabric/.../impl/platform/fabric/MachineLibPlatformImpl` implementing ALL `MachineLibPlatform`
   hooks (+ `createMenuData`, `createMenuType`) using the existing Fabric code (SIDED lookups,
   `ExtendedScreenHandlerType`, `ServerPlayNetworking`, `ContainerItemContext`/`StorageUtil`).
6. Fix the moved Fabric network payloads whose `apply`/handler methods reference client-only types
   (`ClientPlayNetworking`, `MachineStatusEvents`) — client handlers belong in the `client` package.
7. Register the platform SPI impls + payloads at Fabric init (`MachineLibFabric` → delegate to the
   moved `impl/MachineLib`); restore the full `fabric.mod.json` entrypoints (see
   `git show 99c7761^:src/main/resources/fabric.mod.json`).
8. Rewrite the Fabric exposed adapters (`impl/compat/transfer/Exposed*Impl`,
   `impl/storage/exposed/ExposedEnergyStorageImpl`) as `SnapshotParticipant`s wrapping the neutral
   storages (simulate via `try*`, commit via neutral `insert`/`extract`/`set*`, rollback via snapshot).

**Then Phase 1c:** `git mv src/test → fabric/src/test`, `src/testmod → fabric/src/testmod`; wire the
`testmod` sourceset + `gametest`/`data` runs into `fabric/build.gradle.kts` (from
`git show e02695a:build.gradle.kts`); `./gradlew :fabric:test` + `:fabric:runGametest` green.

**NOTE:** progress this pass was interrupted by an Anthropic account weekly usage limit (resets
2026-07-12); resume from the committed WIP tip.

## Deferred to later plans

- **Phase 2:** networking → Architectury `NetworkManager`; menus → `MenuRegistry`/`ExtendedMenuProvider`; config dir + `isModLoaded` → `Platform`. Pull these from `fabric` up into `common`.
- **Phase 3:** NeoForge core — `RegisterCapabilitiesEvent`, `IItemHandler`/`IFluidHandler`/`IEnergyStorage` adapters (fluid mB↔droplet + energy int-saturation from spec §5.3/§5.4), block capability caches, client model + `IClientFluidTypeExtensions`.
- **Phase 4:** cross-platform parity gametests; publish both artifacts; Galacticraft migration note.
- **Phase 5:** REI/JEI/EMI/WTHIT/Cloth/ModMenu per loader.

---

## Status update — 2026-07-09: Phases 2 & 3 COMPLETE (`./gradlew build` GREEN)

Full multiloader build passes: both `MachineLib-fabric-0.7.0.jar` and `MachineLib-neoforge-0.7.0.jar`
are produced; `transformProductionFabric` + `transformProductionNeoForge` bind every `@ExpectPlatform`
hook; 235/235 fabric unit tests pass.

**Phase 2 (Architectury services):** networking → `NetworkManager`, menus → `MenuRegistry` /
`SynchronizedMenuType.ofExtended`, config dir + `isModLoaded` → `Platform`. Done in `common`.

**Phase 3a/3b (NeoForge core):**
- `impl/storage/neoforge/Exposed{Energy,Item,Fluid}StorageNeoForge` — `IEnergyStorage`/`IItemHandler`/
  `IFluidHandler` adapters over the neutral storages (`simulate`→`try*`/commit; energy int-saturate;
  fluid mB↔droplet = ×/÷81).
- `impl/platform/neoforge/MachineLibPlatformImpl` — item↔machine transfer via item caps + `faceFor`.
- `impl/filter/neoforge/PlatformFiltersImpl` — 5 capability-inspecting filters via item caps.
- `neoforge/MachineLibNeoForge` — `@Mod` entrypoint: `MachineLib.init()` + `RegisterCapabilitiesEvent`
  registering energy/item/fluid BLOCK caps per collected `MACHINE_TYPES`.
- Dead `recipeRemainder` `@ExpectPlatform` hook deleted (caller had been neutralized to vanilla
  `getCraftingRemainingItem()`).

**Phase 3c (client layer — moved neutral client code to `common`, NeoForge rendering):**
- Moved to `common`: `DisplayUtil`, `GraphicsUtil`, `MenuDataClient`, `MachineScreen`,
  `MachineStatusEvents` (Fabric Event→Architectury `EventFactory.createLoop`), model records
  (`TextureProvider`, `MachineTextureBase`, `MachineModelData`, `MachineTextureBaseData`,
  `MachineModelDataLoader`, `MachineModelRegistryImpl`+marker constants), `MachineLibClientPackets`,
  and a new neutral abstract `MachineBakedModel` base holding the IO→sprite selection logic.
- Removed `createMenuDataClient`/`fluidTooltip`/`wrapText` from the server SPI (now direct neutral
  calls). New client SPI `MachineLibClientPlatform` (`fluidName`/`fluidSprite`/`fluidColor`/
  `fluidLighterThanAir`/`machineModel`), implemented per loader.
- Fabric: `FabricMachineBakedModel` (was `MachineBakedModel`) now extends the common base;
  `MachineLibClientPlatformImpl` via Fabric transfer/rendering APIs.
- NeoForge: `NeoForgeMachineBakedModel` (`IDynamicBakedModel`, faces via vanilla
  `UnbakedGeometryHelper.bakeElementFace`, IO config via `ModelData`/`ModelProperty` +
  model-level `getModelData`); `MachineGeometry`/`MachineGeometryLoader` (`machinelib:machine`
  geometry loader, base loaded lazily from the resource manager); `MachineLibNeoForgeClient`
  registers the loader + S2C receivers (client-gated in the `@Mod` ctor via `FMLEnvironment.dist`).
  Datagen now writes `"loader":"machinelib:machine"` (inert on Fabric).
- `MachineModelGenerator` stays per-loader (Fabric): relies on Fabric access-widened
  `BlockModelGenerators.{modelOutput,blockStateOutput,skipAutoItemBlock}`.

**Known follow-ups (not blocking build):**
- NeoForge **in-world model rendering** compiles and is structurally faithful but is unverified at
  runtime (no NeoForge client launched here); sprite atlas stitching for machine textures is the
  consuming mod's responsibility (as on Fabric). NeoForge machine **item** model injection is a gap.
- NeoForge **datagen** (`GatherDataEvent`) not yet provided.
- **Phase 4** parity gametests / publish; **Phase 5** integrations (REI/JEI/EMI/WTHIT/Cloth/ModMenu).
