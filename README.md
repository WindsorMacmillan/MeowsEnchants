# MeowsEnchants

MeowsEnchants is a Paper/Folia custom enchantment plugin for declaring custom enchantments in YAML and registering them into the vanilla enchantment registry during the Paper plugin bootstrap phase.

## 1. 插件功能简述

MeowsEnchants 让自定义附魔像原版附魔一样注册到服务端中，而非传统更多附魔那样通过 NBT、PersistentDataContainer 数据或物品 Lore 伪装成“附魔”。 

与常见基于 NBT 或 Lore 的更多附魔插件相比，本插件的主要区别是：

- 自定义附魔注册进原版注册表，附魔与真正的原版附魔完全无异，可以被Bukkit API直接获取，此插件无需提供任何API。
- 附魔注册后可以在创造模式物品栏、原版`/minecraft:enchant`或其他插件的`/enchant`命令中直接获取到，并能正确获取所有属性。
- 插件注册的附魔兼容原版附魔冲突、适用物品、铁砧成本等注册表属性，天然兼容任何其他基于附魔的插件。
- 附魔效果由“触发器 + 目标 + 动作”声明式组合，而不是为每个附魔写死一套 Java 逻辑，可以自由组合轻松拓展。
- 插件监听器按配置单独控制每一个附魔在村民交易和附魔台随机出现的逻辑和概率。

## 2. 服务端兼容性

- 目标服务端：Paper/Folia `1.21.11 - 26.2+`
- Java：`25`
- 插件类型：Paper plugin，使用 `paper-plugin.yml` 的 `bootstrapper`
- Folia：`全功能支持`


| 服务端 / 版本                      | 兼容性 | 结论                                                             |
|-------------------------------|-----|----------------------------------------------------------------|
| Folia `1.21.11 - 26.2+`       | 支持  | 当前主要目标环境，已按 Folia 调度模型适配                                       |
| Paper `1.21.11 - 26.2+`       | 支持  | 支持普通 Paper 调度器回退，但仍要求 Paper plugin bootstrapper 与 Registry API |
| Paper/Folia `1.21.10` 及更低     | 不支持 | 缺少插件使用的实验性API `RegistryComposeEvent` `ItemTypeTagKeys`         |
| Spigot / CraftBukkit          | 不支持 | 不提供 Paper plugin bootstrapper 与 Paper Registry Mutation API    |
| Purpur Leaf Leaves 等 Paper 下游 | 支持  | Fork如果完整保留对应实验性 Paper API，则支持此插件                               |

Paper 的 Registry Mutation API 标记为实验性，未来 Paper/Folia 更新可能改变 API 形态，如遇到问题请反馈。

## 3. 运行原理

### 附魔注册

插件在 bootstrap 阶段读取 `plugins/MeowsEnchants/enchants/*.yml`。
每个附魔都对应一个 YAML 文件并声明以下内容：

- 附魔 ID、显示名称、最大等级、铁砧成本、适用物品标签和冲突附魔
- 触发器，例如攻击、挖掘、右键、被动、潜行、重生等
- 目标，例如自身、玩家、生物或命中的实体
- 动作，例如额外伤害、药水效果、爆炸、连锁挖掘、自动补种、速度、命令等
- 附魔台与图书管理员交易中的独立出现规则

每个有效的 YAML 文件会被解析为一个 `EnchantConfig`，随后通过 Paper 的 `RegistryEvents.ENCHANTMENT.compose()` 注册进原版附魔注册表。注册时会设置：

- 附魔描述文本
- 附魔适用物品标签
- 附魔生效的装备槽
- 附魔最大等级
- 附魔铁砧成本
- 附魔台最小/最大附魔等级
- 附魔冲突集合

### 触发器

插件通过 Bukkit/Paper 事件监听玩家行为，并按附魔配置中的 `触发器` 执行动作。当前常用触发时机包括：

- 战斗：攻击实体、受到伤害、盾牌格挡、击杀实体
- 方块：破坏方块、左键挖掘方块、放置方块、右键交互
- 投射物：投射物命中实体或方块
- 状态：开始潜行、持续潜行、开始疾跑、持续疾跑、周期性被动触发
- 玩家生命周期和物品：死亡、重生、切换主副手、消耗物品
- 钓鱼：钓上鱼或实体、鱼咬钩

### 目标

动作执行前会根据配置中的 `目标` 解析附魔作用的目标。当前支持的目标语义包括：

- `触发者自身`
- `目标玩家`
- `目标怪物`
- `所有生物`
- `不指定目标`

### 动作

附魔触发时执行的动作由 `actions` 列表声明并按顺序执行。当前内置动作包括：

`potion_effect`, `bonus_damage`, `healthsteal`, `teleport`, `explosion`, `velocity`, `shockwave`, `break_armor`, `drop_head`, `attribute_modifier`, `multi_break`, `vacuum_drops`, `replant`, `multi_arrow`, `repair`, `hook`, `feast`, `deflect`, `till`, `reflect_damage`, `command`, `block_break`

详细动作参数不写在 README 中，请查看：

- [English: docs/action_types_en.md](docs/action_types_en.md)
- [中文版：docs/action_types_zh.md](docs/action_types_zh.md)

## 4. 插件使用方法

1. 下载插件 jar。
2. 将插件jar放入服务端 `plugins/` 目录。
3. 启动一次服务端，让插件生成 `plugins/MeowsEnchants/config.yml`。
4. 在 `plugins/MeowsEnchants/enchants/` 下创建每个自定义附魔的 YAML 文件。
5. 重启服务端。附魔配置发生变更后必须重启服务器才能生效。自定义附魔只能在 bootstrap 阶段注册，插件无法通过 reload 动态加入服务器注册表。
6. 所有成功注册的自定义附魔可以直接在创造模式物品栏，JEI/REI的附魔列表、配方查询中直接查看。

全局配置位于： `plugins/MeowsEnchants/config.yml`。 当前全局配置用于控制：

- Skyllia 兼容逻辑开关（用于使附魔遵循空岛保护机制）
- 附魔台概率是否考虑玩家等级（玩家等级将对附魔获取概率产生影响）
- 附魔书概率平衡（附魔书比武器装备更难获得自定义附魔）

附魔配置文件位于：

```text
plugins/MeowsEnchants/enchants/<enchant_id>.yml
```

## 5. 项目构建方法

项目使用 Maven 构建，依赖从 PaperMC Maven 仓库和 PlaceholderAPI 仓库解析。  
本项目 Java 版本为 `25`。构建前请确认本机 JDK 与 Maven 使用的 Java 版本匹配。  

在已安装 Maven 的环境中：

```bash
mvn clean package
```

构建产物位于：

```text
target/meowsenchants-0.1.jar
```
