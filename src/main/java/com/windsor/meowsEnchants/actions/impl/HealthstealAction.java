package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;

/**
 * 生命偷取动作。
 * 根据对目标造成的伤害，按比例恢复玩家生命值。
 * 偷取比例限制在 0 ~ 1.0 之间（即不超过 100%）。
 * 注意：此动作不修改原始伤害值。
 */
public class HealthstealAction implements Action {
    private final ScalingFunction amount;

    public HealthstealAction(Map<String, Object> params) {
        Object amountObj = params.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Missing 'amount' parameter for healthsteal action");
        }
        this.amount = ScalingFunctionLoader.load(amountObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        // 必须为伤害事件
        if (!(context.getOriginalEvent() instanceof EntityDamageByEntityEvent event)) {
            return false;
        }

        // 如果事件已被取消，不执行偷取
        if (event.isCancelled()) {
            return false;
        }

        Player player = context.getPlayer();
        if (player == null) return false;

        // 获取目标实体
        if (!(event.getEntity() instanceof LivingEntity)) {
            return false;
        }

        // 计算偷取比例，限制在 0 ~ 1.0
        double stealFraction = amount.getValue(context.getEnchantLevel());
        if (stealFraction <= 0) return false;
        if (stealFraction > 1.0) stealFraction = 1.0; // 强制上限

        // 当前伤害值（原始伤害，未经修改）
        double currentDamage = event.getDamage();
        if (currentDamage <= 0) return false;

        // 偷取量 = 伤害 * 比例
        double stealAmount = currentDamage * stealFraction;

        // 治疗玩家（不修改伤害事件）
        player.heal(stealAmount);
        return true;
    }
}