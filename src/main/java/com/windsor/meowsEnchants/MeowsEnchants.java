package com.windsor.meowsEnchants;

import com.windsor.meowsEnchants.config.EnchantConfig;
import com.windsor.meowsEnchants.listeners.BlockDropItemListener;
import com.windsor.meowsEnchants.listeners.BlockInteractionListener;
import com.windsor.meowsEnchants.listeners.CombatListener;
import com.windsor.meowsEnchants.listeners.ConnectionListener;
import com.windsor.meowsEnchants.listeners.EnchantTableListener;
import com.windsor.meowsEnchants.listeners.EquipmentChangeListener;
import com.windsor.meowsEnchants.listeners.FallDamageListener;
import com.windsor.meowsEnchants.listeners.FishingListener;
import com.windsor.meowsEnchants.listeners.ItemListener;
import com.windsor.meowsEnchants.listeners.LifecycleListener;
import com.windsor.meowsEnchants.listeners.PassiveTaskManager;
import com.windsor.meowsEnchants.listeners.PlayerStateListener;
import com.windsor.meowsEnchants.listeners.ProjectileListener;
import com.windsor.meowsEnchants.listeners.VillagerTradeListener;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class MeowsEnchants extends JavaPlugin {

    private static MeowsEnchants instance;
    private static Map<Key, EnchantConfig> enchantConfigs = new HashMap<>();
    private static boolean skylliaCompatibilityEnabled;
    private static boolean enchantingTableUsePlayerLevel;
    private static double enchantingTablePlayerLevelFactor = 500.0;
    private static boolean enchantingTableBalanceBooks;

    public static MeowsEnchants getInstance() {
        return instance;
    }

    public static void setEnchantConfigs(Map<Key, EnchantConfig> configs) {
        enchantConfigs = configs;
    }

    public static Map<Key, EnchantConfig> getEnchantConfigs() {
        return enchantConfigs;
    }

    public static boolean isSkylliaCompatibilityEnabled() {
        return skylliaCompatibilityEnabled;
    }

    public static boolean isEnchantingTableUsePlayerLevel() {
        return enchantingTableUsePlayerLevel;
    }

    public static double getEnchantingTablePlayerLevelFactor() {
        return enchantingTablePlayerLevelFactor;
    }

    public static boolean isEnchantingTableBalanceBooks() {
        return enchantingTableBalanceBooks;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadRuntimeConfig();

        getServer().getPluginManager().registerEvents(new CombatListener(), this);
        getServer().getPluginManager().registerEvents(new BlockInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileListener(), this);
        getServer().getPluginManager().registerEvents(new FishingListener(), this);
        PlayerStateListener playerStateListener = new PlayerStateListener(this);
        getServer().getPluginManager().registerEvents(playerStateListener, this);
        getServer().getPluginManager().registerEvents(new LifecycleListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new FallDamageListener(), this);
        getServer().getPluginManager().registerEvents(new EquipmentChangeListener(this), this);
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantTableListener(), this);
        getServer().getPluginManager().registerEvents(new BlockDropItemListener(), this);

        Bukkit.getOnlinePlayers().forEach(player -> PassiveTaskManager.startTaskForPlayer(this, player));
    }

    @Override
    public void onDisable() {
        PassiveTaskManager.stopAll();
        instance = null;
    }

    private void reloadRuntimeConfig() {
        reloadConfig();
        skylliaCompatibilityEnabled = getConfig().getBoolean("skyllia_compatibility", false);
        enchantingTableUsePlayerLevel = getConfig().getBoolean("enchanting_table.use_player_level", false);
        enchantingTablePlayerLevelFactor = getConfig().getDouble("enchanting_table.player_level_factor", 500.0);
        if (enchantingTablePlayerLevelFactor <= 0) {
            enchantingTablePlayerLevelFactor = 500.0;
        }
        enchantingTableBalanceBooks = getConfig().getBoolean("enchanting_table.balance_books", false);
    }
}
