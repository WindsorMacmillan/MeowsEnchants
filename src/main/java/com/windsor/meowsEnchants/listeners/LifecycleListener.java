package com.windsor.meowsEnchants.listeners;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class LifecycleListener implements Listener {

    private final MeowsEnchants plugin;

    public LifecycleListener(MeowsEnchants plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        EnchantTriggerHelper.checkAndTrigger(player, "DEATH",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    @EventHandler
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        Player player = event.getPlayer();
        // 使用 runDelayed 确保玩家完全准备就绪
        SchedulerCompat.runDelayed(player, plugin,
                scheduledTask -> {
                    if (player.isOnline()) {
                        EnchantTriggerHelper.checkAndTrigger(player, "RESPAWN",
                                EnchantTriggerHelper.getSlotsForTrigger(player),
                                event, null);
                    }
                },
                1L
        );
    }
}
