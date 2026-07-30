package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ActionLoader;
import com.windsor.meowsEnchants.config.EnchantConfig;
import com.windsor.meowsEnchants.utils.SkylliaCompatibility;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantTriggerHelper {

    private static final Map<UUID, Map<Key, Long>> cooldowns = new ConcurrentHashMap<>();

    private EnchantTriggerHelper() {}
    // 禁止在主城区域触发的动作类名（不含包名）
    private static final Set<String> PROTECTED_ACTIONS = Set.of(
            "ExplosionAction",
            "HookAction",
            "MultiArrowAction",
            "MultiBreakAction",
            "PotionEffectAction",
            "ShockwaveAction",
            "TillAction",
            "VacuumDropsAction"
    );

    // ---------- 槽位获取 ----------
    public static List<ItemStack> getSlotsForTrigger(Player player) {
        PlayerInventory inv = player.getInventory();
        return List.of(
                inv.getItemInMainHand(),
                inv.getItemInOffHand(),
                inv.getHelmet(),
                inv.getChestplate(),
                inv.getLeggings(),
                inv.getBoots()
        );
    }

    // ---------- 附魔检测 ----------
    public static List<Key> getCustomEnchantsOnItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return Collections.emptyList();
        if (!item.hasItemMeta()) return Collections.emptyList();
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasEnchants()) return Collections.emptyList();
        List<Key> result = new ArrayList<>();
        for (org.bukkit.enchantments.Enchantment ench : meta.getEnchants().keySet()) {
            Key key = ench.getKey();
            if (key.namespace().equals("meowsenchants")) {
                result.add(key);
            }
        }
        return result;
    }

    public static Map<Key, EnchantConfig> getCustomEnchantsFromSlots(List<ItemStack> slots) {
        Map<Key, EnchantConfig> found = new HashMap<>();
        for (ItemStack item : slots) {
            if (item == null || item.getType() == Material.AIR) continue;
            for (Key key : getCustomEnchantsOnItem(item)) {
                EnchantConfig config = MeowsEnchants.getEnchantConfigs().get(key);
                if (config != null) {
                    found.put(key, config);
                }
            }
        }
        return found;
    }

    // ---------- 等级获取 ----------
    public static int getEnchantLevelFromSlots(List<ItemStack> slots, Key enchantKey) {
        int maxLevel = 0;
        for (ItemStack item : slots) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (!meta.hasEnchants()) continue;
            for (org.bukkit.enchantments.Enchantment ench : meta.getEnchants().keySet()) {
                if (ench.getKey().equals(enchantKey)) {
                    int level = meta.getEnchantLevel(ench);
                    if (level > maxLevel) maxLevel = level;
                }
            }
        }
        return maxLevel;
    }

    // ---------- 目标解析（基于事件上下文） ----------
    private static List<Entity> resolveTargets(ActionContext context, String targetType) {
        if (targetType == null || targetType.equalsIgnoreCase("NONE")) {
            return Collections.emptyList();
        }

        // 1. 从原始事件中提取“被命中的实体”作为候选者
        Entity candidate = null;
        Event event = context.getOriginalEvent();

        if (event instanceof EntityDamageByEntityEvent damageEvent) {
            candidate = damageEvent.getEntity();
        } else if (event instanceof ProjectileHitEvent projectileEvent) {
            candidate = projectileEvent.getHitEntity();
        } else if (event instanceof EntityDeathEvent deathEvent) {
            candidate = deathEvent.getEntity();
        }

        // 2. 如果没有候选者（如 PASSIVE, SNEAK_HOLD 等），默认作用于玩家自身
        if (candidate == null) {
            return Collections.singletonList(context.getPlayer());
        }

        // 3. 根据 targetType 过滤候选者
        return switch (targetType.toUpperCase()) {
            case "SELF" -> Collections.singletonList(context.getPlayer());
            case "PLAYERS" -> {
                if (candidate instanceof Player) {
                    yield Collections.singletonList(candidate);
                }
                yield Collections.emptyList();
            }
            case "MOBS" -> {
                if (candidate instanceof LivingEntity && !(candidate instanceof Player)) {
                    yield Collections.singletonList(candidate);
                }
                yield Collections.emptyList();
            }
            case "ALL_ENTITIES" -> {
                if (candidate.equals(context.getPlayer())) {
                    yield Collections.emptyList();
                }
                yield Collections.singletonList(candidate);
            }
            default -> Collections.emptyList();
        };
    }

    // ---------- 冷却管理 ----------
    private static Map<Key, Long> getPlayerCooldownMap(Player player) {
        return cooldowns.computeIfAbsent(player.getUniqueId(), _ -> new ConcurrentHashMap<>());
    }

    public static boolean isOnCooldown(Player player, Key enchantKey) {
        Map<Key, Long> map = getPlayerCooldownMap(player);
        Long end = map.get(enchantKey);
        return end != null && System.currentTimeMillis() < end;
    }

    public static void setCooldown(Player player, Key enchantKey, long cooldownMs) {
        if (cooldownMs <= 0) return;
        long end = System.currentTimeMillis() + cooldownMs;
        getPlayerCooldownMap(player).put(enchantKey, end);
    }

    public static void clearCooldownsForPlayer(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    // ---------- 调试消息 ----------
    public static void sendDebugMessage(Player player, Key enchantKey, EnchantConfig config, String triggerName) {
        // 将 displayName 中的 & 颜色代码转换为 Adventure Component
        Component displayNameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(config.getDisplayName());

        // 构建完整的消息组件
        Component message = Component.text()
                .append(Component.text("§e[MeowsEnchants] §f附魔 "))
                .append(displayNameComponent)
                .append(Component.text(" §f(等级 "))
                .append(Component.text(config.getMaxLevel()))
                .append(Component.text(") 在 §b"))
                .append(Component.text(triggerName))
                .append(Component.text(" §f触发器上触发！"))
                .build();

        //player.sendMessage(message);

        // 控制台日志使用纯文本（去除颜色）
        String plainDisplayName = PlainTextComponentSerializer.plainText().serialize(displayNameComponent);
        //Bukkit.getLogger().info("[MeowsEnchants] " + player.getName() + " 触发了 " + enchantKey.asString() + " (trigger: " + triggerName + ")");
    }

    /**
     * 检查玩家是否位于主城保护区域（0,0 ~ 512,512）
     */
    private static boolean isInProtectedRegion(Player player) {
        return SkylliaCompatibility.isInProtectedRegion(player);
    }

    // ---------- 核心触发检查（含动作执行） ----------
    public static void checkAndTrigger(Player player, String triggerName,
                                       List<ItemStack> slotsToCheck,
                                       Event originalEvent, Object extra) {
        if (player == null || slotsToCheck == null) return;

        Map<Key, EnchantConfig> found = getCustomEnchantsFromSlots(slotsToCheck);
        if (found.isEmpty()) return;

        for (Map.Entry<Key, EnchantConfig> entry : found.entrySet()) {
            Key key = entry.getKey();
            EnchantConfig config = entry.getValue();

            String configTrigger = config.getTrigger();
            if (configTrigger == null || !configTrigger.equalsIgnoreCase(triggerName)) {
                continue;
            }

            if (isOnCooldown(player, key)) {
                continue;
            }

            int enchantLevel = getEnchantLevelFromSlots(slotsToCheck, key);
            if (enchantLevel < 1) continue;

            // 获取目标类型（用于上下文）
            String targetType = config.getTarget();

            // 构建初始上下文（仅用于 resolveTargets）
            ActionContext baseContext = new ActionContext(
                    MeowsEnchants.getInstance(),
                    player,
                    key,    // 传入附魔键
                    enchantLevel,
                    triggerName,
                    Collections.emptyList(),
                    originalEvent,
                    extra,
                    targetType
            );

            // 解析目标
            List<Entity> targets = resolveTargets(baseContext, targetType);

            // 构建最终上下文
            ActionContext context = new ActionContext(
                    MeowsEnchants.getInstance(),
                    player,
                    key,
                    enchantLevel,
                    triggerName,
                    targets,
                    originalEvent,
                    extra,
                    targetType
            );

            // 加载并执行所有动作
            List<Action> actions = ActionLoader.loadActionsFromList(config.getActions());
            boolean allSuccess = true;

            // 如果玩家在主城区域，过滤掉受保护的动作
            if (isInProtectedRegion(player)) {
                actions = actions.stream()
                        .filter(action -> !PROTECTED_ACTIONS.contains(action.getClass().getSimpleName()))
                        .toList();
            }

            for (Action action : actions) {
                try {
                    boolean success = action.execute(context);
                    if (!success) {
                        allSuccess = false;
                        break; // 中断后续动作
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[MeowsEnchants] 执行动作时出错: " + e.getMessage());
                    e.printStackTrace();
                    allSuccess = false;
                    break;
                }
            }

            // 只有所有动作都成功执行，才发送调试消息并设置冷却
            if (allSuccess) {
                sendDebugMessage(player, key, config, triggerName);
                // 播放音效和粒子
                playEffects(player, config, context);
                setCooldown(player, key, config.getCooldown());
            }
        }
    }

    // ---------- 简化版本（不带事件上下文） ----------
    public static void checkAndTrigger(Player player, String triggerName, List<ItemStack> slotsToCheck) {
        checkAndTrigger(player, triggerName, slotsToCheck, null, null);
    }

    private static void playEffects(Player player, EnchantConfig config, ActionContext context) {
        // 音效
        EnchantConfig.SoundConfig sound = config.getSoundConfig();
        if (sound != null) {
            player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
        }

        // 粒子
        EnchantConfig.ParticleConfig particle = config.getParticleConfig();
        if (particle != null) {
            Location loc = getEffectLocation(particle.getLocation(), context).add(0,1,0);
            Object data = particle.getData();
            // 如果数据是 Material，说明需要延迟创建（BlockData 或 ItemStack）
            if (data instanceof Material material) {
                if (particle.getParticle().getDataType() == ItemStack.class) {
                    data = new ItemStack(material, 1);
                } else {
                    data = material.createBlockData();
                }
            }
            if (data != null) {
                loc.getWorld().spawnParticle(
                        particle.getParticle(),
                        loc,
                        particle.getCount(),
                        particle.getSpread(),
                        particle.getSpread(),
                        particle.getSpread(),
                        particle.getSpeed(),
                        data
                );
            } else {
                loc.getWorld().spawnParticle(
                        particle.getParticle(),
                        loc,
                        particle.getCount(),
                        particle.getSpread(),
                        particle.getSpread(),
                        particle.getSpread(),
                        particle.getSpeed()
                );
            }
        }
    }

    private static Location getEffectLocation(EnchantConfig.ParticleConfig.LocationType type, ActionContext context) {
        switch (type) {
            case TARGET: {
                List<Entity> targets = context.getTargets();
                if (targets != null && !targets.isEmpty()) {
                    return targets.getFirst().getLocation();
                }
                return context.getPlayer().getLocation(); // 回退
            }
            case BLOCK: {
                // 尝试从事件中获取方块
                Event event = context.getOriginalEvent();
                if (event instanceof BlockBreakEvent) {
                    return ((BlockBreakEvent) event).getBlock().getLocation().add(0.5, 0.5, 0.5);
                } else if (event instanceof BlockPlaceEvent) {
                    return ((BlockPlaceEvent) event).getBlock().getLocation().add(0.5, 0.5, 0.5);
                } else if (event instanceof PlayerInteractEvent) {
                    Block b = ((PlayerInteractEvent) event).getClickedBlock();
                    if (b != null) return b.getLocation().add(0.5, 0.5, 0.5);
                }
                return context.getPlayer().getLocation(); // 回退
            }
            case SELF:
            default: return context.getPlayer().getLocation();
        }
    }

    /**
     * 检查玩家身上（主手、副手、盔甲）是否包含配置了指定动作的附魔。
     */
    public static boolean hasActionOnPlayer(Player player, String actionType) {
        if (player == null) return false;
        // 获取玩家所有装备
        ItemStack[] slots = {
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand(),
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots()
        };
        for (ItemStack item : slots) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!item.hasItemMeta()) continue;
            for (org.bukkit.enchantments.Enchantment ench : item.getItemMeta().getEnchants().keySet()) {
                Key key = ench.getKey();
                EnchantConfig config = MeowsEnchants.getEnchantConfigs().get(key);
                if (config == null) continue;
                for (Map<String, Object> action : config.getActions()) {
                    if (actionType.equals(action.get("type"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查物品是否包含指定动作的附魔。
     */
    public static boolean hasActionOnItem(ItemStack item, String actionType) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        for (org.bukkit.enchantments.Enchantment ench : item.getItemMeta().getEnchants().keySet()) {
            Key key = ench.getKey();
            EnchantConfig config = MeowsEnchants.getEnchantConfigs().get(key);
            if (config == null) continue;
            for (Map<String, Object> action : config.getActions()) {
                if (actionType.equals(action.get("type"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
