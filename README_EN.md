# MeowsEnchants

[中文](README.md)

MeowsEnchants is a Paper/Folia custom enchantment plugin for declaring enchantments in YAML and registering them into the vanilla enchantment registry during the Paper plugin bootstrap phase.

## 1. Feature Overview

MeowsEnchants registers custom enchantments into the server like vanilla enchantments, instead of imitating enchantments through NBT, PersistentDataContainer data, or item lore like many traditional custom enchantment plugins.

Compared with common NBT- or lore-based custom enchantment plugins, this plugin is different in several ways:

- Custom enchantments are registered into the vanilla registry. They behave like real vanilla enchantments and can be accessed directly through the Bukkit API, so this plugin does not need to provide a separate API.
- Registered enchantments can be obtained directly from the creative inventory, vanilla `/minecraft:enchant`, or other plugins' `/enchant` commands, and their properties can be read correctly.
- Registered enchantments support vanilla-style conflicts, applicable items, anvil costs, and other registry properties, making them naturally compatible with other enchantment-based plugins.
- Enchantment effects are composed declaratively with triggers, targets, and actions instead of hardcoding a separate Java implementation for each enchantment.
- Plugin listeners independently control the logic and probability for each enchantment to appear in villager trades and enchanting table rolls.

## 2. Server Compatibility

- Target server: Paper/Folia `1.21.11 - 26.2+`
- Java: `25`
- Plugin type: Paper plugin using the `bootstrapper` field in `paper-plugin.yml`
- Folia: full support

| Server / Version                               | Compatibility  | Result                                                                                                           |
|------------------------------------------------|----------------|------------------------------------------------------------------------------------------------------------------|
| Folia `1.21.11 - 26.2+`                        | ✅Supported     | Primary target environment; adapted for the Folia scheduler model.                                               |
| Paper `1.21.11 - 26.2+`                        | ✅Supported     | Falls back to the normal Paper scheduler, but still requires Paper plugin bootstrapper and Registry API support. |
| Paper/Folia `1.21.10` and lower                | ❌Not supported | Missing experimental APIs used by the plugin: `RegistryComposeEvent` and required `ItemTypeTagKeys`.             |
| Spigot / CraftBukkit                           | ❌Not supported | They do not provide the Paper plugin bootstrapper or Paper Registry Mutation API.                                |
| Paper downstreams such as Purpur, Leaf, Leaves | ✅Supported     | Supported if the fork keeps the corresponding experimental Paper API intact.                                     |

Paper's Registry Mutation API is experimental. Future Paper/Folia updates may change the API shape; please report issues if they occur.

## 3. How It Works

### Enchantment Registration

During the bootstrap phase, the plugin reads `plugins/MeowsEnchants/enchants/*.yml`.
Each enchantment has its own YAML file and declares:

- Enchantment ID, display name, maximum level, anvil cost, applicable item tag, and conflicting enchantments
- Trigger, such as attack, mining, right-click, passive, sneaking, respawn, and more
- Target, such as self, players, mobs, or hit entities
- Actions, such as bonus damage, potion effects, explosions, multi-block mining, replanting, velocity, commands, and more
- Independent enchanting table and librarian villager trade rules

Each valid YAML file is parsed into an `EnchantConfig`, then registered into the vanilla enchantment registry through Paper's `RegistryEvents.ENCHANTMENT.compose()`. Registration sets:

- Enchantment description text
- Applicable item tag
- Active equipment slots
- Maximum enchantment level
- Anvil cost
- Enchanting table minimum and maximum cost
- Conflicting enchantment set

### Triggers

The plugin listens to Bukkit/Paper events and executes actions according to the configured trigger. Common trigger timings include:

- Combat: attacking entities, taking damage, blocking with a shield, killing entities
- Blocks: breaking blocks, left-click mining blocks, placing blocks, right-click interaction
- Projectiles: projectile hitting an entity or block
- Player states: starting sneaking, holding sneak, starting sprinting, holding sprint, periodic passive trigger
- Player lifecycle and items: death, respawn, swapping hands, consuming items
- Fishing: catching fish or entities, fish bite

### Targets

Before actions execute, the plugin resolves the enchantment target according to the configured target. Supported target meanings include:

- The trigger player
- Target player
- Target mob
- All entities
- No target

### Actions

Actions are declared in the `actions` list and executed in order when the enchantment triggers. Built-in actions include:

`potion_effect`, `bonus_damage`, `healthsteal`, `teleport`, `explosion`, `velocity`, `shockwave`, `break_armor`, `drop_head`, `attribute_modifier`, `multi_break`, `vacuum_drops`, `replant`, `multi_arrow`, `repair`, `hook`, `feast`, `deflect`, `till`, `reflect_damage`, `command`, `block_break`

Detailed action parameters are not documented in this README. See:

- [English: docs/action_types_en.md](docs/action_types_en.md)
- [中文版：docs/action_types_zh.md](docs/action_types_zh.md)

## 4. Usage

1. Download the plugin jar.
2. Put the plugin jar into the server `plugins/` directory.
3. Start the server once and let the plugin generate `plugins/MeowsEnchants/config.yml`.
4. Create one YAML file for each custom enchantment under `plugins/MeowsEnchants/enchants/`.
5. Restart the server. Enchantment config changes require a full server restart. Custom enchantments can only be registered during bootstrap and cannot be dynamically added to the server registry through reload.
6. Successfully registered custom enchantments can be viewed directly in the creative inventory, JEI/REI enchantment lists, and recipe lookup views.

Global config path: `plugins/MeowsEnchants/config.yml`. It currently controls:

- Skyllia compatibility logic, used to make enchantments respect island protection mechanics
- Whether enchanting table probability considers player level
- Book probability balancing, making custom enchantments harder to obtain on books than on weapons or equipment

Enchantment config file path:

```text
plugins/MeowsEnchants/enchants/<enchant_id>.yml
```

## 5. Building

This project uses Maven. Dependencies are resolved from the PaperMC Maven repository and the PlaceholderAPI repository.  
The project currently uses Java `25`. Before building, make sure your local JDK matches the Java version used by Maven.

With a normal Maven installation:

```bash
mvn clean package
```

Build output:

```text
target/meowsenchants-0.1.jar
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
