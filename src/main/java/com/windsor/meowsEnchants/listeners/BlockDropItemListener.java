package com.windsor.meowsEnchants.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class BlockDropItemListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (!hasVacuumDrops(player)) {
            return;
        }

        event.setCancelled(true);
        for (org.bukkit.entity.Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(stack);
            for (ItemStack leftover : remaining.values()) {
                if (leftover != null && leftover.getAmount() > 0) {
                    player.getWorld().dropItemNaturally(item.getLocation(), leftover);
                }
            }
            item.remove();
        }
    }

    /**
     * 检查玩家主手或副手是否持有包含 vacuum_drops 动作的附魔物品。
     */
    private boolean hasVacuumDrops(Player player) {
        return EnchantTriggerHelper.hasActionOnPlayer(player, "vacuum_drops");
    }
}