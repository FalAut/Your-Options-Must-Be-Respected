# Your Options Must Be Respected

Ensure that the default options and configurations provided by a modpack are correctly written before Minecraft reads them.

**Your Options Must Be Respected** is a NeoForge utility mod for modpacks. It scans the default files provided by the modpack early during game startup and copies them to the actual game directory or `config` directory. It is suitable for distributing default key bindings, graphics, audio, accessibility settings, and other configuration files that should only be written on first launch.

This mod is a fork of **Your Options Shall Be Respected**. While retaining the core idea of default configuration synchronization, it adjusts for scenarios where `options.txt` is pre‑created by the launcher.

## Features

- Executes before Minecraft reads `options.txt` and regular configuration files.
- Supports distributing a default `options.txt`.
- Supports distributing default `config` files.
- Does **not** repeatedly overwrite existing or player‑modified regular configurations.
- Uses a marker file for `options.txt` to prevent persistently resetting player settings on subsequent launches.
- Writes via temporary files and move operations to reduce the risk of partial writes.
- Contains no game content, blocks, items, or UI changes – focuses solely on modpack default configuration synchronization.

## Motivation

The main reason for creating this mod is to accommodate launchers that pre‑create `options.txt` before the game fully launches (e.g., to set the language automatically). In such cases, many similar default‑sync mods detect that `options.txt` already exists and skip the sync, preventing the modpack author’s default options from taking effect.

To solve this, this mod introduces an `options_applied.flag` design: after applying the default `options.txt` for the first time, a marker file is written. This allows the mod to replace the launcher‑generated `options.txt` when the marker is absent, while preserving player‑modified settings on subsequent launches – avoiding repeated resets.

## Use Cases

This mod is intended for modpack authors, for example:

- To ensure players get recommended graphics, audio, language, or key binding settings on first launch.
- To pre‑set certain client or server configurations without overwriting player changes on every launch.
- To solve the problem where a launcher pre‑creates `options.txt`, preventing the modpack’s default options from applying.
- To bundle default configuration files inside the modpack directory, rather than asking players to manually copy files.

## Usage

Place default files into:

```text
.minecraft/config/yosbr/
```

Files are synchronized according to the following rules:

```text
.minecraft/config/yosbr/options.txt
=> .minecraft/options.txt
```

```text
.minecraft/config/yosbr/config/example.toml
=> .minecraft/config/example.toml
```

```text
.minecraft/config/yosbr/resourcepacks/example.zip
=> .minecraft/resourcepacks/example.zip
```

In other words:

- Files inside `config/yosbr/config/` are copied to the actual `.minecraft/config/`
- Other paths under `config/yosbr/` are copied to `.minecraft/` preserving the relative path.

## Overwrite Rules

`options.txt` is handled specially:

- If the player does **not** have an `options.txt`, the mod copies the default file.
- If an `options.txt` exists **but** no `options_applied.flag` exists, the mod assumes it may have been pre‑created by the launcher and replaces it with the default file **once**.
- After a successful replacement or copy, the `options_applied.flag` is written.
- On subsequent launches, as long as the marker file exists, the player’s `options.txt` will not be overwritten.

Other regular files follow simpler rules:

- Copy if the destination file does **not** exist.
- If the destination file already exists, keep it (do **not** overwrite).

## Advice for Modpack Authors

- Only put files that are truly needed as defaults into `config/yosbr/`.
- Do **not** put world saves, logs, caches, or any files that change with every play session.
- Test the first launch experience with a fresh instance before releasing.
- After modifying the default `options.txt`, if your test instance already has an `options_applied.flag`, delete that marker file before testing the first‑time application process again.
- 
> **Important Note**  
> When providing a default `options.txt`, make sure it contains the correct `version` key (i.e., the data version of your Minecraft target, e.g., `3955` for 1.21.1). 
> According to Minecraft's internal logic, if `options.txt` is missing the `version` field, the game will discard the file on startup and regenerate default options, effectively ignoring the preset defaults you provided.  
> You can check the data version for each release on the [Minecraft Wiki](https://minecraft.wiki/w/Options.txt) or copy the correct `version` line from a client that has been launched at least once.