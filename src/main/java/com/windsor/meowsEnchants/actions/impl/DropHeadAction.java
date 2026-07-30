package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class DropHeadAction implements Action {
    private final ScalingFunction amount;
    private final Random random = new Random();

    public DropHeadAction(Map<String, Object> params) {
        Object amountObj = params.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Missing 'amount' parameter for drop_head action");
        }
        this.amount = ScalingFunctionLoader.load(amountObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        List<Entity> targets = context.getTargets();
        if (targets == null || targets.isEmpty()) return false;

        Entity target = targets.getFirst();
        if (target == null) return false;

        double chance = this.amount.getValue(context.getEnchantLevel());
        if (chance <= 0 || random.nextDouble() >= chance) return false;

        if (target instanceof Player playerTarget) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(playerTarget);
                head.setItemMeta(meta);
                target.getWorld().dropItemNaturally(target.getLocation(), head);
            }
            return false;
        }

        Material headMaterial = getHeadMaterial(target);
        if (headMaterial == null) return false;

        ItemStack head = new ItemStack(headMaterial, 1);
        target.getWorld().dropItemNaturally(target.getLocation(), head);
        return true;
    }

    private Material getHeadMaterial(Entity entity) {
        EntityType type = entity.getType();
        return switch (type) {
            case CREEPER -> Material.CREEPER_HEAD;
            case ZOMBIE -> Material.ZOMBIE_HEAD;
            case SKELETON -> Material.SKELETON_SKULL;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case PIGLIN -> Material.PIGLIN_HEAD;
            case ENDER_DRAGON -> Material.DRAGON_HEAD;
            default -> null;
        };
    }
}