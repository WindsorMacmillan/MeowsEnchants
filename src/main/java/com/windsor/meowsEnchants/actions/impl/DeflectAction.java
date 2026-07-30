package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;

public class DeflectAction implements Action {
    private final Random random = new Random();

    public DeflectAction(Map<String, Object> params) {
        // 无参数
    }

    @Override
    public boolean execute(ActionContext context) {
        if (!(context.getOriginalEvent() instanceof EntityDamageByEntityEvent event)) {
            return false;
        }

        Entity damager = event.getDamager();
        if (!(damager instanceof Projectile projectile)) {
            return false;
        }

        if (!(event.getEntity() instanceof Player)) {
            return false;
        }

        // 取消伤害
        event.setCancelled(true);

        // 获取当前速度
        Vector velocity = projectile.getVelocity();
        if (velocity.lengthSquared() == 0) {
            return false;
        }

        // 反向（大致方向）
        Vector reversed = velocity.clone().multiply(-1);

        // 随机偏转：水平方向随机旋转 0~360°，垂直方向随机旋转 -30°~30°
        double yaw = random.nextDouble() * 2 * Math.PI;
        double pitch = (random.nextDouble() - 0.5) * Math.PI / 3; // ±30°

        // 应用旋转
        Vector deflected = reversed.clone();
        deflected.rotateAroundY(yaw);
        // 构造水平旋转轴（垂直于当前方向）
        Vector axis = deflected.clone().crossProduct(new Vector(0, 1, 0));
        if (axis.lengthSquared() < 1e-10) {
            axis = deflected.clone().crossProduct(new Vector(1, 0, 0));
        }
        axis.normalize();
        deflected.rotateAroundAxis(axis, pitch);

        // 保持原速度大小
        deflected.normalize().multiply(velocity.length());
        projectile.setVelocity(deflected);
        return true;
    }
}