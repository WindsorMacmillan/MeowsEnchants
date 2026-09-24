package com.windsor.meowsEnchants.config;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import org.slf4j.Logger;

public class EnchantConfig {
    private final String id;
    private final String displayName;
    private final List<String> description;
    private final int maxLevel;
    private final int anvilCost;
    private final String applicableItem;
    private final String trigger;
    private final String target;
    private final long cooldown;
    private final List<Map<String, Object>> actions;
    private final List<String> conflictIds;
    private final Map<String, Object> soundData;
    private final Map<String, Object> particleData;
    private final EnchantingTableConfig enchantingTableConfig;
    private final VillagerTradeConfig villagerTradeConfig;

    private Logger logger;
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    // 延迟解析后的配置对象，在注册事件中填充
    private SoundConfig soundConfig;
    private ParticleConfig particleConfig;

    private TagKey<ItemType> itemTagKey;

    // ---- 内部配置类 ----
    public static class SoundConfig {
        private final Sound sound;
        private final float volume;
        private final float pitch;
        public SoundConfig(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
        public Sound getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
    }

    public static class ParticleConfig {
        private final Particle particle;
        private final int count;
        private final double spread;
        private final double speed;
        private final LocationType location;

        public enum LocationType { SELF, TARGET, BLOCK }
        private final Object data;

        public ParticleConfig(Particle particle, int count, double spread, double speed, LocationType location, Object data) {
            this.particle = particle;
            this.count = count;
            this.spread = spread;
            this.speed = speed;
            this.location = location;
            this.data = data;
        }
        public Particle getParticle() { return particle; }
        public int getCount() { return count; }
        public double getSpread() { return spread; }
        public double getSpeed() { return speed; }
        public LocationType getLocation() { return location; }
        public Object getData() { return data; }
    }

    public static class EnchantingTableConfig {
        private final boolean enabled;
        private final double chance;
        private final int minCost;
        private final int maxCost;

        public EnchantingTableConfig(boolean enabled, double chance, int minCost, int maxCost) {
            this.enabled = enabled;
            this.chance = Math.clamp(chance, 0.0, 1.0);
            this.minCost = Math.max(0, minCost);
            this.maxCost = Math.max(this.minCost, maxCost);
        }

        public boolean isEnabled() { return enabled; }
        public double getChance() { return chance; }
        public int getMinCost() { return minCost; }
        public int getMaxCost() { return maxCost; }
    }

    public static class VillagerTradeConfig {
        private final boolean enabled;
        private final double chance;
        private final boolean treasure;

        public VillagerTradeConfig(boolean enabled, double chance, boolean treasure) {
            this.enabled = enabled;
            this.chance = Math.clamp(chance, 0.0, 1.0);
            this.treasure = treasure;
        }

        public boolean isEnabled() { return enabled; }
        public double getChance() { return chance; }
        public boolean isTreasure() { return treasure; }
    }

    // ---- 构造方法 ----
    public EnchantConfig(@NotNull String id,
                         @NotNull String displayName,
                         @Nullable List<String> description,
                         int maxLevel,
                         int anvilCost,
                         @Nullable String applicableItem,
                         @Nullable String trigger,
                         @Nullable String target,
                         long cooldown,
                         @Nullable List<Map<String, Object>> actions,
                         @Nullable List<String> conflicts,
                         @Nullable Map<String, Object> soundData,
                         @Nullable Map<String, Object> particleData,
                         @Nullable EnchantingTableConfig enchantingTableConfig,
                         @Nullable VillagerTradeConfig villagerTradeConfig) {
        this.id = id;
        this.displayName = displayName;
        this.description = description != null ? List.copyOf(description) : Collections.emptyList();
        this.maxLevel = maxLevel;
        this.anvilCost = anvilCost;
        this.applicableItem = applicableItem;
        this.trigger = trigger != null ? trigger.toUpperCase(Locale.ROOT) : null;
        this.target = target != null ? target.toUpperCase(Locale.ROOT) : null;
        this.cooldown = Math.max(0, cooldown);
        this.actions = actions != null ? List.copyOf(actions) : Collections.emptyList();
        this.conflictIds = conflicts != null ? List.copyOf(conflicts) : Collections.emptyList();
        this.soundData = soundData;
        this.particleData = particleData;
        this.enchantingTableConfig = enchantingTableConfig != null
                ? enchantingTableConfig
                : new EnchantingTableConfig(false, 0.0, 0, 30);
        this.villagerTradeConfig = villagerTradeConfig != null
                ? villagerTradeConfig
                : new VillagerTradeConfig(false, 0.0, false);
    }

    // ---- Getters ----
    public @NotNull String getId() { return id; }
    public @NotNull String getDisplayName() { return displayName; }
    public @NotNull List<String> getDescription() { return description; }
    public @Nullable String getApplicableItem() { return applicableItem; }
    public int getMaxLevel() { return maxLevel; }
    public int getAnvilCost() { return anvilCost; }
    public double getEnchantingTableChance() { return enchantingTableConfig.getChance(); }
    public @NotNull EnchantingTableConfig getEnchantingTableConfig() { return enchantingTableConfig; }
    public @NotNull VillagerTradeConfig getVillagerTradeConfig() { return villagerTradeConfig; }
    public @Nullable String getTrigger() { return trigger; }
    public @Nullable String getTarget() { return target; }
    public long getCooldown() { return cooldown; }
    public long getCooldownTicks() { return Math.max(1, (cooldown + 49) / 50); }
    public @NotNull List<Map<String, Object>> getActions() { return actions; }
    public @NotNull List<String> getConflictIds() { return conflictIds; }
    public @Nullable SoundConfig getSoundConfig() { return soundConfig; }
    public @Nullable ParticleConfig getParticleConfig() { return particleConfig; }

    // ---- 延迟解析方法（注册或重新加载配置后调用） ----
    public void resolveSound() {
        if (soundData == null || logger == null) return;
        String typeStr = (String) soundData.get("type");
        if (typeStr == null) return;
        Sound sound = Registry.SOUNDS.get(Key.key(typeStr));
        if (sound == null) {
            logger.warn("[MeowsEnchants] 未知音效: " + typeStr);
            return;
        }
        float volume = ((Number) soundData.getOrDefault("volume", 1.0)).floatValue();
        float pitch = ((Number) soundData.getOrDefault("pitch", 1.0)).floatValue();
        this.soundConfig = new SoundConfig(sound, volume, pitch);
    }

    public void resolveParticle() {
        if (particleData == null || logger == null) return;
        String typeStr = (String) particleData.get("type");
        if (typeStr == null) return;

        // 解析粒子类型
        Particle particle = Registry.PARTICLE_TYPE.get(Key.key(typeStr.toLowerCase()));
        if (particle == null) {
            try {
                particle = Particle.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        if (particle == null) {
            logger.warn("[MeowsEnchants] 未知粒子类型: " + typeStr);
            return;
        }

        int count = ((Number) particleData.getOrDefault("count", 10)).intValue();
        double spread = ((Number) particleData.getOrDefault("spread", 0.5)).doubleValue();
        double speed = ((Number) particleData.getOrDefault("speed", 0.1)).doubleValue();
        String locStr = ((String) particleData.getOrDefault("location", "SELF")).toUpperCase();
        ParticleConfig.LocationType location;
        try {
            location = ParticleConfig.LocationType.valueOf(locStr);
        } catch (IllegalArgumentException e) {
            location = ParticleConfig.LocationType.SELF;
        }

        // 解析 data
        Object dataObj = null;
        Map<String, Object> dataMap = (Map<String, Object>) particleData.get("data");
        if (dataMap != null) {
            dataObj = parseParticleData(particle, dataMap);
        }

        this.particleConfig = new ParticleConfig(particle, count, spread, speed, location, dataObj);
    }

    /**
     * 根据粒子类型和配置数据生成对应的数据对象。
     * 支持所有带选项的粒子类型，使用 Particle.getDataType() 进行分发。
     */
    private Object parseParticleData(Particle particle, Map<String, Object> dataMap) {
        Class<?> dataType = particle.getDataType();
        if (dataType == null || dataType == Void.class) {
            return null;
        }

        // 1. BlockData 类 - 返回 Material 以供延迟创建
        if (dataType == BlockData.class) {
            String materialStr = (String) dataMap.get("material");
            if (materialStr == null) {
                logger.warn("[MeowsEnchants] 粒子 " + particle.name() + " 需要 material 参数");
                return null;
            }
            Material material = parseMaterial(materialStr);
            return material; // 返回 Material 而不是 BlockData
        }

        // 2. Color 类
        if (dataType == Color.class) {
            String colorStr = (String) dataMap.get("color");
            if (colorStr == null) {
                logger.warn("[MeowsEnchants] 粒子 " + particle.name() + " 需要 color 参数");
                return null;
            }
            return parseColor(colorStr);
        }

        // 3. DustOptions 类
        if (dataType == Particle.DustOptions.class) {
            String colorStr = (String) dataMap.get("color");
            Number sizeNum = (Number) dataMap.getOrDefault("size", 1.0);
            if (colorStr == null) {
                logger.warn("[MeowsEnchants] DUST 粒子需要 color 参数");
                return null;
            }
            Color color = parseColor(colorStr);
            if (color == null) return null;
            float size = sizeNum.floatValue();
            return new Particle.DustOptions(color, size);
        }

        // 4. DustTransition 类
        if (dataType == Particle.DustTransition.class) {
            String colorStr = (String) dataMap.get("color");
            String toColorStr = (String) dataMap.get("to_color");
            Number sizeNum = (Number) dataMap.getOrDefault("size", 1.0);
            if (colorStr == null || toColorStr == null) {
                logger.warn("[MeowsEnchants] DUST_COLOR_TRANSITION 需要 color 和 to_color 参数");
                return null;
            }
            Color color = parseColor(colorStr);
            Color toColor = parseColor(toColorStr);
            if (color == null || toColor == null) return null;
            float size = sizeNum.floatValue();
            return new Particle.DustTransition(color, toColor, size);
        }

        // 5. ItemStack 类 - 延迟创建：仅存 Material，运行时再转为 ItemStack
        if (dataType == ItemStack.class) {
            String materialStr = (String) dataMap.get("material");
            if (materialStr == null) {
                logger.warn("[MeowsEnchants] 粒子 " + particle.name() + " 需要 material 参数");
                return null;
            }
            Material material = parseMaterial(materialStr);
            if (material == null) return null;
            return material; // 返回 Material，运行时在 playEffects 中创建 ItemStack
        }

        // 6. Integer 类
        if (dataType == Integer.class) {
            Number intNum = (Number) dataMap.get("value");
            if (intNum == null) {
                logger.warn("[MeowsEnchants] 粒子 " + particle.name() + " 需要 value 参数");
                return null;
            }
            return intNum.intValue();
        }

        // 其他未支持的类型
        logger.warn("[MeowsEnchants] 暂不支持粒子数据类: " + dataType.getName() + " 对于 " + particle.name());
        return null;
    }

    private Material parseMaterial(String str) {
        Material material = Material.getMaterial(str.toUpperCase());
        if (material == null) {
            try {
                Key key = Key.key(str);
                material = Registry.MATERIAL.get(key);
            } catch (Exception ignored) {}
        }
        if (material == null) {
            logger.warn("[MeowsEnchants] 未知材质: " + str);
        }
        return material;
    }

    private Color parseColor(String hex) {
        if (hex == null) return null;
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return Color.fromRGB(r, g, b);
        } catch (Exception e) {
            logger.warn("[MeowsEnchants] 颜色格式错误: " + hex);
            return null;
        }
    }

    // ---- 冲突解析 ----
    public @NotNull RegistryKeySet<Enchantment> resolveConflictKeySet() {
        if (conflictIds.isEmpty()) {
            return RegistrySet.keySet(RegistryKey.ENCHANTMENT, Collections.emptyList());
        }
        Set<TypedKey<Enchantment>> typedKeys = new HashSet<>();
        Registry<Enchantment> registry = io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT);
        for (String id : conflictIds) {
            try {
                Key key = Key.key(id);
                Enchantment ench = registry.get(key);
                if (ench == null) {
                    if (logger != null) {
                        logger.warn("冲突附魔未注册，跳过: " + id);
                    }
                    continue;
                }
                typedKeys.add(TypedKey.create(RegistryKey.ENCHANTMENT, key));
            } catch (Exception e) {
                if (logger != null) {
                    logger.warn("冲突附魔ID格式错误，跳过: " + id);
                }
            }
        }
        if (typedKeys.isEmpty()) {
            return RegistrySet.keySet(RegistryKey.ENCHANTMENT, Collections.emptyList());
        }
        return RegistrySet.keySet(RegistryKey.ENCHANTMENT, typedKeys);
    }

    // ---- 物品标签解析 ----
    public @Nullable TagKey<ItemType> resolveItemTagKey() {
        if (itemTagKey != null) return itemTagKey;
        if (applicableItem == null) return null;
        itemTagKey = getTagKeyFromConfigItem(applicableItem);
        return itemTagKey;
    }

    @Nullable
    private TagKey<ItemType> getTagKeyFromConfigItem(@NotNull String item) {
        return switch (item.toUpperCase(Locale.ROOT)) {
            case "SWORDS" -> ItemTypeTagKeys.SWORDS;
            case "AXES" -> ItemTypeTagKeys.AXES;
            case "MACE" -> ItemTypeTagKeys.ENCHANTABLE_MACE;
            case "TRIDENT" -> ItemTypeTagKeys.ENCHANTABLE_TRIDENT;
            case "MELEE_WEAPON" -> ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON;
            case "SHARP_WEAPON" -> ItemTypeTagKeys.ENCHANTABLE_SHARP_WEAPON;
            case "WEAPON" -> ItemTypeTagKeys.ENCHANTABLE_WEAPON;
            case "PICKAXES" -> ItemTypeTagKeys.PICKAXES;
            case "SHOVELS" -> ItemTypeTagKeys.SHOVELS;
            case "HOES" -> ItemTypeTagKeys.HOES;
            case "BOW" -> ItemTypeTagKeys.ENCHANTABLE_BOW;
            case "CROSSBOW" -> ItemTypeTagKeys.ENCHANTABLE_CROSSBOW;
            case "RANGED" -> ItemTypeTagKeys.WITHER_SKELETON_DISLIKED_WEAPONS;
            case "TOOLS" -> ItemTypeTagKeys.ENCHANTABLE_MINING;
            case "FISHING_ROD" -> ItemTypeTagKeys.ENCHANTABLE_FISHING;
            case "HELMETS" -> ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR;
            case "CHESTPLATES" -> ItemTypeTagKeys.ENCHANTABLE_CHEST_ARMOR;
            case "LEGGINGS" -> ItemTypeTagKeys.ENCHANTABLE_LEG_ARMOR;
            case "BOOTS" -> ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR;
            case "ARMOR" -> ItemTypeTagKeys.ENCHANTABLE_ARMOR;
            case "EQUIPPABLE" -> ItemTypeTagKeys.ENCHANTABLE_EQUIPPABLE;
            default -> ItemTypeTagKeys.ENCHANTABLE_DURABILITY;
        };
    }
}
