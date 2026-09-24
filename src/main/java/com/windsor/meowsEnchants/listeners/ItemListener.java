package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class ItemListener implements Listener {

    private final MeowsEnchants plugin;

    public ItemListener(MeowsEnchants plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        SchedulerCompat.runDelayed(player, plugin,
                _ -> {
                    if (player.isOnline()) {
                        EnchantTriggerHelper.checkAndTrigger(player, "ITEM_SWAP",
                                EnchantTriggerHelper.getSlotsForTrigger(player),
                                event, null);
                    }
                },
                2L // 延迟 2 tick 确保主手更新
        );
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        EnchantTriggerHelper.checkAndTrigger(player, "CONSUME",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }
}
