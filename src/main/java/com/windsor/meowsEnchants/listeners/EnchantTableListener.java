package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.config.EnchantConfig;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

public class EnchantTableListener implements Listener {

    private final Random random = new Random();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        int cost = event.getExpLevelCost();

        for (Map.Entry<Key, EnchantConfig> entry : MeowsEnchants.getEnchantConfigs().entrySet()) {
            Key enchantKey = entry.getKey();
            EnchantConfig config = entry.getValue();
            EnchantConfig.EnchantingTableConfig tableConfig = config.getEnchantingTableConfig();

            if (!tableConfig.isEnabled() || tableConfig.getChance() <= 0.0) {
                continue;
            }
            if (cost < tableConfig.getMinCost() || cost > tableConfig.getMaxCost()) {
                continue;
            }

            Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(enchantKey);
            if (enchantment == null) {
                continue;
            }
            if (item.getType() != Material.BOOK && !enchantment.canEnchantItem(item)) {
                continue;
            }
            if (enchantsToAdd.containsKey(enchantment)) {
                continue;
            }
            if (hasConflict(enchantment, enchantsToAdd)) {
                continue;
            }

            double randomNumber = random.nextDouble();
            int selectedLevel = 0;
            for (int level = config.getMaxLevel(); level >= 1; level--) {
                double finalChance = tableConfig.getChance() / Math.pow(2, level);
                if (MeowsEnchants.isEnchantingTableUsePlayerLevel()) {
                    finalChance *= event.getEnchanter().getLevel() / MeowsEnchants.getEnchantingTablePlayerLevelFactor();
                }
                if (MeowsEnchants.isEnchantingTableBalanceBooks() && item.getType() == Material.BOOK) {
                    finalChance /= 4.0;
                }
                if (randomNumber < finalChance) {
                    selectedLevel = level;
                    break;
                }
            }

            if (selectedLevel > 0) {
                enchantsToAdd.put(enchantment, selectedLevel);
            }
        }
    }

    private boolean hasConflict(Enchantment ench, Map<Enchantment, Integer> existing) {
        for (Enchantment existingEnch : existing.keySet()) {
            if (ench.conflictsWith(existingEnch) || existingEnch.conflictsWith(ench)) {
                return true;
            }
        }
        return false;
    }
}
