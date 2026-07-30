package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;

public class MultiArrowAction implements Action {
    private final ScalingFunction extraArrows;
    private final ScalingFunction spread;
    private final Random random = new Random();

    public MultiArrowAction(Map<String, Object> params) {
        Object extraObj = params.get("extra_arrows");
        if (extraObj == null) {
            throw new IllegalArgumentException("Missing 'extra_arrows' parameter for multi_arrow action");
        }
        this.extraArrows = ScalingFunctionLoader.load(extraObj);

        Object spreadObj = params.get("spread");
        if (spreadObj == null) {
            throw new IllegalArgumentException("Missing 'spread' parameter for multi_arrow action");
        }
        this.spread = ScalingFunctionLoader.load(spreadObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        int extraCount = (int) Math.max(0, this.extraArrows.getValue(context.getEnchantLevel()));
        if (extraCount <= 0) return false;

        double spreadDeg = this.spread.getValue(context.getEnchantLevel());
        spreadDeg = Math.clamp(spreadDeg, 0, 60);

        Plugin plugin = context.getPlugin();
        if (plugin == null) return false;

        // 获取主箭速度
        double speed = 3.0; // 默认速度（回退值）
        Vector baseDirection = player.getEyeLocation().getDirection().normalize();

        // 从原始事件获取速度
        if (context.getOriginalEvent() instanceof EntityShootBowEvent bowEvent) {
            Projectile projectile = (Projectile) bowEvent.getProjectile();
            if (projectile instanceof Arrow) {
                speed = projectile.getVelocity().length();
            }
        }

        final double finalSpeed = speed;

        // 每 tick 射出一支额外箭矢
        for (int i = 0; i < extraCount; i++) {
            long delay = i + 1;
            double finalSpreadDeg = spreadDeg;
            SchedulerCompat.runDelayed(player, plugin, _ -> {
                if (!player.isOnline() || player.isDead()) return;

                Vector direction = generateRandomDirection(baseDirection, finalSpreadDeg);
                // 应用速度大小
                Vector velocity = direction.multiply(finalSpeed);
                // 发射箭矢并设置不可拾取
                Arrow arrow = player.launchProjectile(Arrow.class, velocity);
                arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            }, delay);
        }
        return true;
    }

    /**
     * 生成一个与给定方向夹角在 spreadDeg 度内的随机单位向量。
     */
    private Vector generateRandomDirection(Vector baseDir, double spreadDeg) {
        if (spreadDeg == 0) {
            return baseDir.clone().normalize();
        }

        double spreadRad = Math.toRadians(spreadDeg);
        double angle = random.nextDouble() * spreadRad;
        double azimuth = random.nextDouble() * 2 * Math.PI;

        Vector axis = baseDir.clone().crossProduct(new Vector(0, 1, 0));
        if (axis.lengthSquared() < 1e-10) {
            axis = baseDir.clone().crossProduct(new Vector(1, 0, 0));
        }
        axis.normalize();

        Vector rotated = baseDir.clone().rotateAroundAxis(axis, angle);
        rotated.rotateAroundAxis(baseDir, azimuth);
        return rotated.normalize();
    }
}
