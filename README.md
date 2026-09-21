# Server Info — Fabric 26.2 — Server Only

This version deliberately does **not** use Cloth Config because Cloth Config requires client-side code.

The mod is `environment: server`, so players do not install anything.

### Commands

`/serverinfo` — opens the server information GUI.

`/serverinfo mods` — opens the installed Fabric mods page.

`/serverinfo datapacks` — opens the active datapacks page.

### GUI

The GUI uses a normal vanilla 6-row chest screen. Because the screen is a vanilla Minecraft screen, completely unmodded clients can display it.

It lists:
- Fabric mod ID
- Mod name
- Mod version
- Active datapack IDs
- Pagination for large lists

### Build

Use Java 25:

```bash
./gradlew build
```

Output:

`build/libs/server-info-1.0.0.jar`

Install that JAR only in the server's `mods` folder.

### Notes on Minecraft 26.x

Minecraft 26.1+ ships unobfuscated (official Mojang mappings baked in), so
Fabric Loom no longer creates the `modImplementation` / `modApi` /
`modCompileOnly` configurations — plain `implementation`, `api`, and
`compileOnly` are used instead, and there is no `mappings` line in
`build.gradle`.
