package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.config.EnchantConfig;
import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class AttributeModifierHelper {

    private static NamespacedKey WAS_VANILLA_KEY = null;

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

    /**
     * 检查玩家是否位于主城保护区域（0,0 ~ 512,512）
     */
    private static boolean isInProtectedRegion(Player player) {
        Location loc = player.getLocation();
        double x = loc.getX();
        double z = loc.getZ();
        return x >= 0 && x <= 512 && z >= 0 && z <= 512;
    }

    private static NamespacedKey getWasVanillaKey(Plugin plugin) {
        if (WAS_VANILLA_KEY == null) {
            WAS_VANILLA_KEY = new NamespacedKey(plugin, "attr_was_vanilla");
        }
        return WAS_VANILLA_KEY;
    }

    public static void updateAllForPlayer(Player player, Plugin plugin) {
        if (player == null || plugin == null) return;
        for (Map.Entry<Key, EnchantConfig> entry : MeowsEnchants.getEnchantConfigs().entrySet()) {
            Key enchantKey = entry.getKey();
            EnchantConfig config = entry.getValue();
            for (Map<String, Object> actionParams : config.getActions()) {
                if (!"attribute_modifier".equals(actionParams.get("type"))) continue;
                String attrName = (String) actionParams.get("attribute");
                if (attrName == null) continue;
                NamespacedKey attrKey = NamespacedKey.minecraft(attrName.toLowerCase());
                Attribute attribute = Registry.ATTRIBUTE.get(attrKey);
                if (attribute == null) continue;

                ScalingFunction amount = ScalingFunctionLoader.load(actionParams.get("amount"));
                String opStr = (String) actionParams.get("operation");
                if (opStr == null) continue;
                AttributeModifier.Operation operation = getOperation(opStr);
                String key = (String) actionParams.get("key");
                if (key == null) continue;

                Map<String, Object> conditions = (Map<String, Object>) actionParams.get("conditions");
                boolean equipped = conditions != null && conditions.containsKey("equipped") && (boolean) conditions.get("equipped");

                applyModifierForPlayer(player, plugin, enchantKey, attribute, amount, operation, key, equipped);
            }
        }
    }

    public static void applyModifierForPlayer(Player player, Plugin plugin,
                                              Key enchantKey,
                                              Attribute attribute,
                                              ScalingFunction amount,
                                              AttributeModifier.Operation operation,
                                              String key,
                                              boolean equipped) {
        if (player == null || plugin == null) return;
        NamespacedKey modifierKey = new NamespacedKey(plugin, "attr_" + enchantKey.asString().replace(':', '_') + "_" + key);
        PlayerInventory inv = player.getInventory();
        ItemStack[] slots = {
                inv.getItemInMainHand(),
                inv.getItemInOffHand(),
                inv.getHelmet(),
                inv.getChestplate(),
                inv.getLeggings(),
                inv.getBoots()
        };

        EquipmentSlotGroup slotGroup = equipped ? EquipmentSlotGroup.ARMOR : EquipmentSlotGroup.ANY;

        for (ItemStack item : slots) {
            if (item == null || item.getType().isAir()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            // 获取附魔等级
            int enchantLevel = 0;
            if (meta.hasEnchants()) {
                for (org.bukkit.enchantments.Enchantment ench : meta.getEnchants().keySet()) {
                    if (ench.getKey().equals(enchantKey)) {
                        enchantLevel = meta.getEnchantLevel(ench);
                        break;
                    }
                }
            }

            // 移除旧修饰符
            Collection<AttributeModifier> existing = meta.getAttributeModifiers(attribute);
            if (existing != null) {
                for (AttributeModifier mod : new ArrayList<>(existing)) {
                    if (mod.getKey().equals(modifierKey)) {
                        meta.removeAttributeModifier(attribute, mod);
                    }
                }
            }

            if (enchantLevel > 0) {
                double value = amount.getValue(enchantLevel);
                if (value != 0) {
                    if (!meta.hasAttributeModifiers()) {
                        for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                            var defaults = item.getType().getDefaultAttributeModifiers(slot);
                            defaults.forEach((attr, mod) -> {
                                Collection<AttributeModifier> existingMods = meta.getAttributeModifiers(attr);
                                boolean alreadyPresent = existingMods != null && existingMods.stream()
                                        .anyMatch(m -> m.getKey().equals(mod.getKey()));
                                if (!alreadyPresent) {
                                    meta.addAttributeModifier(attr, mod);
                                }
                            });
                        }
                        meta.getPersistentDataContainer().set(
                                getWasVanillaKey(plugin),
                                org.bukkit.persistence.PersistentDataType.BOOLEAN,
                                true
                        );
                    }

                    meta.addAttributeModifier(attribute, new AttributeModifier(
                            modifierKey, value, operation, slotGroup));
                }
            } else {
                var remaining = meta.getAttributeModifiers();
                boolean hasOtherCustomModifiers = remaining != null
                        && remaining.values().stream()
                        .anyMatch(mod -> mod.getKey().getNamespace()
                                .equals(plugin.getName().toLowerCase()));

                if (!hasOtherCustomModifiers) {
                    NamespacedKey wasVanillaKey = getWasVanillaKey(plugin);
                    boolean wasVanilla = meta.getPersistentDataContainer().has(wasVanillaKey);

                    if (wasVanilla) {
                        meta.setAttributeModifiers(null);
                        meta.getPersistentDataContainer().remove(wasVanillaKey);
                    } else {
                        var allModifiers = meta.getAttributeModifiers();
                        if (allModifiers != null) {
                            for (Attribute attr : new HashSet<>(allModifiers.keySet())) {
                                Collection<AttributeModifier> mods = meta.getAttributeModifiers(attr);
                                if (mods == null) continue;
                                for (AttributeModifier mod : new ArrayList<>(mods)) {
                                    if (mod.getKey().getNamespace()
                                            .equals(plugin.getName().toLowerCase())) {
                                        meta.removeAttributeModifier(attr, mod);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item.setItemMeta(meta);
        }
    }

    private static AttributeModifier.Operation getOperation(String op) {
        return switch (op.toUpperCase()) {
            case "ADD_NUMBER" -> AttributeModifier.Operation.ADD_NUMBER;
            case "ADD_SCALAR" -> AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> throw new IllegalArgumentException("Unknown operation: " + op);
        };
    }
}