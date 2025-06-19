# MinigamesPlugin 🎮

[![Minecraft Version](https://img.shields.io/badge/Spigot-1.13%2B-green)](https://www.spigotmc.org/) [![Java Version](https://img.shields.io/badge/Java-8%2B-brightgreen)](https://www.oracle.com/java/) [![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A **modular**, **extensible**, and **fun** Minigames plugin for Spigot/Bukkit Minecraft servers. Out-of-the-box support for **TNT Run**, **SkyWars**, and **Spleef**, plus:

- ❇️ Party system (team up to queue together)  
- 🗓️ Flexible queue & countdown with action-bars & scoreboards  
- 📊 In-game stats tracking (wins, losses, plays) & PlaceholderAPI hooks  
- 🖥️ Clickable GUI to join games instead of typing commands  
- 👑 Spectator mode with region-clamping for eliminated players  
- 🎯 Easy to add new game types and arenas via simple Java subclasses

---

## 🚀 Features

- **TNT Run**: Dynamic floor destruction, per-arena timer & “Players Left” sidebar.
- **SkyWars**: WE schematic paste, chest loot tables, ring-shrinking arena, random events.
- **Spleef**: Break snow blocks or launch snowballs to drop opponents; last player standing wins.
- **Trouble in Terrorist Town**: Hidden roles of traitor, detective and innocent battle until one team remains. Item shop contents and costs are configured in `TTT.yml`.
- **Party System**: Create/invite/disband parties with colored name-tag teams.
- **Queue Management**: Min/max players, 60s countdown, chat & action-bar updates.  
- **Scoreboards**:  
  - Queue sidebar showing waiting players & party leaders  
  - Arena sidebar with timer, players left, custom lines  
- **StatsManager**: Persistent `stats.yml` for wins/losses/plays per game, integrated with PlaceholderAPI.  
- **GUIManager**: Clickable inventories for selecting game type & mode.  
- **PlaceholderAPI** expansion:  
  ```
  %minigames_active_games%  
  %minigames_queue_<TYPE>%  
  %minigames_queue_<TYPE>_<MODE>%  
  %minigames_wins_<TYPE>%  
  %minigames_losses_<TYPE>%  
  %minigames_plays_<TYPE>%  
  ```  

---

## 📥 Installation

1. **Download** the latest `MinigamesPlugin.jar` from the [Releases](https://github.com/your-repo/minigames-plugin/releases).  
2. **Drop** it into your server’s `plugins/` folder.  
3. **Start** or **Reload** your server to generate default config files.  

---

## ⚙️ Configuration

After the first run, you’ll find:

```
plugins/
└─ MinigamesPlugin/
   ├─ config.yml
   ├─ stats.yml
   ├─ SkyWars.yml
   ├─ SkyWars/
   │  ├─ Arena1.yml
   │  ├─ Arena2.yml
   │  └─ Defaults.yml
   └─ schematics/
      └─ your_schematic.schem
```

### `config.yml`

```yaml
minigames:
  TNT_RUN:
    display_name: "&aTNT Run"
    icon: TNT
    lore:
      - "&7Run around until the floor falls!"
    minPlayers: 2
    maxPlayers: 8
    enabled_modes:
      - CLASSIC
      - HARDCORE
  SKY_WARS:
    display_name: "&bSkyWars"
    icon: ENDER_CHEST
    lore:
      - "&7Fight in the sky!"
    minPlayers: 2
    maxPlayers: 16
    enabled_modes:
      - SOLO
      - TEAMS
```

### `SkyWars.yml`

Global timers & event weights. Customize `event_interval`, `shrink_start`, loot-table defaults, etc.

### Arena files

- **Per-arena** overrides in `SkyWars/ArenaX.yml`, falling back to `Defaults.yml`  
- Define `schematic`, `center`, `spawns`, and `loot_table`

---

## 💬 Commands & Permissions

| Command                          | Description                                | Permission             |
| -------------------------------- | ------------------------------------------ | ---------------------- |
| `/minigames join <TYPE> <MODE>`  | Join a game queue                          | `minigames.play`       |
| `/minigames leave`               | Leave queue or active game                 | `minigames.play`       |
| `/minigames stats [player]`      | View stats (coming soon)                   | `minigames.play`       |
| `/mgui`                          | Open the Minigames GUI                     | `minigames.play`       |
| `/party create`                  | Create a new party                         | `minigames.play`       |
| `/party invite <player>`         | Invite player to your party                | `minigames.play`       |
| `/party leave`                   | Leave your current party                   | `minigames.play`       |
| `/party disband`                 | Disband your party (leader only)           | `minigames.play`       |

---

## 🧩 Extending & Contributing

1. **GameType**: Extend `GameInstance` and override `onGameStart()`, `onGameEnd()`, (and optionally `requiresTicks()/tick()`).  
2. **Registration**: Update `GameManager#startNewGame(...)` to wire your new class.  
3. **Arena**: Drop a new `.yml` under `SkyWars/` (or your game folder) and schematic under `schematics/`.  
4. **Submit** a PR—bug fixes, new minigames, docs!  

---

## 📜 License

Released under the [MIT License](LICENSE).  
Feel free to fork, tweak, and share your adventures!  

---

> “Games give you a chance to excel, and if you’re playing in good company, you don’t even mind if you lose because you had the enjoyment of the company during the game.” — **Garry Kasparov**
