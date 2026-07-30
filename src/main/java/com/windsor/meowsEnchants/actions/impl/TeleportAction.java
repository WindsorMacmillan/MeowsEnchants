package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SkylliaCompatibility;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;

public class TeleportAction implements Action {
    private final ScalingFunction range;

    public TeleportAction(Map<String, Object> params) {
        Object rangeObj = params.get("range");
        if (rangeObj == null) {
            throw new IllegalArgumentException("Missing 'range' parameter for teleport action");
        }
        this.range = ScalingFunctionLoader.load(rangeObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        if (!SkylliaCompatibility.canUseIslandRestrictedAction(player)) {
            return false;
        }

        double maxRange = this.range.getValue(context.getEnchantLevel());
        if (maxRange <= 0) return false;

        Location start = player.getLocation();
        float yaw = start.getYaw();
        float pitch = start.getPitch();
        World world = start.getWorld();
        if (world == null) return false;

        Vector direction = start.getDirection();
        double dx = direction.getX();
        double dy = direction.getY();
        double dz = direction.getZ();

        double startX = start.getX();
        double startY = start.getY();
        double startZ = start.getZ();
        double limitedRange = SkylliaCompatibility.limitHorizontalDistanceToRegion(start, dx, dz, maxRange);

        Location finalLoc = null;
        for (double dist = limitedRange; dist >= 0.5; dist -= 1.0) {
            double targetX = startX + dx * dist;
            double targetY = startY + dy * dist;
            double targetZ = startZ + dz * dist;
            Location candidate = new Location(world, targetX, targetY, targetZ, yaw, pitch);

            if (isGroundNearby(candidate) && isLocationSafe(candidate)) {
                finalLoc = candidate;
                break;
            }
        }

        if (finalLoc == null) return false;

        player.teleportAsync(finalLoc);
        return true;
    }

    private boolean isGroundNearby(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        int baseX = loc.getBlockX();
        int baseZ = loc.getBlockZ();
        int baseY = loc.getBlockY();
        for (int yOffset = 0; yOffset <= 4; yOffset++) {
            int checkY = baseY - yOffset;
            if (checkY < world.getMinHeight()) continue;
            if (!world.getBlockAt(baseX, checkY, baseZ).isEmpty()) return true;
        }
        return false;
    }

    private boolean isLocationSafe(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        int blockX = loc.getBlockX();
        int blockZ = loc.getBlockZ();
        int blockY = loc.getBlockY();
        if (!world.getBlockAt(blockX, blockY, blockZ).isEmpty()) return false;
        if (blockY + 1 > world.getMaxHeight()) return false;
        return world.getBlockAt(blockX, blockY + 1, blockZ).isEmpty();
    }
}
