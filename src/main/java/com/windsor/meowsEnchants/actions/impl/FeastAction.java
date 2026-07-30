package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;

public class FeastAction implements Action {
    private final ScalingFunction amount;
    private final Random random = new Random();

    public FeastAction(Map<String, Object> params) {
        Object amountObj = params.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Missing 'amount' parameter for feast action");
        }
        this.amount = ScalingFunctionLoader.load(amountObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        // 检查饥饿度是否已满
        int foodLevel = player.getFoodLevel();
        if (foodLevel >= 20) {
            return false; // 已满，无需恢复
        }

        // 概率判定
        double chance = this.amount.getValue(context.getEnchantLevel());
        if (chance <= 0 || random.nextDouble() >= chance) {
            return false;
        }

        // 恢复 1 点饥饿值（上限 20）
        int newFood = Math.min(20, foodLevel + 1);
        player.setFoodLevel(newFood);

        // 恢复 1 点饱和度，但不超过当前饥饿值
        float saturation = player.getSaturation();
        float newSaturation = Math.min(newFood, saturation + 1);
        player.setSaturation(newSaturation);
        return true;
    }
}