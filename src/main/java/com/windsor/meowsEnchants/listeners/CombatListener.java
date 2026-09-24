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

    /**
     * 攻击实体触发器：使用 MONITOR 优先级确保在所有保护插件处理后，
     * 若事件未被取消才触发附魔，避免附魔效果在事件被取消后仍生效。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onAttackEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
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

    /**
     * 受到伤害触发器：使用 MONITOR 优先级，理由同上。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onTakeDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        EnchantTriggerHelper.checkAndTrigger(player, "TAKE_DAMAGE",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    /**
     * 盾牌格挡触发器：使用 MONITOR 优先级，理由同上。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onShieldBlock(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.isBlocking()) return;
        EnchantTriggerHelper.checkAndTrigger(player, "SHIELD_BLOCK",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }
}