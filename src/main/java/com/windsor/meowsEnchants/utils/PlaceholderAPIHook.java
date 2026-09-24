package com.windsor.meowsEnchants.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

public class PlaceholderAPIHook {

    private static volatile BiFunction<OfflinePlayer, String, String> papiFunction;

    private static synchronized void ensureHook() {
        if (papiFunction != null) return;

        try {
            Plugin papiPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
            if (papiPlugin == null || !papiPlugin.isEnabled()) return;

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
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MeowsEnchants] PlaceholderAPI 挂钩失败: " + e.getMessage());
        }
    }

    public static String setPlaceholders(OfflinePlayer player, String text) {
        ensureHook();
        BiFunction<OfflinePlayer, String, String> function = papiFunction;
        return function == null ? text : function.apply(player, text);
    }

    public static boolean isAvailable() {
        ensureHook();
        return papiFunction != null;
    }
}
