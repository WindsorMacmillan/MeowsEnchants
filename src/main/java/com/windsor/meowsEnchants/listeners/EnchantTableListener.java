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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;

public class EnchantTableListener implements Listener {

    private static final Logger logger = LoggerFactory.getLogger(EnchantTableListener.class);
    private final Random random = new Random();

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        Map<Enchantment, Integer> enchantsToAdd = event.getEnchantsToAdd();
        int cost = event.getExpLevelCost();
        int button = event.whichButton();

        // 调试信息：记录本次附魔事件的关键信息
        //logger.info("[MeowsEnchants] 附魔台事件: 按钮={}, 消耗={}, 物品={}",
        //        button, cost, item.getType());

        for (Map.Entry<Key, EnchantConfig> entry : MeowsEnchants.getEnchantConfigs().entrySet()) {
            Key enchantKey = entry.getKey();
            EnchantConfig config = entry.getValue();
            EnchantConfig.EnchantingTableConfig tableConfig = config.getEnchantingTableConfig();

            if (!tableConfig.isEnabled() || tableConfig.getChance() <= 0.0) {
                continue;
            }
            // 附魔消耗需在配置的 [min_cost, max_cost] 范围内
            if (cost < tableConfig.getMinCost() || cost > tableConfig.getMaxCost()) {
                //logger.info("[MeowsEnchants] 附魔{} 消耗{} 不在范围[{},{}]内，跳过",
                //        config.getId(), cost, tableConfig.getMinCost(), tableConfig.getMaxCost());
                continue;
            }

            Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(enchantKey);
            if (enchantment == null) {
                //logger.info("[MeowsEnchants] 附魔{} 未在注册表中找到，跳过", config.getId());
                continue;
            }
            if (item.getType() != Material.BOOK && !enchantment.canEnchantItem(item)) {
                //logger.info("[MeowsEnchants] 附魔{} 不适用于物品{}, 跳过", config.getId(), item.getType());
                continue;
            }
            if (enchantsToAdd.containsKey(enchantment)) {
                //logger.info("[MeowsEnchants] 附魔{} 已在原版结果中，跳过", config.getId());
                continue;
            }
            if (hasConflict(enchantment, enchantsToAdd)) {
                //logger.info("[MeowsEnchants] 附魔{} 与已有附魔冲突，跳过", config.getId());
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
                //logger.info("[MeowsEnchants] 附魔台覆写:{} {} -> {}，随机值{}/消耗{}",
                //        config.getId(), selectedLevel, event.getEnchanter().getName(),
                //        String.format("%.4f", randomNumber), cost);
            } else {
                //logger.info("[MeowsEnchants] 附魔{} 所有等级均未命中，随机值{}", config.getId(),
                //        String.format("%.4f", randomNumber));
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
