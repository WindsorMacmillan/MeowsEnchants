package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.actions.impl.VelocityAction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FallDamageListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (VelocityAction.shouldCancelFallDamage(player)) {
            event.setCancelled(true);
        }
    }
}