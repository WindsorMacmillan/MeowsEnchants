package com.windsor.meowsEnchants;

import com.windsor.meowsEnchants.config.EnchantConfig;
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
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class MeowsEnchantsBootstrap implements PluginBootstrap {

    private static final String ENCHANTS_DIR = "plugins/MeowsEnchants/enchants";

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        Logger logger = context.getLogger();

        File dir = new File(ENCHANTS_DIR);
        if (!dir.exists()) {
            try {
                copyBundledEnchants(dir.toPath(), logger);
            } catch (IOException e) {
                logger.warn("Failed to extract bundled enchant configs: " + e.getMessage());
            }
            if (!dir.exists() && !dir.mkdirs()) {
                logger.warn("Unable to create enchants directory: " + ENCHANTS_DIR);
                return;
            }
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            logger.info("No custom enchantment config files found.");
            return;
        }

        Yaml yaml = new Yaml();
        Map<Key, EnchantConfig> configMap = new HashMap<>();

        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            try (InputStream input = new FileInputStream(file)) {
                Map<String, Object> data = yaml.load(input);
                if (data == null) {
                    logger.warn("Config file " + file.getName() + " is empty, skipping.");
                    continue;
                }

                String name = (String) data.get("name");
                Number maxLevelNum = (Number) data.get("max_level");
                Number anvilCostNum = (Number) data.get("anvil_cost");
                String applicable = parseApplicableItems(data.get("applicable_items"));
                String trigger = (String) data.get("trigger");
                String target = (String) data.get("target");
                long cooldown = getLong(data.get("cooldown"), 0L);
                List<Map<String, Object>> actionsList = parseActions(data.get("actions"));
                List<String> conflicts = parseStringList(data.get("conflicts"));
                Map<String, Object> soundData = asStringObjectMap(data.get("sound"));
                Map<String, Object> particleData = asStringObjectMap(data.get("particle"));
                EnchantConfig.EnchantingTableConfig enchantingTableConfig = parseEnchantingTableConfig(data);
                EnchantConfig.VillagerTradeConfig villagerTradeConfig = parseVillagerTradeConfig(data);

                if (name == null || maxLevelNum == null || anvilCostNum == null || applicable == null) {
                    logger.warn("Config file " + file.getName() + " is missing required fields, skipping.");
                    continue;
                }

                EnchantConfig config = new EnchantConfig(
                        id, name, maxLevelNum.intValue(), anvilCostNum.intValue(), applicable,
                        trigger, target, cooldown, actionsList, conflicts,
                        soundData, particleData, enchantingTableConfig, villagerTradeConfig
                );
                config.setLogger(logger);

                Key key = Key.key("meowsenchants", id);
                configMap.put(key, config);
                logger.info("Loaded enchantment config: " + id);
            } catch (Exception e) {
                logger.warn("Failed to read config file " + file.getName() + ": " + e.getMessage());
            }
        }

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

                            config.resolveSound(event);
                            config.resolveParticle(event);
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

    private static void copyBundledEnchants(Path targetDir, Logger logger) throws IOException {
        CodeSource codeSource = MeowsEnchantsBootstrap.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            Files.createDirectories(targetDir);
            logger.warn("Unable to locate plugin jar; created empty enchants directory.");
            return;
        }

        URI sourceUri;
        try {
            sourceUri = codeSource.getLocation().toURI();
        } catch (Exception e) {
            Files.createDirectories(targetDir);
            logger.warn("Unable to resolve plugin jar location; created empty enchants directory.");
            return;
        }

        Path sourcePath = Path.of(sourceUri);
        if (!Files.isRegularFile(sourcePath)) {
            Files.createDirectories(targetDir);
            logger.warn("Plugin code source is not a jar file; created empty enchants directory.");
            return;
        }

        boolean copiedAny = false;
        try (JarFile jarFile = new JarFile(sourcePath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("enchants/") || name.equals("enchants/") || entry.isDirectory()) {
                    continue;
                }

                Path relative = Path.of(name.substring("enchants/".length()));
                Path target = targetDir.resolve(relative).normalize();
                if (!target.startsWith(targetDir.normalize())) {
                    continue;
                }

                Files.createDirectories(target.getParent());
                try (InputStream input = jarFile.getInputStream(entry)) {
                    Files.copy(input, target);
                }
                copiedAny = true;
            }
        }

        if (copiedAny) {
            logger.info("Extracted bundled enchant configs to " + targetDir);
        } else {
            Files.createDirectories(targetDir);
            logger.warn("No bundled enchant configs found; created empty enchants directory.");
        }
    }

    private static String parseApplicableItems(Object rawApplicable) {
        if (rawApplicable instanceof String value) {
            return value;
        }
        if (rawApplicable instanceof List<?> list && !list.isEmpty()) {
            return list.getFirst().toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseActions(Object rawActions) {
        if (!(rawActions instanceof Map<?, ?> actionsMap)) {
            return null;
        }
        List<Map<String, Object>> actions = new ArrayList<>();
        for (Object value : actionsMap.values()) {
            if (value instanceof Map<?, ?>) {
                actions.add((Map<String, Object>) value);
            }
        }
        return actions;
    }

    private static List<String> parseStringList(Object rawList) {
        if (!(rawList instanceof List<?> list)) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (Object value : list) {
            if (value instanceof String string) {
                result.add(string);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringObjectMap(Object rawMap) {
        if (rawMap instanceof Map<?, ?>) {
            return (Map<String, Object>) rawMap;
        }
        return null;
    }

    private static long getLong(Object raw, long defaultValue) {
        return raw instanceof Number number ? number.longValue() : defaultValue;
    }

    private static EnchantConfig.EnchantingTableConfig parseEnchantingTableConfig(Map<String, Object> data) {
        boolean enabled = false;
        double chance = 0.0;
        int minCost = 0;
        int maxCost = 30;

        Object rawLegacyChance = data.get("enchanting_table_chance");
        if (rawLegacyChance instanceof Number number) {
            chance = number.doubleValue();
            enabled = chance > 0.0;
        }

        Object raw = data.get("enchanting_table");
        if (raw instanceof Map<?, ?> map) {
            Object rawEnabled = map.get("enabled");
            if (rawEnabled instanceof Boolean value) {
                enabled = value;
            }
            Object rawChance = map.get("chance");
            if (rawChance instanceof Number number) {
                chance = number.doubleValue();
            }
            Object rawMinCost = map.get("min_cost");
            if (rawMinCost instanceof Number number) {
                minCost = number.intValue();
            }
            Object rawMaxCost = map.get("max_cost");
            if (rawMaxCost instanceof Number number) {
                maxCost = number.intValue();
            }
        }

        return new EnchantConfig.EnchantingTableConfig(enabled, chance, minCost, maxCost);
    }

    private static EnchantConfig.VillagerTradeConfig parseVillagerTradeConfig(Map<String, Object> data) {
        boolean enabled = false;
        double chance = 0.0;
        boolean treasure = false;

        Object raw = data.get("villager_trades");
        if (raw instanceof Map<?, ?> map) {
            Object rawEnabled = map.get("enabled");
            if (rawEnabled instanceof Boolean value) {
                enabled = value;
            }
            Object rawChance = map.get("chance");
            if (rawChance instanceof Number number) {
                chance = number.doubleValue();
            }
            Object rawTreasure = map.get("treasure");
            if (rawTreasure instanceof Boolean value) {
                treasure = value;
            }
        }

        return new EnchantConfig.VillagerTradeConfig(enabled, chance, treasure);
    }
}
