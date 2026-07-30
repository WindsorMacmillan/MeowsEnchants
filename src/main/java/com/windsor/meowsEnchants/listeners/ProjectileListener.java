package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ProjectileListener implements Listener {

    // 用于标记投射物的 NamespacedKey
    private static final NamespacedKey EXPLOSION_MARKER = new NamespacedKey(MeowsEnchants.getInstance(), "explosion_arrow");

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getProjectile() instanceof Projectile projectile)) return;

        // 检查主手和副手是否有包含 explosion 动作的附魔
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean hasExplosion = EnchantTriggerHelper.hasActionOnItem(mainHand, "explosion") ||
                EnchantTriggerHelper.hasActionOnItem(offHand, "explosion");

        if (hasExplosion) {
            // 标记投射物
            PersistentDataContainer pdc = projectile.getPersistentDataContainer();
            pdc.set(EXPLOSION_MARKER, PersistentDataType.BOOLEAN, true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        EnchantTriggerHelper.checkAndTrigger(player, "PROJECTILE_HIT",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    /**
     * 检查投射物是否由爆炸附魔标记。
     */
    public static boolean isExplosionProjectile(Projectile projectile) {
        if (projectile == null) return false;
        PersistentDataContainer pdc = projectile.getPersistentDataContainer();
        return pdc.has(EXPLOSION_MARKER, PersistentDataType.BOOLEAN);
    }
}