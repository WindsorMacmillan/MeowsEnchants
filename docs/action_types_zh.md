# 动作类型

所有动作类型可在附魔YAML配置文件中使用。每个动作定义在 `actions` 部分下，包含唯一键和 `type` 字段。

```yaml
actions:
  my_key:
    type: action_type_name
    # ... 参数
```

#### 通用 `exclude` 参数

任何动作类型均支持可选的 `exclude` 列表。若玩家的当前状态匹配列表中的任意条目，则该动作在此次触发中完全跳过。

```yaml
actions:
  my_key:
    type: multi_break
    exclude:
      - sneaking
      - sprinting
    # ... 其他参数
```

| 值         | 跳过条件               |
| ---------- | ---------------------- |
| `sneaking` | `player.isSneaking`   |
| `sprinting`| `player.isSprinting`  |
| `swimming` | `player.isSwimming`   |
| `flying`   | `player.isFlying`     |
| `gliding`  | `player.isGliding`    |

---

### potion_effect（药水效果）

对目标施加药水效果。

| 参数        | 类型            | 必须 | 默认值 | 描述                                       |
| ----------- | --------------- | ---- | ------ | ------------------------------------------ |
| `effect`    | string          | 是   | --     | 药水效果类型（小写，如 `poison`, `strength`, `slowness`） |
| `duration`  | ScalingFunction | 是   | --     | 持续时间（刻，20刻 = 1秒）                |
| `amplifier` | ScalingFunction | 是   | --     | 效果等级（0 = I级，1 = II级，依此类推）   |
| `particles` | boolean         | 否   | `true` | 是否显示粒子效果                           |
| `icon`      | boolean         | 否   | `true` | 是否在HUD上显示效果图标                    |

```yaml
actions:
  poison:
    type: potion_effect
    effect: poison
    duration:
      base: 40
      per_level: 20
    amplifier:
      scaling: fixed
      value: 0
    particles: true
    icon: true
```

---

### bonus_damage（额外伤害）

造成额外伤害或对现有伤害进行倍率加成。

| 参数                         | 类型            | 必须 | 默认值  | 描述                                         |
| ---------------------------- | --------------- | ---- | ------- | -------------------------------------------- |
| `amount`                     | ScalingFunction | 是   | --      | 伤害值（若 `multiplier` 为 true，则为倍率） |
| `multiplier`                 | boolean         | 否   | `false` | 若为 true，`amount` 作为现有伤害的倍率       |
| `conditions.sneaking`        | boolean         | 否   | `false` | 仅当潜行时激活                               |
| `conditions.night_time`      | boolean         | 否   | `false` | 仅当夜晚时激活                               |
| `conditions.low_health`      | double          | 否   | `0.0`   | 仅当玩家生命值低于此值时激活                 |
| `conditions.dimension`       | string          | 否   | --      | 仅当在此维度中激活（如 `the_nether`）       |
| `conditions.target_types`    | list            | 否   | --      | 仅对指定实体类型生效                         |
| `conditions.passive_mobs`    | boolean         | 否   | `false` | 仅对被动生物生效                             |

```yaml
actions:
  extra_dmg:
    type: bonus_damage
    amount:
      base: 2.0
      per_level: 1.0
    multiplier: false
    conditions:
      night_time: true
```

---

### healthsteal（生命偷取）

从目标偷取生命值并治疗玩家。

| 参数     | 类型            | 必须 | 默认值 | 描述                         |
| -------- | --------------- | ---- | ------ | ---------------------------- |
| `amount` | ScalingFunction | 是   | --     | 偷取伤害的比例（如 0.10 = 10%） |

```yaml
actions:
  steal:
    type: healthsteal
    amount:
      base: 0.1
      per_level: 0.1
```

---

### teleport（传送）

将玩家向视线方向传送。

| 参数    | 类型            | 必须 | 默认值 | 描述               |
| ------- | --------------- | ---- | ------ | ------------------ |
| `range` | ScalingFunction | 是   | --     | 最大传送距离（格） |

```yaml
actions:
  blink:
    type: teleport
    range:
      base: 5.0
      per_level: 3.0
```

---

### explosion（爆炸）

在目标位置生成爆炸。

| 参数           | 类型            | 必须 | 默认值  | 描述         |
| -------------- | --------------- | ---- | ------- | ------------ |
| `power`        | ScalingFunction | 是   | --      | 爆炸威力     |
| `fire`         | boolean         | 否   | `false` | 是否引发火焰 |
| `break_blocks` | boolean         | 否   | `false` | 是否破坏方块 |

```yaml
actions:
  boom:
    type: explosion
    power:
      base: 2.0
      per_level: 1.0
    fire: false
    break_blocks: false
```

---

### velocity（速度）

对玩家或目标施加速度（击退/发射）。

| 参数             | 类型            | 必须 | 默认值  | 描述                                                         |
| ---------------- | --------------- | ---- | ------- | ------------------------------------------------------------ |
| `direction`      | string          | 是   | --      | 方向：`UP`, `DOWN`, `FORWARD`, `BACKWARD`, `LOOK`, `AWAY`    |
| `power`          | ScalingFunction | 是   | --      | 速度大小                                                     |
| `apply_to`       | string          | 否   | `SELF`  | 作用于谁：`SELF` 或 `TARGET`                                |
| `no_fall_damage` | boolean         | 否   | `false` | 取消本次发射导致的摔落伤害                                   |

**方向说明：**

* `UP` / `DOWN` — 绝对垂直方向
* `FORWARD` / `BACKWARD` — 沿玩家视线水平方向
* `LOOK` — 同 `FORWARD`
* `AWAY` — 远离目标（需要已解析的目标）

```yaml
actions:
  launch:
    type: velocity
    direction: UP
    power:
      base: 0.8
      per_level: 0.3
    apply_to: SELF
    no_fall_damage: true
```

---

### shockwave（冲击波）

将附近实体推离玩家。

| 参数     | 类型            | 必须 | 默认值 | 描述           |
| -------- | --------------- | ---- | ------ | -------------- |
| `radius` | ScalingFunction | 是   | --     | 影响半径（格） |
| `power`  | ScalingFunction | 是   | --     | 击退力量       |

```yaml
actions:
  push:
    type: shockwave
    radius:
      base: 5.0
      per_level: 2.0
    power:
      base: 1.0
      per_level: 0.5
```

---

### break_armor（损坏盔甲）

随机损坏目标玩家的一件盔甲。

| 参数     | 类型            | 必须 | 默认值 | 描述               |
| -------- | --------------- | ---- | ------ | ------------------ |
| `damage` | ScalingFunction | 是   | --     | 耐久度伤害值       |

```yaml
actions:
  shatter:
    type: break_armor
    damage:
      base: 10
      per_level: 10
```

---

### drop_head（掉落头颅）

掉落被击杀实体的头颅。

| 参数     | 类型            | 必须 | 默认值 | 描述           |
| -------- | --------------- | ---- | ------ | -------------- |
| `amount` | ScalingFunction | 是   | --     | 掉落概率（0~1）|

```yaml
actions:
  head:
    type: drop_head
    amount:
      base: 0.01
      per_level: 0.01
```

---

### attribute_modifier（属性修饰符）

在装备物品时持续应用属性修饰符。通常与 `PASSIVE` 触发器配合使用。

| 参数         | 类型            | 必须 | 默认值 | 描述                                                                                             |
| ------------ | --------------- | ---- | ------ | ------------------------------------------------------------------------------------------------ |
| `attribute`  | string          | 是   | --     | 属性名称：`MAX_HEALTH`, `MOVEMENT_SPEED`, `ATTACK_DAMAGE`, `JUMP_STRENGTH`, `KNOCKBACK_RESISTANCE`, `ARMOR_TOUGHNESS` |
| `amount`     | ScalingFunction | 是   | --     | 修饰数值                                                                                         |
| `operation`  | string          | 是   | --     | 运算方式：`ADD_NUMBER`, `ADD_SCALAR`, 或 `MULTIPLY_SCALAR_1`                                      |
| `key`        | string          | 是   | --     | 唯一修饰键                                                                                       |

```yaml
actions:
  speed_boost:
    type: attribute_modifier
    keep_old_attribute: true
    attribute: "MOVEMENT_SPEED"
    amount:
      base: 0.02
      per_level: 0.01
    operation: "ADD_NUMBER"
    key: "speed"
```

**注意：** 卸载物品时修饰符会自动移除。

---

### multi_break（连锁挖掘）

围绕挖掘的方块，在方形半径内连锁破坏方块（3x3、5x5 图案）。

| 参数        | 类型            | 必须 | 默认值 | 描述                                                       |
| ----------- | --------------- | ---- | ------ | ---------------------------------------------------------- |
| `radius`    | ScalingFunction | 是   | --     | 破坏半径                                                   |
| `exclude`   | list            | 否   | `[]`   | 跳过动作的玩家状态（参见通用 `exclude` 参数）              |
| `conditions.sneaking` | boolean | 否   | `false` | 仅当潜行时激活                               |

```yaml
actions:
  excavate:
    type: multi_break
    exclude:
      - sneaking
    radius:
      base: 1
      per_level: 1
```

---

### vacuum_drops（真空收集）

取消自然方块掉落，并将掉落物直接送入玩家背包。放不下的物品掉落在方块位置。仅适用于 `BLOCK_BREAK` 触发器。

无参数。

```yaml
actions:
  collect:
    type: vacuum_drops
```

---

### replant（自动补种）

收获后自动补种作物。适用于小麦、胡萝卜、马铃薯、甜菜根和地狱疣。

| 参数         | 类型    | 必须 | 默认值  | 描述                                             |
| ------------ | ------- | ---- | ------- | ------------------------------------------------ |
| `fertilizer` | boolean | 否   | `false` | 若为 true，背包有至少5个骨粉时消耗骨粉并补种为成熟作物 |

```yaml
actions:
  plant:
    type: replant
    fertilizer: true
```

---

### multi_arrow（多重箭矢）

在主箭旁额外射出多支箭矢。

| 参数           | 类型            | 必须 | 默认值 | 描述               |
| -------------- | --------------- | ---- | ------ | ------------------ |
| `extra_arrows` | ScalingFunction | 是   | --     | 额外箭矢数量       |
| `spread`       | ScalingFunction | 是   | --     | 散布角度（度）     |

```yaml
actions:
  volley:
    type: multi_arrow
    extra_arrows:
      base: 1
      per_level: 1
    spread:
      base: 5
      per_level: 2
```

---

### repair（修复）

恢复物品的耐久度。

| 参数     | 类型            | 必须 | 默认值 | 描述                       |
| -------- | --------------- | ---- | ------ | -------------------------- |
| `amount` | ScalingFunction | 是   | --     | 每次触发修复1点耐久的概率（0~1） |

```yaml
actions:
  mending:
    type: repair
    amount:
      base: 0.4
      per_level: 0.2
    exp_mode: true
```

`exp_mode`（布尔，可选，默认 `false`）：若为 `true`，每次成功修复消耗1点经验值。

---

### hook（钩爪）

将目标实体拉向玩家（抓钩）。

| 参数    | 类型            | 必须 | 默认值 | 描述       |
| ------- | --------------- | ---- | ------ | ---------- |
| `power` | ScalingFunction | 是   | --     | 拉力强度   |

```yaml
actions:
  pull:
    type: hook
    power:
      base: 1.0
      per_level: 0.5
```

---

### feast（饱食）

恢复玩家的饥饿度和饱和度。

| 参数     | 类型            | 必须 | 默认值 | 描述                           |
| -------- | --------------- | ---- | ------ | ------------------------------ |
| `amount` | ScalingFunction | 是   | --     | 每次触发恢复1点饥饿值的概率（0~1） |

```yaml
actions:
  feed:
    type: feast
    amount:
      base: 0.2
      per_level: 0.2
```

---

### deflect（偏转）

偏转来袭的弹射物（箭矢等）。无参数。配合 `TAKE_DAMAGE` 触发器使用。

```yaml
actions:
  block:
    type: deflect
    amount:
      base: 0.2
      per_level: 0.2
```

`amount`（ScalingFunction，必须）：偏转概率（0~1）。

---

### gravity（引力）

将附近实体拉向玩家。

| 参数     | 类型            | 必须 | 默认值 | 描述       |
| -------- | --------------- | ---- | ------ | ---------- |
| `radius` | ScalingFunction | 是   | --     | 作用半径   |
| `power`  | ScalingFunction | 是   | --     | 拉力强度   |

```yaml
actions:
  pull:
    type: gravity
    radius:
      base: 5
      per_level: 2
    power:
      base: 0.5
      per_level: 0.2
```

---

### till（耕作）

使用锄头时，在半径内耕作泥土/草方块。

| 参数     | 类型            | 必须 | 默认值 | 描述       |
| -------- | --------------- | ---- | ------ | ---------- |
| `radius` | ScalingFunction | 是   | --     | 耕作半径   |

```yaml
actions:
  farm:
    type: till
    radius:
      base: 1
      per_level: 1
```

---

### reflect_damage（伤害反弹）

完全反弹近战伤害给攻击者。

| 参数         | 类型            | 必须 | 默认值 | 描述                   |
| ------------ | --------------- | ---- | ------ | ---------------------- |
| `percentage` | ScalingFunction | 是   | --     | 反弹伤害的概率（0~1）  |

```yaml
actions:
  reflect:
    type: reflect_damage
    percentage:
      base: 0.10
      per_level: 0.05
```

**注意：** 仅对近战伤害有效（需要 `EntityDamageByEntityEvent`）。

---

### command（命令）

以玩家或控制台身份运行服务器命令。

| 参数      | 类型   | 必须 | 默认值   | 描述                                       |
| --------- | ------ | ---- | -------- | ------------------------------------------ |
| `command` | string | 是   | --       | 要运行的命令（不含前导 `/`）。支持占位符。 |
| `run_as`  | string | 否   | `PLAYER` | 运行者：`PLAYER` 或 `CONSOLE`              |

**占位符：**

| 占位符      | 替换为                                   |
| ----------- | ---------------------------------------- |
| `{player}`  | 玩家名称                                 |
| `{target}`  | 目标实体名称（若无则为空字符串）         |
| `{level}`   | 附魔等级                                 |

```yaml
actions:
  announce:
    type: command
    run_as: CONSOLE
    command: "say {player} triggered the enchant at level {level}!"
```

```yaml
actions:
  reward:
    type: command
    run_as: CONSOLE
    command: "give {player} diamond 1"
```

**注意：** `PLAYER` 使用玩家自己的权限运行命令。如需管理员权限请使用 `CONSOLE`。

---

### send_message（发送消息）

向玩家或附近玩家发送 MiniMessage 格式的消息。

| 参数      | 类型   | 必须 | 默认值 | 描述                                                               |
| --------- | ------ | ---- | ------ | ------------------------------------------------------------------ |
| `message` | string | 是   | --     | MiniMessage 文本。支持 `{player}`, `{target}`, `{level}` 占位符。  |
| `radius`  | double | 否   | `0.0`  | 若 > 0，则广播给半径内所有玩家。                                   |

```yaml
actions:
  notify:
    type: send_message
    message: "<gold>{player}</gold> <gray>击中了</gray> <red>{target}</red> <gray>，附魔等级为 {level}！"
    radius: 0.0
```

**说明：**
- `{player}` 替换为附魔使用者的名称。
- `{target}` 替换为目标实体名称，若无则为空字符串。
- `{level}` 替换为附魔等级。
- 当 `radius > 0` 时，半径内所有在线玩家都会收到消息。

---

### durability（耐久损耗）

按配置量损坏指定装备槽的物品。尊重原版 Unbreaking 附魔的耐久损耗计算。

| 参数     | 类型            | 必须 | 默认值   | 描述                                                             |
| -------- | --------------- | ---- | -------- | ---------------------------------------------------------------- |
| `amount` | ScalingFunction | 是   | --       | 耐久损耗量（最小为1）                                            |
| `slot`   | string          | 否   | `HAND`   | 装备槽：`HAND`, `MAIN_HAND`, `OFF_HAND`, `HEAD`, `CHEST`, `LEGS`, `FEET` |
| `exclude`| list            | 否   | `[]`     | 跳过动作的玩家状态（参见通用 `exclude` 参数）                    |

```yaml
actions:
  durability_cost:
    type: durability
    exclude:
      - sneaking
    amount:
      base: 1
      per_level: 1
    slot: HAND
```

**注意：**
- `HAND` 和 `MAIN_HAND` 均指主手。
- 耐久损耗受 Unbreaking 附魔影响，实际消耗可能因物品的 Unbreaking 等级而减少。
- 若槽位为空或物品无法损坏，则不生效。
