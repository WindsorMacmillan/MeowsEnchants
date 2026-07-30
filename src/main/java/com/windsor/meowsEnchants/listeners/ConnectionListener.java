package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    private final MeowsEnchants plugin;

    public ConnectionListener(MeowsEnchants plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 启动该玩家的 PASSIVE 任务
        PassiveTaskManager.startTaskForPlayer(plugin, event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 停止该玩家的 PASSIVE 任务
        PassiveTaskManager.stopTaskForPlayer(event.getPlayer());
    }
}