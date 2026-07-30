package com.windsor.meowsEnchants.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CombatListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttackEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK &&
                event.getCause() != EntityDamageByEntityEvent.DamageCause.PROJECTILE) return;

        // ✅ 正确：调用完整版本，传入 event
        EnchantTriggerHelper.checkAndTrigger(player, "ATTACK_ENTITY",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    @EventHandler
    public void onKillEntity(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player player = entity.getKiller();
        if (player == null) return;

        List<ItemStack> slots = EnchantTriggerHelper.getSlotsForTrigger(player);
        EnchantTriggerHelper.checkAndTrigger(player, "KILL_ENTITY", slots,
                event, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EnchantTriggerHelper.checkAndTrigger(player, "TAKE_DAMAGE",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShieldBlock(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;
        EnchantTriggerHelper.checkAndTrigger(player, "SHIELD_BLOCK",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }
}