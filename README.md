# CreeperAttack

A Paper 1.21.x plugin that turns creepers into a timed survival event. Every
few minutes, a wave of creepers spawns near every online player for a short
window — a lightweight, config-driven "invasion" event.

## Features

- Automatic recurring event: every `event.cooldown-minutes` (default 4), a
  creeper attack event starts and lasts `event.duration-seconds` (default 30).
- `spawn.amount` creepers (default 20) are spawned near **each** eligible
  online player over the course of the event.
- Spawn locations are chosen randomly between `spawn.minimum-distance` and
  `spawn.radius` blocks from the player, and are validated for safety
  (solid ground, no lava/water, enough headroom) before a creeper is placed.
- Players are skipped if they are offline, dead, in a disabled world, or hold
  the `creeperattack.bypass` permission.
- Configurable creeper properties: powered state, explosion radius override,
  whether they drop loot, and an optional custom name.
- Admin command with tab completion: `/creeperattack <start|stop|reload>`.
- Config reloads apply immediately, including a new cooldown timer, without
  restarting the server.

## Installation

1. Build the plugin (see [Build Instructions](#build-instructions)) or obtain
   `CreeperAttack-1.0.jar` from the `build/libs/` directory.
2. Drop the jar into your Paper server's `plugins/` folder.
3. Start (or restart) the server. A default `config.yml` will be generated
   under `plugins/CreeperAttack/`.
4. Edit `config.yml` to taste and run `/creeperattack reload`, or just
   restart the server.

Requires **Paper 1.21.x** (or a Paper fork) and **Java 21**.

## Commands

All subcommands require the `creeperattack.admin` permission.

| Command                  | Description                                              |
|---------------------------|------------------------------------------------------------|
| `/creeperattack start`   | Immediately starts a creeper attack event (does not reset the automatic timer). |
| `/creeperattack stop`    | Stops the currently active creeper attack event.          |
| `/creeperattack reload`  | Reloads `config.yml` and applies the new settings.         |

Tab completion is provided for all three subcommands.

## Permissions

| Permission               | Default | Description                                             |
|---------------------------|---------|-----------------------------------------------------------|
| `creeperattack.admin`    | op      | Start, stop, and reload creeper attack events.            |
| `creeperattack.bypass`   | false   | Excludes the player from being targeted by events.        |

## Configuration

```yaml
enabled: true

event:
  cooldown-minutes: 4      # How often an event automatically starts.
  duration-seconds: 30     # How long the spawning period lasts.

spawn:
  amount: 20                # Creepers spawned per eligible player, per event.
  radius: 15                 # Max spawn distance from the player (blocks).
  minimum-distance: 5        # Min spawn distance from the player (blocks).

worlds:
  disabled:
    - world_nether
    - world_the_end

creeper:
  powered: false
  explosion-radius: default  # "default" keeps vanilla behaviour, or a number e.g. "3".
  can-drop-items: true
  custom-name-enabled: false
  custom-name: "&cEvent Creeper"

messages:
  event-started: "&cCreeper attack started!"
  event-ended: "&aCreeper attack ended!"
  no-permission: "&cYou do not have permission."
  # ...see the full list in config.yml, all colour-coded with '&' codes.
```

Setting `enabled: false` stops both the automatic timer and the manual
`/creeperattack start` command from starting new events.

## Build Instructions

This is a standard Gradle project targeting Java 21.

```bash
./gradlew build
```

The compiled plugin jar will be at `build/libs/CreeperAttack-1.0.jar`.

> **Note:** This project does not ship a Gradle wrapper jar (`gradle/wrapper/gradle-wrapper.jar`),
> since it was generated in an offline environment with no internet access
> to download one. Before running `./gradlew build` for the first time, run
> `gradle wrapper --gradle-version 8.8` once (with your own local Gradle
> install) to generate it, or simply run `gradle build` directly if you have
> Gradle 8.x installed. From then on `./gradlew build` will work normally.

## Project Structure

```
CreeperAttack/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── README.md
└── src/main/
    ├── java/com/example/creeperattack/
    │   ├── CreeperAttackPlugin.java   # Plugin entry point
    │   ├── command/                   # /creeperattack command + tab completion
    │   ├── manager/                   # EventManager, SpawnManager
    │   ├── task/                      # CreeperSpawnTask (repeating spawn rounds)
    │   ├── listener/                  # CreeperDeathListener (loot control)
    │   └── config/                    # ConfigManager
    └── resources/
        ├── plugin.yml
        └── config.yml
```

## How It Works

- `EventManager` owns a single repeating Bukkit scheduler task ("cooldown
  timer") that fires every `event.cooldown-minutes`. Reloading the config
  cancels and re-creates this task so duplicate timers never accumulate.
- When an event starts, `EventManager` spins up one `CreeperSpawnTask`
  (a `BukkitRunnable`) that runs `spawn.amount` times, spaced evenly across
  `event.duration-seconds`. Each run asks `SpawnManager` to place one
  creeper near every eligible online player.
- `SpawnManager` picks a random point in an annulus between
  `minimum-distance` and `radius` around the player, then validates the
  location (solid, non-liquid ground; enough headroom) before spawning. If
  no safe spot is found after a few attempts, that player is simply skipped
  for that round — the plugin never spawns into an unsafe block or crashes.
- Spawned creepers are tagged with a `PersistentDataContainer` marker so the
  optional "no drops" behaviour only ever affects creepers this plugin
  spawned, never naturally spawned ones.
- `EventManager.shutdown()` cancels every outstanding task on plugin
  disable/reload, so no scheduler tasks or entity references leak.
