package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.XpManager;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;

import java.util.Map;
import java.util.Random;

public class RepairAction implements Action {
    private final ScalingFunction amount;
    private final boolean expMode;
    private final Random random = new Random();

    public RepairAction(Map<String, Object> params) {
        Object amountObj = params.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Missing 'amount' parameter for repair action");
        }
        this.amount = ScalingFunctionLoader.load(amountObj);

        this.expMode = params.containsKey("exp_mode") && (boolean) params.get("exp_mode");
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        Key enchantKey = context.getEnchantKey();
        if (enchantKey == null) return false;

        PlayerInventory inv = player.getInventory();
        ItemStack[] slots = {
                inv.getItemInMainHand(),
                inv.getItemInOffHand(),
                inv.getHelmet(),
                inv.getChestplate(),
                inv.getLeggings(),
                inv.getBoots()
        };
        boolean anyRepaired = false;

        for (ItemStack item : slots) {
            if (item == null || item.getType() == Material.AIR) continue;

            // 只修复带有该附魔的物品
            if (!hasEnchant(item, enchantKey)) continue;

            if (!(item.getItemMeta() instanceof Damageable meta)) continue;

            int currentDamage = meta.getDamage();
            int maxDurability = item.getType().getMaxDurability();
            if (currentDamage <= 0 || maxDurability <= 0) continue;

            double chance = this.amount.getValue(context.getEnchantLevel());
            if (chance <= 0 || random.nextDouble() >= chance) continue;

            // 经验模式检查（每次修复前检查）
            if (expMode) {
                if (player.calculateTotalExperiencePoints() < 1) {
                    break; // 经验不足，停止后续所有修复
                }
            }

            // 修复 1 点耐久
            meta.setDamage(currentDamage - 1);
            item.setItemMeta(meta);
            anyRepaired = true;

            // 扣除经验（如启用）
            if (expMode) {
                XpManager.takePlayerExp(player, 1);
            }
        }

        return anyRepaired;
    }

    /**
     * 检查物品是否包含指定的附魔。
     */
    private boolean hasEnchant(ItemStack item, Key enchantKey) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasEnchants()) return false;
        return item.getItemMeta().getEnchants().keySet().stream()
                .anyMatch(ench -> ench.getKey().equals(enchantKey));
    }
}