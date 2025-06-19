# Codex Agent Guidelines

This repository contains a Java 17 Spigot plugin built with Maven. The following conventions help Codex
contribute patches consistently.

## Coding Style
- Use **4 spaces** for indentation and avoid tabs.
- Keep line length under **120 characters**.
- Public classes and methods should include brief Javadoc comments.
- Follow Java naming conventions: `PascalCase` for classes and `camelCase` for methods/fields.
- New classes should reside under `src/main/java/com/auroraschaos/minigames` in an appropriate package.

## Build
- Compile the plugin with `mvn -q package`. This ensures the sources build correctly.
- If tests are added later, run `mvn -q test` as well.
- Maven may not be available in the Codex environment; if a build step fails for this reason, mention it in the
  PR's testing section.

## Project Structure
- Java sources live in `src/main/java` under the `com.auroraschaos.minigames` package hierarchy.
- Plugin resources and defaults are in `src/main/resources` alongside `plugin.yml`.
- The Maven build outputs the plugin jar to `target/`.
- `pom.xml` declares dependencies and the build configuration.

## Documentation
- Update `README.md` or other docs when behaviour or configuration changes.

## Commits & PRs
- Use short imperative commit messages (e.g. `Add countdown timer utility`).
- The PR description should summarise what changed and note the result of running the Maven build.
## Design Overview
- **Primary goals:** modularity, scalability and easy configuration via YAML and GUI.
- Integrate with Citizens2, PlaceholderAPI, WorldEdit and Vault.
- **Architecture:** `MinigamesPlugin` bootstraps the managers.
The managers layer (`GameManager`, `ArenaManager`, `StatsManager`, `PartyManager`,
  `GUIManager`, `NPCManager`, `PlaceholderAPIHook`) handles games, arenas, parties and stats.
Game logic lives in `GameInstance` subclasses and modules communicate via `GameStartEvent` and `GameEndEvent`.

## Testing Guidelines
- Write unit tests for modules and mock dependencies where possible.
- Verify event flow (e.g. ensure `GameStartEvent` is fired) and use a local test server for integration tests.
- Enable debug logging when troubleshooting.

## Minigames Catalog
- Example games in `Minigames.txt` include TNT Run, Bed Wars, Sky Wars, Spleef, Parkour Race,
  Capture the Flag, Block Hunt, Mob Arena, Gladiator Duel, Build Battle and Quake.
- Further entries list Maze Runner, Lucky Blocks, Wool Wars, Fishing Frenzy, Dodgeball,
Slime Soccer, Boss Battle, Mini Walls and Trouble in Terrorist Town.
