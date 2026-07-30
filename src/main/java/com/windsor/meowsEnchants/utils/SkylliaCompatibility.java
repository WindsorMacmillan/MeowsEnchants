package com.windsor.meowsEnchants.utils;

import com.windsor.meowsEnchants.MeowsEnchants;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class SkylliaCompatibility {

    public static final int REGION_SIZE = 512;

    private SkylliaCompatibility() {
    }

    public static boolean isEnabled() {
        return MeowsEnchants.isSkylliaCompatibilityEnabled();
    }

    public static boolean canUseIslandRestrictedAction(Player player) {
        if (!isEnabled()) {
            return true;
        }
        if (player.isOp()) {
            return true;
        }
        if (!PlaceholderAPIHook.isAvailable()) {
            return false;
        }
        String result = PlaceholderAPIHook.setPlaceholders(player, "%skyllia_visited_on_island%");
        return "true".equalsIgnoreCase(result);
    }

    public static boolean isInProtectedRegion(Player player) {
        if (!isEnabled() || player.isOp()) {
            return false;
        }
        Location loc = player.getLocation();
        double x = loc.getX();
        double z = loc.getZ();
        return x >= 0 && x <= REGION_SIZE && z >= 0 && z <= REGION_SIZE;
    }

    public static boolean isInSameRegion(int baseX, int baseZ, int x, int z) {
        if (!isEnabled()) {
            return true;
        }
        int baseRegionX = Math.floorDiv(baseX, REGION_SIZE);
        int baseRegionZ = Math.floorDiv(baseZ, REGION_SIZE);
        int regionX = Math.floorDiv(x, REGION_SIZE);
        int regionZ = Math.floorDiv(z, REGION_SIZE);
        return regionX == baseRegionX && regionZ == baseRegionZ;
    }

    public static double limitHorizontalDistanceToRegion(Location start, double dx, double dz, double maxDistance) {
        if (!isEnabled()) {
            return maxDistance;
        }
        double startX = start.getX();
        double startZ = start.getZ();
        int startRegionX = Math.floorDiv(start.getBlockX(), REGION_SIZE);
        int startRegionZ = Math.floorDiv(start.getBlockZ(), REGION_SIZE);

        double maxHoriz = Double.MAX_VALUE;
        if (dx > 0) {
            double boundaryX = (startRegionX + 1) * REGION_SIZE;
            double dist = (boundaryX - startX) / dx;
            if (dist > 0) maxHoriz = Math.min(maxHoriz, dist);
        } else if (dx < 0) {
            double boundaryX = startRegionX * REGION_SIZE;
            double dist = (boundaryX - startX) / dx;
            if (dist > 0) maxHoriz = Math.min(maxHoriz, dist);
        }
        if (dz > 0) {
            double boundaryZ = (startRegionZ + 1) * REGION_SIZE;
            double dist = (boundaryZ - startZ) / dz;
            if (dist > 0) maxHoriz = Math.min(maxHoriz, dist);
        } else if (dz < 0) {
            double boundaryZ = startRegionZ * REGION_SIZE;
            double dist = (boundaryZ - startZ) / dz;
            if (dist > 0) maxHoriz = Math.min(maxHoriz, dist);
        }
        return Math.min(maxDistance, maxHoriz);
    }
}
