package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityAction implements Action {
    private final String direction;
    private final ScalingFunction power;
    private final String applyTo;
    private final boolean noFallDamage;
    private final boolean onGround;

    private static final int MAX_GROUND_DISTANCE = 1;
    private static final ConcurrentHashMap<UUID, Long> fallDamageProtection = new ConcurrentHashMap<>();

    public VelocityAction(Map<String, Object> params) {
        Object directionObj = params.get("direction");
        if (directionObj == null) {
            throw new IllegalArgumentException("Missing 'direction' parameter for velocity action");
        }
        this.direction = directionObj.toString().toUpperCase();

        Object powerObj = params.get("power");
        if (powerObj == null) {
            throw new IllegalArgumentException("Missing 'power' parameter for velocity action");
        }
        this.power = ScalingFunctionLoader.load(powerObj);

        this.applyTo = params.containsKey("apply_to") ? params.get("apply_to").toString().toUpperCase() : "SELF";
        this.noFallDamage = params.containsKey("no_fall_damage") && (boolean) params.get("no_fall_damage");
        Object onGroundObj = params.containsKey("onGround") ? params.get("onGround") : params.get("on_ground");
        this.onGround = onGroundObj == null || Boolean.parseBoolean(onGroundObj.toString());
    }

    @Override
    public boolean execute(ActionContext context) {
        try {
            Player player = context.getPlayer();
            if (player == null) return false;
            if (onGround && !isWithinGroundDistance(player)) return false;

            // 确定应用目标
            List<Entity> targets = context.getTargets();
            Entity targetEntity = (targets != null && !targets.isEmpty()) ? targets.getFirst() : null;

            Entity applyEntity;
            if (applyTo.equalsIgnoreCase("SELF")) {
                applyEntity = player;
            } else { // TARGET
                if (targetEntity == null) return false;
                applyEntity = targetEntity;
            }

            if (!(applyEntity instanceof LivingEntity)) return false;

            // 计算速度向量
            Vector velocity = calculateVelocity(player, targetEntity, applyEntity, context.getEnchantLevel());
            if (velocity.lengthSquared() == 0) return false;
            velocity.setY(velocity.getY() * 0.25);

            // 应用速度
            applyEntity.setVelocity(velocity);

            // 如果需要取消摔落伤害，且应用目标是玩家
            if (noFallDamage && applyEntity instanceof Player targetPlayer) {
                UUID uuid = targetPlayer.getUniqueId();
                fallDamageProtection.put(uuid, System.currentTimeMillis() + 5000);
                SchedulerCompat.runDelayed(targetPlayer, MeowsEnchants.getInstance(),
                        _ -> fallDamageProtection.remove(uuid),
                        100L);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private Vector calculateVelocity(Player player, Entity target, Entity applyEntity, int level) {
        double powerValue = this.power.getValue(level);
        if (powerValue <= 0) return new Vector(0, 0, 0);

        Vector directionVector;
        switch (direction) {
            case "UP":
                directionVector = new Vector(0, 1, 0);
                break;
            case "DOWN":
                directionVector = new Vector(0, -1, 0);
                break;
            case "FORWARD":
            case "LOOK": {
                Location eyeLoc = player.getEyeLocation();
                directionVector = eyeLoc.getDirection().clone();
                if (directionVector.lengthSquared() > 0) {
                    directionVector.normalize();
                } else {
                    directionVector = new Vector(0, 0, 1);
                }
                break;
            }
            case "BACKWARD": {
                Location eyeLoc = player.getEyeLocation();
                directionVector = eyeLoc.getDirection().clone().multiply(-1);
                if (directionVector.lengthSquared() > 0) {
                    directionVector.normalize();
                } else {
                    directionVector = new Vector(0, 0, -1);
                }
                break;
            }
            case "AWAY": {
                if (target == null) return new Vector(0, 0, 0);
                Vector fromTarget = applyEntity.getLocation().toVector().subtract(target.getLocation().toVector());
                fromTarget.setY(0);
                if (fromTarget.lengthSquared() > 0) {
                    fromTarget.normalize();
                } else {
                    return new Vector(0, 0, 0);
                }
                directionVector = fromTarget;
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported direction: " + direction);
        }

        return directionVector.multiply(powerValue);
    }

    private boolean isWithinGroundDistance(Player player) {
        if (player.isOnGround()) return true;

        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return false;

        int blockX = location.getBlockX();
        int blockZ = location.getBlockZ();
        int feetBlockY = location.getBlockY();
        for (int distance = 1; distance <= MAX_GROUND_DISTANCE; distance++) {
            int supportY = feetBlockY - distance;
            if (supportY < world.getMinHeight()) return false;
            if (!world.getBlockAt(blockX, supportY, blockZ).isPassable()) return true;
        }
        return false;
    }

    public static boolean shouldCancelFallDamage(Player player) {
        UUID uuid = player.getUniqueId();
        Long expiry = fallDamageProtection.get(uuid);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            fallDamageProtection.remove(uuid);
            return true;
        } else {
            fallDamageProtection.remove(uuid);
            return false;
        }
    }
}
