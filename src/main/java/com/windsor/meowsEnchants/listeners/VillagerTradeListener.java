package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.config.EnchantConfig;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VillagerTradeListener implements Listener {

    private final Random random = new Random();

    public VillagerTradeListener(Plugin plugin) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (villager.getProfession() != Villager.Profession.LIBRARIAN) {
            return;
        }

        MerchantRecipe originalRecipe = event.getRecipe();
        ItemStack originalResult = originalRecipe.getResult();
        if (originalResult.getType() != Material.ENCHANTED_BOOK) {
            return;
        }
        if (!(originalResult.getItemMeta() instanceof EnchantmentStorageMeta)) {
            return;
        }

        List<Map.Entry<Key, EnchantConfig>> candidates = new ArrayList<>();
        for (Map.Entry<Key, EnchantConfig> entry : MeowsEnchants.getEnchantConfigs().entrySet()) {
            EnchantConfig.VillagerTradeConfig tradeConfig = entry.getValue().getVillagerTradeConfig();
            if (tradeConfig.isEnabled() && tradeConfig.getChance() > 0.0) {
                candidates.add(entry);
            }
        }

        Collections.shuffle(candidates, random);
        for (Map.Entry<Key, EnchantConfig> entry : candidates) {
            EnchantConfig config = entry.getValue();
            EnchantConfig.VillagerTradeConfig tradeConfig = config.getVillagerTradeConfig();
            if (random.nextDouble() >= tradeConfig.getChance()) {
                continue;
            }

            Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(entry.getKey());
            if (enchantment == null) {
                continue;
            }

            int level = random.nextInt(config.getMaxLevel()) + 1;
            MerchantRecipe replacement = createCustomEnchantedBookTrade(originalRecipe, enchantment, level, tradeConfig.isTreasure());
            event.setRecipe(replacement);
            return;
        }
    }

    private MerchantRecipe createCustomEnchantedBookTrade(MerchantRecipe originalRecipe,
                                                          Enchantment enchantment,
                                                          int level,
                                                          boolean treasure) {
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();
        meta.addStoredEnchant(enchantment, level, true);
        enchantedBook.setItemMeta(meta);

        int baseMin = 2 + level * 3;
        int baseMax = 6 + level * 13;
        int price = baseMin + random.nextInt(baseMax - baseMin + 1);
        if (treasure) {
            price *= 2;
        }
        price = Math.clamp(price, 1, 64);

        int maxUses = originalRecipe.getMaxUses() > 0 ? originalRecipe.getMaxUses() : 12;
        MerchantRecipe recipe = new MerchantRecipe(enchantedBook, maxUses);
        recipe.addIngredient(new ItemStack(Material.EMERALD, price));
        recipe.setExperienceReward(originalRecipe.hasExperienceReward());
        recipe.setVillagerExperience(originalRecipe.getVillagerExperience());
        recipe.setPriceMultiplier(originalRecipe.getPriceMultiplier());
        recipe.setDemand(originalRecipe.getDemand());
        recipe.setSpecialPrice(originalRecipe.getSpecialPrice());
        recipe.setUses(0);
        return recipe;
    }
}
