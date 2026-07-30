package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

public class HookAction implements Action {
    private final ScalingFunction power;

    public HookAction(Map<String, Object> params) {
        Object powerObj = params.get("power");
        if (powerObj == null) {
            throw new IllegalArgumentException("Missing 'power' parameter for hook action");
        }
        this.power = ScalingFunctionLoader.load(powerObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        List<Entity> targets = context.getTargets();
        if (targets == null || targets.isEmpty()) return false;

        Entity target = targets.getFirst();
        if (!(target instanceof LivingEntity)) return false;

        // 计算距离（3D距离）
        double distance = player.getLocation().distance(target.getLocation());
        if (distance <= 1.0) return false; // 太近不拉

        double powerValue = this.power.getValue(context.getEnchantLevel());
        if (powerValue <= 0) return false;

        // 根据距离计算动量大小
        double magnitude;
        if (distance >= 5.0) {
            magnitude = powerValue;
        } else {
            magnitude = powerValue / Math.pow(5.0 - distance, 2);
        }

        // 计算水平方向（从目标指向玩家，忽略Y）
        Vector horizontal = player.getLocation().toVector().subtract(target.getLocation().toVector());
        horizontal.setY(0);
        if (horizontal.lengthSquared() < 0.0001) {
            return false; // 水平方向无法确定，不施加动量
        }
        horizontal.normalize();

        // 俯仰角固定与水平面夹角45度，即水平分量和垂直分量各占 sqrt(2)/2
        double cos45 = Math.cos(Math.toRadians(45));
        double sin45 = Math.sin(Math.toRadians(45));
        Vector direction = horizontal.clone().multiply(cos45).add(new Vector(0, 1, 0).multiply(sin45));
        direction.normalize();

        Vector velocity = direction.multiply(magnitude);
        target.setVelocity(velocity);
        return true;
    }
}