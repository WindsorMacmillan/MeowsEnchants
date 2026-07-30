package com.windsor.meowsEnchants.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class PlaceholderAPIHook {

    private static BiFunction<OfflinePlayer, String, String> papiFunction = null;

    static {
        try {
            Plugin papiPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
            if (papiPlugin != null && papiPlugin.isEnabled()) {
                ClassLoader loader = papiPlugin.getClass().getClassLoader();
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI", true, loader);
                Method method = papiClass.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                papiFunction = (player, text) -> {
                    try {
                        return (String) method.invoke(null, player, text);
                    } catch (Exception e) {
                        return text;
                    }
                };
                Bukkit.getLogger().info("[MeowsEnchants] PlaceholderAPI 已成功挂钩。");
            } else {
                Bukkit.getLogger().warning("[MeowsEnchants] PlaceholderAPI 未加载，占位符功能不可用。");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MeowsEnchants] PlaceholderAPI 挂钩失败: " + e.getMessage());
        }
    }

    public static String setPlaceholders(OfflinePlayer player, String text) {
        if (papiFunction == null) return text;
        return papiFunction.apply(player, text);
    }

    public static boolean isAvailable() {
        return papiFunction != null;
    }
}