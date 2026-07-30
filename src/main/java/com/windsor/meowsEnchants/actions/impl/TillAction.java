package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SkylliaCompatibility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashMap;
import java.util.Map;

public class TillAction implements Action {
    private final ScalingFunction radius;
    private static final ThreadLocal<Boolean> processing = ThreadLocal.withInitial(() -> false);

    private static final Map<Material, Material> TRANSFORM_MAP = new HashMap<>();

    static {
        TRANSFORM_MAP.put(Material.DIRT, Material.FARMLAND);
        TRANSFORM_MAP.put(Material.GRASS_BLOCK, Material.FARMLAND);
        TRANSFORM_MAP.put(Material.PODZOL, Material.FARMLAND);
        TRANSFORM_MAP.put(Material.MYCELIUM, Material.FARMLAND);
        TRANSFORM_MAP.put(Material.COARSE_DIRT, Material.DIRT);
        TRANSFORM_MAP.put(Material.ROOTED_DIRT, Material.DIRT);
    }

    public TillAction(Map<String, Object> params) {
        Object radiusObj = params.get("radius");
        if (radiusObj == null) {
            throw new IllegalArgumentException("Missing 'radius' parameter for till action");
        }
        this.radius = ScalingFunctionLoader.load(radiusObj);
    }

    public static boolean isProcessing() {
        return processing.get();
    }

    @Override
    public boolean execute(ActionContext context) {
        if (processing.get()) {
            return false;
        }

        if (!(context.getOriginalEvent() instanceof PlayerInteractEvent originalEvent)) {
            return false;
        }
        if (originalEvent.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        Player player = context.getPlayer();
        if (player == null) return false;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return false;
        if (!tool.getType().name().contains("HOE")) {
            return false;
        }

        Block clickedBlock = originalEvent.getClickedBlock();
        if (clickedBlock == null) return false;

        int rad = (int) this.radius.getValue(context.getEnchantLevel());
        if (rad < 0) return false;

        // 取消原版耕地事件（由我们统一处理）
        originalEvent.setCancelled(true);

        Location center = clickedBlock.getLocation();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        World world = clickedBlock.getWorld();

        processing.set(true);
        try {
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    if (!SkylliaCompatibility.isInSameRegion(centerX, centerZ, x, z)) continue;

                    Block block = world.getBlockAt(x, center.getBlockY(), z);
                    Material original = block.getType();
                    Material target = TRANSFORM_MAP.get(original);
                    if (target == null) continue;

                    // 模拟 PlayerInteractEvent（锄头右键耕地）
                    PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                            player,
                            org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                            tool,
                            block,
                            BlockFace.UP,
                            EquipmentSlot.HAND
                    );
                    Bukkit.getPluginManager().callEvent(interactEvent);
                    if (interactEvent.isCancelled()) {
                        continue;
                    }

                    // 执行转化
                    block.setType(target);
                    // 消耗工具耐久
                    damageTool(tool, player);
                    // 缠根泥土掉落根
                    if (original == Material.ROOTED_DIRT) {
                        block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.HANGING_ROOTS, 1));
                    }
                }
            }
        } finally {
            processing.set(false);
        }
        return true;
    }

    private void damageTool(ItemStack tool, Player player) {
        if (tool == null || tool.getType().isAir()) return;
        if (!(tool.getItemMeta() instanceof Damageable)) return;
        tool.damage(1, player);
    }
}
