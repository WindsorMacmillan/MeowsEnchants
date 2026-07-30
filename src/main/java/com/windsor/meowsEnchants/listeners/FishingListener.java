package com.windsor.meowsEnchants.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingListener implements Listener {

    @EventHandler
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH ||
                event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            EnchantTriggerHelper.checkAndTrigger(player, "FISHING",
                    EnchantTriggerHelper.getSlotsForTrigger(player),
                    event, null);
        } else if (event.getState() == PlayerFishEvent.State.BITE) {
            EnchantTriggerHelper.checkAndTrigger(player, "FISH_BITE",
                    EnchantTriggerHelper.getSlotsForTrigger(player),
                    event, null);
        }
    }
}