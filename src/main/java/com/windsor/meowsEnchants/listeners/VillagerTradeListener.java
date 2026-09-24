package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.config.EnchantConfig;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import io.papermc.paper.event.player.PlayerTradeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

public class VillagerTradeListener implements Listener {

    private static final String CUSTOM_NAMESPACE = "meowsenchants";
    private static final String VANILLA_NAMESPACE = "minecraft";
    private static final Component POLLUTED_TRADE_MESSAGE =
            LegacyComponentSerializer.legacyAmpersand().deserialize("&f[&b附魔&f] &c该村民交易已被污染！");

    private final Random random = new Random();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        if (!(originalResult.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return;
        }

        boolean hasEnabledCustomEnchant = false;
        for (Map.Entry<Enchantment, Integer> stored : meta.getStoredEnchants().entrySet()) {
            Enchantment enchantment = stored.getKey();
            if (!isCustomEnchantment(enchantment)) {
                continue;
            }

            EnchantConfig config = MeowsEnchants.getEnchantConfigs().get(Key.key(enchantment.getKey().toString()));
            if (config != null && config.getVillagerTradeConfig().isEnabled()) {
                hasEnabledCustomEnchant = true;
                continue;
            }

            MerchantRecipe replacement = createRandomVanillaBookTrade(originalRecipe);
            if (replacement != null) {
                event.setRecipe(replacement);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (hasEnabledCustomEnchant) {
            return;
        }

        rollConfiguredCustomTrade(originalRecipe, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTrade(PlayerTradeEvent event) {
        AbstractVillager merchant = event.getMerchant();
        if (!(merchant instanceof Villager villager)) {
            return;
        }
        MerchantRecipe trade = event.getTrade();
        ItemStack result = trade.getResult();
        if (result.getType() != Material.ENCHANTED_BOOK) {
            return;
        }
        if (!(result.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return;
        }

        for (Map.Entry<Enchantment, Integer> stored : meta.getStoredEnchants().entrySet()) {
            Enchantment enchantment = stored.getKey();
            if (!isCustomEnchantment(enchantment)) {
                continue;
            }

            EnchantConfig config = MeowsEnchants.getEnchantConfigs().get(Key.key(enchantment.getKey().toString()));
            if (config != null && config.getVillagerTradeConfig().isEnabled()) {
                continue;
            }

            event.setCancelled(true);
            if (!villager.isDead()) {
                villager.setHealth(0.0);
            } else {
                villager.remove();
            }
            event.getPlayer().sendMessage(POLLUTED_TRADE_MESSAGE);
            return;
        }
    }

    private MerchantRecipe createRandomVanillaBookTrade(MerchantRecipe originalRecipe) {
        List<Enchantment> candidates = getVanillaVillagerBookEnchantments();
        if (candidates.isEmpty()) {
            return null;
        }

        Collections.shuffle(candidates, random);
        Enchantment enchantment = candidates.getFirst();
        int level = random.nextInt(enchantment.getStartLevel(), enchantment.getMaxLevel() + 1);

        return createBookTrade(originalRecipe, enchantment, level, enchantment.isTreasure());
    }

    private void rollConfiguredCustomTrade(MerchantRecipe originalRecipe, VillagerAcquireTradeEvent event) {
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
            event.setRecipe(createBookTrade(originalRecipe, enchantment, level, tradeConfig.isTreasure()));
            return;
        }
    }

    private MerchantRecipe createBookTrade(MerchantRecipe originalRecipe,
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

        MerchantRecipe recipe = new MerchantRecipe(
                enchantedBook,
                originalRecipe.getUses(),
                originalRecipe.getMaxUses() > 0 ? originalRecipe.getMaxUses() : 12,
                originalRecipe.hasExperienceReward(),
                originalRecipe.getVillagerExperience(),
                originalRecipe.getPriceMultiplier(),
                originalRecipe.getDemand(),
                originalRecipe.getSpecialPrice()
        );
        recipe.addIngredient(new ItemStack(Material.EMERALD, price));
        return recipe;
    }

    private boolean isCustomEnchantment(Enchantment enchantment) {
        return enchantment != null && CUSTOM_NAMESPACE.equals(enchantment.getKey().getNamespace());
    }

    private boolean isVanillaNormalEnchantment(Enchantment enchantment) {
        return enchantment != null
                && VANILLA_NAMESPACE.equals(enchantment.getKey().getNamespace())
                && !enchantment.isTreasure()
                && !enchantment.isCursed();
    }

    private List<Enchantment> getVanillaVillagerBookEnchantments() {
        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        Set<Enchantment> nonTreasure;
        try {
            nonTreasure = new HashSet<>(registry.getTagValues(EnchantmentTagKeys.NON_TREASURE));
        } catch (Exception ignored) {
            nonTreasure = null;
        }

        List<Enchantment> result = new ArrayList<>();
        try {
            for (Enchantment enchantment : registry.getTagValues(EnchantmentTagKeys.TRADEABLE)) {
                if (!isVanillaNormalEnchantment(enchantment)) {
                    continue;
                }
                if (nonTreasure != null) {
                    if (nonTreasure.contains(enchantment)) {
                        result.add(enchantment);
                    }
                } else if (!enchantment.isTreasure()) {
                    result.add(enchantment);
                }
            }
        } catch (Exception ignored) {
            for (Enchantment enchantment : registry) {
                if (isVanillaNormalEnchantment(enchantment)) {
                    result.add(enchantment);
                }
            }
        }
        return result;
    }
}
