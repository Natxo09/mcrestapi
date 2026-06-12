# MCRestAPI — Fabric Mod REST API for Minecraft

## Project Overview

Fabric mod for Minecraft 26.1.2 (dedicated server only) that exposes a REST API + WebSocket for monitoring and controlling Minecraft servers. Uses `com.sun.net.httpserver` from the JDK (zero external dependencies for HTTP).

## Tech Stack

| Component       | Version                    |
|-----------------|----------------------------|
| Minecraft       | 26.1.2                     |
| Fabric Loader   | 0.19.3                     |
| Fabric Loom     | 1.17.11                    |
| Fabric API      | 0.151.0+26.1.2             |
| Java            | 25                         |
| Gradle          | 9.5.1                      |
| Mappings        | Mojang official             |

## Project Structure

```
src/main/java/net/natxo/mcrestapi/
├── MCRestAPI.java              # Entrypoint (DedicatedServerModInitializer)
├── config/
│   └── ApiConfig.java          # JSON config loader (port, bind, API key)
├── http/
│   ├── ApiServer.java          # JDK HttpServer wrapper
│   ├── HttpUtil.java           # JSON response helper
│   ├── Router.java             # Route registration with auth
│   ├── middleware/
│   │   └── AuthMiddleware.java # Bearer token validation
│   └── websocket/              # (Phase 3)
├── endpoints/
│   ├── ServerEndpoint.java     # GET /api/server (supports ?fields= filtering)
│   └── PlayersEndpoint.java    # GET /api/players
└── collectors/
    ├── TpsCollector.java       # TPS/MSPT calculator via tick events
    └── PlayerTracker.java      # Thread-safe player data snapshots
```

## Inspecting Minecraft API (deobfuscated)

Minecraft 26.1+ ships **deobfuscated** — official class/method names with parameter names, no mappings. Inspect any Minecraft class directly from the Loom-provided merged jar:

```bash
# Full class inspection (replace 26.1.2 with the target MC version)
javap -p -classpath ~/.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar <fully.qualified.ClassName>

# Search for a specific method
javap -p -classpath ~/.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar <ClassName> 2>&1 | grep -i "methodName"

# List classes in the jar
jar tf ~/.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar | grep -i "ClassName"

# Authlib (GameProfile, etc.) — record-style accessors (name() not getName())
# locate it with: find ~/.gradle -name 'authlib-*.jar'
```

### Key API notes (Minecraft 26.1, official names)

- `GameProfile.name()` not `getName()` (authlib uses record-style)
- `ResourceKey.identifier()` not `location()`
- `ServerPlayer.server` is private — use `player.level().getServer()` instead
- `PlayerList.isOp()` takes `NameAndId`, not `GameProfile` — use `new NameAndId(gameProfile)`
- `DedicatedServer` has `getProperties()` for server.properties access
- `Settings.MutableValue` fields use `.get()` to read values
- `Difficulty.getSerializedName()` not `getKey()` (implements `StringRepresentable`)
- Weather/time moved off `LevelData`: use `Level.isRaining()` / `isThundering()`; the day time is `Level.getOverworldClockTime()` (26.1 WorldClock), and `getDayCount()` was removed (derive `clockTime / 24000`)

## Build & Run

Minecraft 26.1 requires **Java 25** — the Gradle JVM itself must run on JDK 25 (set `JAVA_HOME` to a JDK 25 if it isn't your default).

```bash
./gradlew build         # Compile and produce mod jar
./gradlew runServer     # Launch dedicated server with mod loaded (first run: accept EULA in run/eula.txt)
```

## Config

Generated at `run/config/mcrestapi.json` on first server start. Contains port, bind address, max connections, and auto-generated API key.

## Bruno Collection

API testing collection in `McRestApi-Bruno/`. Open with Bruno, select the "Local" environment. API key is stored as a variable in the environment.

## Publishing & Releases

- **Modrinth project ID:** `XZCgCz7D`
- **Minotaur plugin** configured in `build.gradle` for automated publishing
- **Modrinth token** stored in `.env` locally and in GitHub secret `MODRINTH_TOKEN`
- **CI/CD:** `.github/workflows/publish.yml` triggers on tag push (`v*`)
- **Two READMEs:** `README.md` (GitHub, full, **per-branch**) and `MODRINTH_README.md` (the **single shared** Modrinth description — keep it version-agnostic)

### Minecraft version support (branches)

The mod supports multiple Minecraft versions in parallel — **one branch per MC line**:

- `main` always tracks the **latest** Minecraft version (currently `26.1.2`). Older lines live on a branch named by MC version (e.g. `1.21.11`) and keep getting back-support.
- New MC version → branch off `main`, port it, merge into `main` via PR; the previous version stays on its own branch.
- Per branch, set the versions in `gradle.properties`, `fabric.mod.json`, and `gameVersions` in `build.gradle`.
- Modrinth versions stay unique across lines via `versionNumber = "${mod_version}+${minecraft_version}"` (e.g. `2.0.0+26.1.2`). Keep `mod_version` ranges disjoint per line (26.1 → `2.x`, 1.21.11 → `1.x`) so git tags never collide.
- `MODRINTH_README.md` is the shared Modrinth body across **every** version — never pin it to a single MC version. The GitHub `README.md` is per-branch and may be version-specific.
- Minecraft 26.1+ needs **Java 25**; the CI workflows already run on Java 25.

### How to publish a new version

1. Update `mod_version` in `gradle.properties`
2. Create an annotated tag with the changelog as the message:
   ```bash
   git tag -a v1.1.0 -m "Summary of this release

   - Change 1
   - Change 2
   - Fix something"
   ```
3. Push the tag: `git push origin v1.1.0`
4. The workflow automatically:
   - Builds the mod
   - Publishes to Modrinth with the tag message as changelog
   - Creates a GitHub Release with the JAR attached

## Rules

- NEVER add Co-Authored-By or any self-attribution in commits. No co-author lines, no "Generated by Claude", nothing.

## Development Plan

Full roadmap and technical plan in `docs/PLAN.md`.
