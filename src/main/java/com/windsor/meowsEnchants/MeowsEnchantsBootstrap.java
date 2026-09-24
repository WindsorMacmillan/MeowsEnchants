package com.windsor.meowsEnchants;

import com.windsor.meowsEnchants.config.EnchantConfig;
import com.windsor.meowsEnchants.config.EnchantConfigLoader;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MeowsEnchantsBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        Logger logger = context.getLogger();

        Path dir = EnchantConfigLoader.enchantDir();
        if (!Files.isDirectory(dir)) {
            try {
                EnchantConfigLoader.extractBundled(dir, logger);
            } catch (IOException e) {
                logger.warn("Failed to extract bundled enchant configs: " + e.getMessage());
            }
            if (!Files.exists(dir)) {
                logger.warn("Unable to create enchants directory: " + dir);
                return;
            }
        }

        Map<Key, EnchantConfig> configMap = EnchantConfigLoader.loadAll(dir, logger);
        if (configMap.isEmpty()) {
            logger.info("No valid enchantment configs to register.");
            return;
        }

        MeowsEnchants.setEnchantConfigs(configMap);

        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
                    for (Map.Entry<Key, EnchantConfig> entry : configMap.entrySet()) {
                        Key enchantKey = entry.getKey();
                        EnchantConfig config = entry.getValue();

                        TagKey<ItemType> tagKey = config.resolveItemTagKey();
                        if (tagKey == null) {
                            logger.warn("Enchant " + config.getId() + " has no usable vanilla item tag, skipping.");
                            continue;
                        }

                        Tag<ItemType> supportedTag;
                        try {
                            supportedTag = event.getOrCreateTag(tagKey);
                        } catch (Exception e) {
                            logger.error("Failed to get item tag: " + tagKey.key(), e);
                            continue;
                        }

                        Component displayName = MiniMessage.miniMessage().deserialize(config.getDisplayName());
                        TypedKey<Enchantment> typedKey = TypedKey.create(RegistryKey.ENCHANTMENT, enchantKey);

                        try {
                            event.registry().register(
                                    typedKey,
                                    builder -> {
                                        builder.description(displayName)
                                                .supportedItems(supportedTag)
                                                .activeSlots(EquipmentSlotGroup.ANY)
                                                .anvilCost(config.getAnvilCost())
                                                .maxLevel(config.getMaxLevel())
                                                .weight(1)
                                                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(30, 0))
                                                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(30, 0));

                                        RegistryKeySet<Enchantment> conflictSet = config.resolveConflictKeySet();
                                        if (!conflictSet.isEmpty()) {
                                            builder.exclusiveWith(conflictSet);
                                        }
                                    }
                            );

                            config.resolveSound();
                            config.resolveParticle();
                            logger.info("Registered custom enchantment: " + config.getId());
                        } catch (Exception e) {
                            logger.error("Failed to register enchant " + config.getId(), e);
                        }
                    }
                })
        );
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new MeowsEnchants();
    }
}
