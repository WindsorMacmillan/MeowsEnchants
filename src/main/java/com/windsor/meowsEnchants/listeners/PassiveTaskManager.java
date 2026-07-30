package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.actions.impl.AttributeModifierHelper;
import com.windsor.meowsEnchants.config.EnchantConfig;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PassiveTaskManager {

    private static final Map<UUID, SchedulerCompat.Task> passiveTasks = new HashMap<>();

    public static void startTaskForPlayer(MeowsEnchants plugin, Player player) {
        stopTaskForPlayer(player);
        UUID uuid = player.getUniqueId();

        // 计算所有被动附魔的最大冷却 tick
        long maxCooldown = getMaxCooldownTicksForPlayer(player);
        long period = Math.max(20, maxCooldown);
        long initialDelay = period;

        SchedulerCompat.Task task = SchedulerCompat.runAtFixedRate(player, plugin,
                scheduledTask -> {
                    if (!player.isOnline()) {
                        scheduledTask.cancel();
                        passiveTasks.remove(uuid);
                        return;
                    }
                    EnchantTriggerHelper.checkAndTrigger(player, "PASSIVE",
                            EnchantTriggerHelper.getSlotsForTrigger(player));
                    AttributeModifierHelper.updateAllForPlayer(player, plugin);
                },
                initialDelay,
                period
        );
        passiveTasks.put(uuid, task);
    }

    public static void stopTaskForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        SchedulerCompat.Task task = passiveTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static void stopAll() {
        for (SchedulerCompat.Task task : passiveTasks.values()) {
            task.cancel();
        }
        passiveTasks.clear();
    }

    private static long getMaxCooldownTicksForPlayer(Player player) {
        long max = 0;
        for (ItemStack item : EnchantTriggerHelper.getSlotsForTrigger(player)) {
            for (Key key : EnchantTriggerHelper.getCustomEnchantsOnItem(item)) {
                EnchantConfig config = MeowsEnchants.getEnchantConfigs().get(key);
                if (config != null && "PASSIVE".equalsIgnoreCase(config.getTrigger())) {
                    long ticks = config.getCooldownTicks();
                    if (ticks > max) max = ticks;
                }
            }
        }
        return max;
    }
}
