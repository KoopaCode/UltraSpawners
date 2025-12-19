UltraSpawners [LITE]

UltraSpawners is in active development.
The plugin works, but some features are incomplete, unstable, or disabled.
Expect bugs and breaking changes.

Advanced spawner management plugin for Paper 1.21.4 Minecraft servers.
Free and open source.

Issues and bug reports:
https://github.com/KoopaCode/UltraSpawners/issues

Community support:
https://discord.gg/UafKUcTKSp

## Features

- **Spawner Items**. Break any spawner to receive a drop with full data preserved
- **Stacking**. Merge spawners of the same type on one block with configurable limits
- **Upgrade Tiers**. Config-driven spawning upgrades per tier
- **Economy Integration**. Optional Vault support for upgrades
- **Multi-Database**. SQLite by default or MySQL
- **Anti-Dupe**. Atomic saves and NBT validation
- **TPS Guard**. Automatic scaling during low TPS
- **GUI Menu**. Visual upgrade interface
- **Admin Commands**. Full inspection and control

## Installation

1. Download the compiled JAR from Modrinth
2. Place `UltraSpawners.jar` in the `plugins/` folder
3. Restart the server
4. Configure `plugins/UltraSpawners/config.yml`
5. Reload with `/ultraspawners reload`

## Download Latest Builds

You can get the latest builds and pre-release JARs from either the GitHub releases page or the Modrinth plugin page:

- GitHub Releases: https://github.com/KoopaCode/UltraSpawners/releases
- Modrinth Plugin Page: https://modrinth.com/plugin/ultraspawners

Tips:
- Use the GitHub Releases page to download official release JARs and view changelogs.
- Use the Modrinth page to download published builds and archive versions.
- The plugin also includes a runtime version checker which fetches the `versions/modrinth` file from the repository. If a newer version is available, server operators (OPs) or players with `*` permission will be notified in-game with the latest version number and the short changelog.

Example:

If you're on `1.0.0` and the latest entry in `versions/modrinth` is `1.0.1 (Fixed major dupe exploit)`, the plugin will display a console message and notify OPs in-game: "New update available for UltraSpawners: 1.0.0 -> 1.0.1 — Fixed major dupe exploit".

## Configuration

### General
```yaml
general:
  prefix: "&8[&5UltraSpawners&8] &r"
  debug: false
```

### Drops
```yaml
drops:
  dropOnBreak: true
  dropOnExplosion: true
```

### Stacking
```yaml
stacking:
  enable: true
  maxStackPerBlock: 64
  mergeDifferentTypes: false
```

### Upgrades

Fully config-driven tiers.

```yaml
upgrades:
  enable: true
  maxTier: 4
  paymentMode: ITEMS
  tiers:
    0:
    1:
```

### Economy
```yaml
economy:
  vaultEnable: false
  vaultDisabledMessage: "&cVault is disabled"
```

If Vault is disabled or missing, Vault-based upgrades are blocked.
All other features continue to work.

### Performance
```yaml
performance:
  spawnCapPerTrigger: 20
  enableTpsGuard: true
  tpsThreshold: 15.0
  minimumSpawnAtLowTps: 1
```

### Storage
```yaml
storage:
  type: SQLITE
  sqlite:
    filePath: "plugins/UltraSpawners/database.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "ultraspawners"
    username: "root"
    password: "password"
    useSSL: false
    maxPoolSize: 10
```

## Database Setup

### SQLite
No setup required. The database is created automatically.

### MySQL
1. Create a database
2. Set credentials in `config.yml`
3. Tables are created on first startup

## Commands

| Command | Permission | Description |
|--------|-----------|-------------|
| `/ultraspawners reload` | `ultraspawners.admin` | Reload config |
| `/ultraspawners give <player> <type> [stack] [tier]` | `ultraspawners.give` | Give spawner |
| `/ultraspawners inspect` | `ultraspawners.use` | Inspect spawner |
| `/ultraspawners settype <type>` | `ultraspawners.edit` | Change mob |
| `/ultraspawners setstack <amount>` | `ultraspawners.edit` | Set stack |
| `/ultraspawners settier <tier>` | `ultraspawners.edit` | Set tier |
| `/ultraspawners menu` | `ultraspawners.menu` | Open menu |

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `ultraspawners.use` | false | Basic use |
| `ultraspawners.admin` | op | Admin access |
| `ultraspawners.give` | false | Give spawners |
| `ultraspawners.edit` | false | Edit spawners |
| `ultraspawners.menu` | true | Access menu |

## Anti-Dupe Protections

- Atomic database writes
- NBT validation on items
- Range and sanity checks
- Chunk load validation
- Persistent storage on restart

## Credits

Developer: Koopa  
https://github.com/KoopaCode

## License

MIT License

Copyright (c) 2025 Andrew P. Harper
