# Build & Deploy

## Requirements

- Java 21 (Gradle manages its own toolchain — system Java version may differ)
- Gradle 9.4.0 (wrapper included)

## Build Commands

```bash
./gradlew build              # Build all modules (core, paper, neoforge, fabric)
./gradlew :paper:build       # Build single module
./gradlew :core:build        # Build core only
```

Output JARs:
- `paper/build/libs/paper.jar`
- `neoforge/build/libs/neoforge.jar`
- `fabric/build/libs/fabric.jar`

## Deploy to Dev Servers

```bash
./scripts/deploy.sh paper          # Deploy to Paper
./scripts/deploy.sh all            # Deploy to all platforms
```

The deploy script copies built JARs into `_data/<platform>/plugins|mods/TakaroMinecraft.jar`.

**You must build before deploying.** The deploy script does not trigger a build.

## Reload After Deploy

```bash
./scripts/reload.sh paper          # Paper: sends `reload confirm` via RCON
```

NeoForge and Fabric cannot hot-reload — restart the container instead:

```bash
docker compose restart neoforge
docker compose restart fabric
```

## Version Catalog

Dependencies are managed in `gradle/libs.versions.toml`:
- Minecraft 1.21.11
- Paper API 1.21.11-R0.1-SNAPSHOT
- NeoForge 21.11.38-beta
- Fabric Loader 0.18.4
- Shadow Plugin 9.3.2

## Build Gotchas

- System `java --version` may show Java 11, but Gradle uses its own Java 21 toolchain
- Shadow plugin relocates dependencies to `io.takaro.libs.*` to avoid classpath conflicts
- NeoForge uses ModDevGradle; Fabric uses Fabric Loom — these manage platform-specific build steps
