# PrismArenas

> Lightweight Minecraft Bukkit/Spigot plugin for creating and managing PvP arenas.

## Features

- Create and manage arenas with bounds and spawn points
- Wand selection tool for choosing region corners
- GUI editor for arena properties
- Snapshot & regeneration system for arena resets
- YAML-based storage with pluggable storage providers

## Installation

1. Build the plugin with Maven:

```powershell
mvn clean package
```

2. Copy the resulting JAR from `target/` into your server's `plugins/` folder.
3. Start the server once to generate configuration and data files (`config.yml`, `arenas.yml`, `messages.yml`).

## Configuration

After first run, edit the generated YAML files in the plugin data folder to adjust settings and arena definitions.

- `config.yml` — global plugin settings
- `arenas.yml` — arena definitions and saved regions
- `messages.yml` — customizable chat messages

## Commands

The plugin provides in-game commands for creating and editing arenas. (Exact command names and permissions are defined in `plugin.yml`.) Common flows:

- Use the wand tool to select arena bounds (two-corner selection).
- Open the arena editor GUI to change settings and spawn points.

Check `plugin.yml` for the full command list and permission nodes.

## Wand Usage

The wand (selection tool) is provided by the plugin to mark two opposite corners of an arena. Once both corners are set, you can create an arena from the selection or save it to `arenas.yml`.

## Snapshot & Regeneration

PrismArenas uses a snapshot engine to record blocks in an arena and restore them when needed. The regeneration system runs tasks to safely reset arenas and uses YAML-backed snapshot storage by default.

## Development

Project structure highlights:

- Source: `src/main/java/me/qyro/prismarenas`
- Main class: `me.qyro.prismarenas.PrismArenas`
- Managers: `manager/` (arena, config, messages, snapshots)
- Storage providers: `storage/provider` and YAML implementations under `storage/yaml`

To build locally:

```powershell
mvn clean package
```

To run tests or inspect compiled resources, check the `target/` folder after a build.

## Contributing

Contributions are welcome. Please open issues or pull requests for bug fixes and small features. Follow existing code style and add tests where appropriate.

## License

Specify a license in the project (e.g., MIT, Apache-2.0) by adding a `LICENSE` file. If you want, I can add a default license for you.

## Need help?

If you want, I can:

- Add command / permission documentation extracted from `plugin.yml`.
- Add usage examples and screenshots for the GUI and wand.
- Create a `LICENSE` file or add SpigotMC publishing instructions.

---
Generated for the PrismArenas project.

## READ.md made by AI. 
