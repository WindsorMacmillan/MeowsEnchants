package com.windsor.meowsEnchants.utils;

import org.bukkit.entity.Player;

public final class XpManager {

    private XpManager() {}

    /**
     * 从玩家扣除指定数量的经验值（使用 Paper API）。
     * 直接设置总经验值，不触发升级音效。
     */
    public static void takePlayerExp(Player player, int amount) {
        if (amount <= 0) return;
        int current = player.calculateTotalExperiencePoints();
        int newTotal = Math.max(0, current - amount);
        player.setExperienceLevelAndProgress(newTotal);
    }
}