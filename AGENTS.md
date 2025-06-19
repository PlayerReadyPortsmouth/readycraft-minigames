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
