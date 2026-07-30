package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 损坏目标玩家随机一件盔甲的耐久度。
 * 伤害值受 Unbreaking 附魔影响（通过 ItemStack#damage 方法）。
 */
public class BreakArmorAction implements Action {
    private final ScalingFunction damage;
    private static final Random RANDOM = new Random();

    public BreakArmorAction(Map<String, Object> params) {
        Object damageObj = params.get("damage");
        if (damageObj == null) {
            throw new IllegalArgumentException("Missing 'damage' parameter for break_armor action");
        }
        this.damage = ScalingFunctionLoader.load(damageObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        List<Entity> targets = context.getTargets();
        if (targets == null || targets.isEmpty()) return false;

        Entity targetEntity = targets.getFirst();
        if (!(targetEntity instanceof Player target)) return false;

        PlayerInventory inv = target.getInventory();

        // 收集所有非空盔甲物品及其对应的实际槽位（36=靴子, 37=护腿, 38=胸甲, 39=头盔）
        List<ArmorSlot> armorSlots = new ArrayList<>();
        // 槽位数组顺序：靴子(36), 护腿(37), 胸甲(38), 头盔(39)
        int[] slotIds = {36, 37, 38, 39};
        for (int slotId : slotIds) {
            ItemStack item = inv.getItem(slotId);
            if (item != null && !item.getType().isAir()) {
                armorSlots.add(new ArmorSlot(slotId, item));
            }
        }

        if (armorSlots.isEmpty()) return false;

        // 随机选择一个盔甲槽位
        ArmorSlot selected = armorSlots.get(RANDOM.nextInt(armorSlots.size()));
        int damageAmount = (int) this.damage.getValue(context.getEnchantLevel());
        if (damageAmount <= 0) return false;

        // 应用耐久度伤害（会考虑 Unbreaking 附魔）
        selected.item.damage(damageAmount, target);

        // 更新物品（虽然引用已修改，但为了保险，重新设置到槽位）
        inv.setItem(selected.slot, selected.item);
        return true;
    }

    /**
     * 辅助类，存储盔甲槽位及其物品。
     */
    private static class ArmorSlot {
        final int slot;
        final ItemStack item;

        ArmorSlot(int slot, ItemStack item) {
            this.slot = slot;
            this.item = item;
        }
    }
}