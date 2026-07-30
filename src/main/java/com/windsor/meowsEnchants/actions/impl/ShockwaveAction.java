package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SkylliaCompatibility;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ShockwaveAction implements Action {
    private final ScalingFunction radius;
    private final ScalingFunction power;

    // 限制最大影响实体数量，防止性能崩溃
    private static final int MAX_ENTITIES = 64;
    // 限制最大动量，避免过大的碰撞计算
    private static final double MAX_MAGNITUDE = 16.0;

    public ShockwaveAction(Map<String, Object> params) {
        Object radiusObj = params.get("radius");
        if (radiusObj == null) {
            throw new IllegalArgumentException("Missing 'radius' parameter for shockwave action");
        }
        this.radius = ScalingFunctionLoader.load(radiusObj);

        Object powerObj = params.get("power");
        if (powerObj == null) {
            throw new IllegalArgumentException("Missing 'power' parameter for shockwave action");
        }
        this.power = ScalingFunctionLoader.load(powerObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        // 二重保险：检查玩家是否在岛屿上
        if (!SkylliaCompatibility.canUseIslandRestrictedAction(player)) {
            return false;
        }

        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        double rad = this.radius.getValue(context.getEnchantLevel());
        double pow = this.power.getValue(context.getEnchantLevel());
        if (rad <= 0 || pow <= 0) return false;

        // 获取半径内的所有实体，并按距离排序，只取最近的 MAX_ENTITIES 个
        Collection<Entity> nearby = world.getNearbyEntities(playerLoc, rad, rad, rad);
        List<Entity> sortedEntities = nearby.stream()
                .filter(entity -> !entity.equals(player) && entity instanceof LivingEntity)
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(playerLoc)))
                .limit(MAX_ENTITIES)
                .toList();

        // 限制最大动量
        double maxPower = Math.min(pow, MAX_MAGNITUDE);

        for (Entity entity : sortedEntities) {
            Location entityLoc = entity.getLocation();
            double distance = playerLoc.distance(entityLoc);
            if (distance > rad) continue;

            double magnitude;
            if (distance <= 1.0) {
                magnitude = maxPower;
            } else {
                double denominator = Math.pow(distance - 1.0, 2);
                if (denominator == 0) continue;
                magnitude = maxPower / denominator;
            }
            // 二次限制，防止个别极端值
            magnitude = Math.min(magnitude, MAX_MAGNITUDE);

            Vector horizontal = entityLoc.toVector().subtract(playerLoc.toVector());
            horizontal.setY(0);
            if (horizontal.lengthSquared() == 0) continue;
            horizontal.normalize();

            double sqrt2 = Math.sqrt(2);
            Vector direction = horizontal.clone().multiply(1.0 / sqrt2)
                    .add(new Vector(0, 1, 0).multiply(1.0 / sqrt2));
            direction.normalize();

            Vector velocity = direction.multiply(magnitude);
            entity.setVelocity(velocity);
        }
        return true;
    }

}
