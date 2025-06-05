# Minigames Plugin

![GitHub Release](https://img.shields.io/github/v/release/yourname/minigames-plugin)
![Java Version](https://img.shields.io/badge/java-17+-blue)
![Paper Version](https://img.shields.io/badge/paper-1.21.5--R0.1--SNAPSHOT-success)
[![Build, Release, and Archive Minigames Plugin JAR](https://github.com/AurorasChaos/readycraft-minigames/actions/workflows/maven-build.yml/badge.svg)](https://github.com/AurorasChaos/readycraft-minigames/actions/workflows/maven-build.yml)

A Minecraft minigames plugin for [PaperServer](https://papermc.io/) 1.21.5 that provides customizable arena-based minigames, commands, GUI menus, scoreboard integration, and PlaceholderAPI & Citizens2 support. Leverage this plugin to host fast-paced, server-side minigame experiences on your Paper server.

---

## Table of Contents

1. [Features](#features)  
2. [Prerequisites](#prerequisites)  
3. [Installation](#installation)  
   - [Download & Deploy JAR](#download--deploy-jar)  
   - [Build From Source](#build-from-source)  
4. [Configuration](#configuration)  
   - [`plugin.yml` Overview](#pluginyml-overview)  
   - [Default `config.yml`](#default-configyml)  
5. [Usage](#usage)  
   - [Basic Commands](#basic-commands)  
   - [Arena Setup](#arena-setup)  
   - [Integrations](#integrations)  
6. [Dependencies](#dependencies)  
7. [Contributing](#contributing)  
8. [License](#license)  

---

## Features

- 🎯 **Multiple Minigame Types**  
  - TNT Run  
  - Spleef  
  - Team Deathmatch  
  - Capture the Flag  
  - Custom game modes via configuration

- 🏟️ **Arena Management**  
  - Create, edit, and delete arenas dynamically through commands.  
  - Automatic region protection using WorldGuard.  
  - Arena lobby, spawn points, and spectate mode.

- 🔧 **GUI Menus**  
  - In-game inventory GUI for joining, leaving, and spectating games.  
  - Customizable item icons and menu titles via config.

- 🏅 **Scoreboard Integration**  
  - Real-time scoreboard for players (kills, lives, objectives).  
  - Automatic lobby scoreboard when not in a match.

- 🤝 **PlaceholderAPI Support**  
  - Placeholder expansions for displaying game stats in chat and scoreboards.  
  - Example placeholders: `%minigames_status%`, `%minigames_kills%`.

- 👥 **Citizens2 Integration**  
  - NPCs as game signups, announcers, or lobby guides.  
  - Shade-relocated Citizens classes to avoid dependency conflicts.

- 📜 **Configuration-Driven**  
  - All game logic, messages, and balancing are configurable via `config.yml`.  
  - YAML files for arena definitions, messages, and general plugin settings.

- 🔊 **Event Handling & Listeners**  
  - Custom listeners for player movement, damage events, death handling, and more.  
  - Reliable task scheduling using BukkitScheduler and `BukkitRunnable`.

---

## Prerequisites

- **Minecraft Server**: [Paper 1.21.5-R0.1-SNAPSHOT](https://papermc.io/downloads)  
- **Java**: OpenJDK or AdoptOpenJDK 17+ (plugin is compiled with Java 17; requires JDK 17 or newer)  
- **Build Tool**: [Apache Maven](https://maven.apache.org/) 3.6.0+  
- **Dependencies (Optional)**:  
  - [PlaceholderAPI 2.11.5](https://www.spigotmc.org/resources/placeholderapi.6245/) (for placeholder expansions)  
  - [Citizens2 2.0.38-SNAPSHOT](https://ci.citizensnpcs.co/job/Citizens2/) (for NPC support)  
  - [FastAsyncWorldEdit 7.2.13](https://www.spigotmc.org/resources/fast-async-worldedit.13932/) (for in-game map editing)  
  - [WorldGuard 7.0.7](https://enginehub.org/worldguard/) (for region protection)

---

## Installation

### Download & Deploy JAR

1. Go to the [Releases](https://github.com/yourname/minigames-plugin/releases) page.  
2. Download the latest `minigames-plugin-<version>.jar`.  
3. Place the downloaded JAR in your server’s `plugins/` directory.  
4. Restart (or reload) the server:  
   ```bash
   # From server console or terminal:
   java -jar paper-1.21.5-R0.1-SNAPSHOT.jar
   ```
5. A `MinigamesPlugin` folder (with `config.yml`, `arenas/`, etc.) will be created in `plugins/`.  

---

### Build From Source

If you prefer to compile the plugin yourself (e.g., to test changes):

1. Clone the repository:
   ```bash
   git clone https://github.com/yourname/minigames-plugin.git
   cd minigames-plugin
   ```
2. Verify you have Java 17+ installed:
   ```bash
   java -version
   # Should report "openjdk version 17.x.x" or higher
   ```
3. Build with Maven:
   ```bash
   mvn clean package
   ```
   - This will produce two JARs in `target/`:  
     - `minigames-plugin-1.0.0.jar` (plain artifact)  
     - `minigames-plugin-1.0.0-shaded.jar` (includes shaded Citizens relocations)

4. Copy the desired JAR (shaded or plain) into your server’s `plugins/` folder:  
   ```bash
   cp target/minigames-plugin-1.0.0-shaded.jar /path/to/your/paper-server/plugins/
   ```
5. Start (or restart) your Paper server and verify that `MinigamesPlugin` is enabled.

---

## Configuration

After the first run, the plugin will generate a folder structure under `plugins/MinigamesPlugin/`. Important files include:

```
plugins/MinigamesPlugin/
├── config.yml
├── arenas/
│   ├── example-arena.yml
│   └── ...
├── messages.yml
└── placeholder-expansions.yml
```

### `plugin.yml` Overview

Below is a summary of the `plugin.yml` entries (automatically bundled in the JAR). You generally do not need to modify it, but it’s useful to know the main commands and permissions:

```yaml
name: MinigamesPlugin
main: com.yourname.minigames.MinigamesPlugin
version: 1.0.0
api-version: 1.21
authors:
  - YourName
depend: [Paper, PlaceholderAPI, Citizens, WorldGuard]
commands:
  mg:
    description: Main Minigames command
    usage: /<command> [subcommand]
    aliases: [minigames]
permissions:
  minigames.use:
    description: Allows use of minor commands (join/leave/visit)
    default: true
  minigames.admin:
    description: Access to admin commands (create, delete, reload)
    default: op
```

### Default `config.yml`

```yaml
# -----------------------------------------------------------------------------
# MinigamesPlugin Configuration
# -----------------------------------------------------------------------------

# 1) General plugin settings
general:
  # Message prefix in chat
  prefix: "&8[&cMinigames&8] &r"
  # Broadcast interval (in seconds) for open arenas lobby
  lobbyBroadcastInterval: 30

# 2) Arena behavior defaults
arena-settings:
  # Default max players per game
  max-players: 16
  # Default min players required to start
  min-players: 2
  # Countdown time (seconds) before game starts once min-players met
  countdown: 15

# 3) Scoreboard settings
scoreboard:
  enabled: true
  update-interval-ticks: 20
  title: "&e&lMinigames &7[&a{status}&7]"

# 4) Message customization
messages:
  game-start: "{prefix}&aGame starting in {countdown} seconds!"
  not-enough-players: "{prefix}&cNot enough players to start (Requires {min} players)."
  joined-arena: "{prefix}&aYou joined &e{arena}&a!"
  left-arena: "{prefix}&cYou left &e{arena}&c!"
  already-in-game: "{prefix}&cYou’re already in a game."
  reload-success: "{prefix}&aPlugin reloaded successfully!"
  reload-failure: "{prefix}&cReload failed. Check console for errors."

# 5) PlaceholderAPI expansions
placeholders:
  # Example: %minigames_players_in_arena_{arena}% expands to number of players
  # List of custom expansions if needed

# 6) Arena file directory (automatically created)
arena-folder: arenas
```

> **Tip:** Feel free to tweak any of the values above to customize lobby messages, countdown timers, or default arena limits.

---

## Usage

### Basic Commands

All commands use `/mg` (alias: `/minigames`). Only players with `minigames.admin` can run admin subcommands.

```text
/ mg join <arena>        - Join a minigame arena
/ mg leave               - Leave your current game
/ mg list                - List all available arenas
/ mg create <arena>      - Create new arena (admin)
/ mg delete <arena>      - Delete arena folder (admin)
/ mg setspawn <arena>    - Set the spawn point for <arena> (admin)
/ mg reload              - Reload plugin configuration (admin)
/ mg spectate <arena>    - Spectate an ongoing game (admin or when enabled)
/ mg saveconfig          - Save default config and messages (admin)
/ mg help                 - Show help menu
```

#### Example: Creating & Joining an Arena

1. **Create a new arena named `parkour_arena`:**
   ```text
   /mg create parkour_arena
   ```
   - This will generate `plugins/MinigamesPlugin/arenas/parkour_arena.yml` with placeholder values.

2. **Set the lobby spawn & game spawn locations:**
   - Stand at your desired lobby spawn location and run:
     ```text
   /mg setspawn parkour_arena lobby
   ```
   - Stand at your desired in-game start location and run:
     ```text
   /mg setspawn parkour_arena game
   ```

3. **Join the arena as a player:**
   ```text
   /mg join parkour_arena
   ```

4. **Leave the arena:**
   ```text
   /mg leave
   ```

5. **Spectate a running game:**
   ```text
   /mg spectate parkour_arena
   ```

---

### Arena Setup

After running `/mg create <arenaName>`, locate the generated file in `plugins/MinigamesPlugin/arenas/<arenaName>.yml`. A typical arena file structure:

```yaml
arenaName: parkour_arena

# 1) Display name shown in GUI/scoreboard
displayName: "Parkour Arena"

# 2) Max/min players
maxPlayers: 12
minPlayers: 2

# 3) World & region boundaries
world: world
lobbyLocation:
  x: 100.5
  y: 64.0
  z: -150.5
  yaw: 0.0
  pitch: 0.0
gameSpawn:
  x: 120.0
  y: 65.0
  z: -130.0
  yaw: 180.0
  pitch: 0.0

# 4) Game-specific settings
gameType: TNT_RUN
countdown: 20
timeLimitSeconds: 300

# 5) Rewards & end conditions
reward:
  money: 50
  XP: 10
```

> **Note:**  
> - Valid `gameType` values: `TNT_RUN`, `SPLEEF`, `TDM`, `CTF`, or custom-defined types.  
> - Customize `reward` as needed; if you use an economy plugin, you can integrate money rewards.

---

### Integrations

#### PlaceholderAPI

When PlaceholderAPI is installed, you can display dynamic placeholders in chat, scoreboards, and holograms. Example placeholders:

- `%minigames_status_{arena}%` → `WAITING`, `IN_PROGRESS`, or `FINISHED`  
- `%minigames_players_{arena}%` → Number of current players in the arena  
- `%minigames_winner_{arena}%` → Name of winning player/team  

Make sure to register your plugin’s expansion (if not auto-registered) by running:

```text
/placeholderapi reload
```

#### Citizens2

If Citizens2 is present on the server, you can spawn an NPC that automatically opens the join GUI when a player right-clicks it. Create an NPC and assign it a name such as `MinigamesNPC`. The plugin will detect any NPC whose name starts with “Minigames” and attach a click event:

```text
# In-game (as an OP):
/npc create MinigamesNPC --type VILLAGER
```

Optionally, you can set the NPC’s skin, look direction, and equipment. When a player right-clicks the NPC, it will open the lobby GUI listing all available arenas.

---

## Dependencies

This plugin declares the following dependencies in `pom.xml`:

```xml
<dependencies>
  <!-- Paper API (provided) -->
  <dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>1.21.5-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
  </dependency>

  <!-- PlaceholderAPI (provided) -->
  <dependency>
    <groupId>me.clip</groupId>
    <artifactId>placeholderapi</artifactId>
    <version>2.11.5</version>
    <scope>provided</scope>
  </dependency>

  <!-- Citizens2 API (compile) -->
  <dependency>
    <groupId>net.citizensnpcs</groupId>
    <artifactId>citizensapi</artifactId>
    <version>2.0.38-SNAPSHOT</version>
    <scope>compile</scope>
  </dependency>

  <!-- WorldEdit/FAWE (provided) -->
  <dependency>
    <groupId>com.sk89q.worldedit</groupId>
    <artifactId>worldedit-bukkit</artifactId>
    <version>7.2.13</version>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>com.fastasyncworldedit</groupId>
    <artifactId>FastAsyncWorldEdit-Core</artifactId>
    <scope>provided</scope>
  </dependency>

  <!-- WorldGuard (provided) -->
  <dependency>
    <groupId>com.sk89q.worldguard</groupId>
    <artifactId>worldguard-bukkit</artifactId>
    <version>7.0.7</version>
    <scope>provided</scope>
  </dependency>

  <!-- Testing (JUnit & Mockito) -->
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.0.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

- **`provided`** scope means the server must supply these (e.g., through its plugin folder).  
- The **Citizens2** dependency is compiled/shaded into your plugin JAR to avoid classpath conflicts.

---

## Contributing

Contributions, bug reports, and feature requests are welcome! Please follow these guidelines:

1. **Fork** the repository.  
2. **Create a new branch** for your feature or bugfix:  
   ```bash
   git checkout -b feature/awesome-feature
   ```
3. **Write clean, documented code** and update relevant configuration or message files.  
4. **Add/Update tests** under `src/test/java` if applicable.  
5. **Ensure existing tests pass** and your changes do not break core functionality:  
   ```bash
   mvn clean test
   ```
6. **Submit a Pull Request** describing your changes, why they’re needed, and any relevant screenshots or examples.

Before merging, ensure the following:

- Code style matches existing conventions (4-space indentation, clear method/variable names).  
- No new warnings or build errors.  
- Adequate documentation is provided (update `README.md` or add JavaDoc as needed).

---

## License

This project is licensed under the [MIT License](LICENSE). See `LICENSE` for details.

---

> _“Build fun, fair, and fast – and let the games begin!”_  
> – YourName (Plugin Author)  
