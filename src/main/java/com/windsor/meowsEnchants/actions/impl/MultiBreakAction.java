package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.listeners.EnchantTriggerHelper;
import com.windsor.meowsEnchants.utils.SkylliaCompatibility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MultiBreakAction implements Action {
    private final ScalingFunction radius;
    private final Set<String> excludeStates;
    private final boolean sneakingRequired;
    private final boolean matchTool;

    // 线程本地标志，防止递归
    private static final ThreadLocal<Boolean> processing = ThreadLocal.withInitial(() -> false);

    public MultiBreakAction(Map<String, Object> params) {
        Object radiusObj = params.get("radius");
        if (radiusObj == null) {
            throw new IllegalArgumentException("Missing 'radius' parameter for multi_break action");
        }
        this.radius = ScalingFunctionLoader.load(radiusObj);

        this.excludeStates = new HashSet<>();
        Object excludeObj = params.get("exclude");
        if (excludeObj instanceof List) {
            for (Object o : (List<?>) excludeObj) {
                if (o instanceof String) {
                    excludeStates.add(((String) o).toLowerCase());
                }
            }
        }

        Map<String, Object> conditions = (Map<String, Object>) params.get("conditions");
        this.sneakingRequired = conditions != null && conditions.containsKey("sneaking") && (boolean) conditions.get("sneaking");

        this.matchTool = params.containsKey("match_tool") && (boolean) params.get("match_tool");
    }

    @Override
    public boolean execute(ActionContext context) {
        // 防止递归调用
        if (processing.get()) {
            return false;
        }

        Player player = context.getPlayer();
        if (player == null) return false;

        // 排除状态
        if (excludeStates.contains("sneaking") && player.isSneaking()) return false;
        if (excludeStates.contains("sprinting") && player.isSprinting()) return false;
        if (excludeStates.contains("swimming") && player.isSwimming()) return false;
        if (excludeStates.contains("flying") && player.isFlying()) return false;
        if (excludeStates.contains("gliding") && player.isGliding()) return false;

        if (sneakingRequired && !player.isSneaking()) return false;

        if (!(context.getOriginalEvent() instanceof BlockBreakEvent originalEvent)) {
            return false;
        }
        Block targetBlock = originalEvent.getBlock();

        int rad = (int) this.radius.getValue(context.getEnchantLevel());
        if (rad < 0) return false;
        if (rad == 0) return false;

        World world = targetBlock.getWorld();
        Location center = targetBlock.getLocation();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        boolean anyBroken = false;
        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean hasVacuum = EnchantTriggerHelper.hasActionOnPlayer(player, "vacuum_drops");

        // 设置处理标志
        processing.set(true);
        try {
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dy = -rad; dy <= rad; dy++) {
                    for (int dz = -rad; dz <= rad; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        int x = centerX + dx;
                        int y = centerY + dy;
                        int z = centerZ + dz;

                        if (!SkylliaCompatibility.isInSameRegion(centerX, centerZ, x, z)) continue;

                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == Material.AIR) continue;
                        if (block.getType().getHardness() < 0) continue;

                        if (matchTool && !isCorrectTool(block, tool)) {
                            continue;
                        }

                        // 触发 BlockBreakEvent 让保护插件拦截
                        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);
                        Bukkit.getPluginManager().callEvent(breakEvent);
                        if (breakEvent.isCancelled()) continue;

                        // 获取自然掉落物（考虑时运、精准采集）
                        Collection<ItemStack> drops = block.getDrops(tool);

                        // 提取容器类方块（箱子、漏斗、熔炉等）的储存物品
                        // 注意：潜影盒破坏时仅掉落自身（物品已含 NBT 内容），不应提取内部物品
                        if (!Tag.SHULKER_BOXES.isTagged(block.getType())
                                && block.getState() instanceof InventoryHolder holder) {
                            for (ItemStack stack : holder.getInventory().getContents()) {
                                if (stack != null && !stack.getType().isAir()) {
                                    drops.add(stack.clone());
                                }
                            }
                            holder.getInventory().clear();
                        }

                        // 处理掉落
                        if (hasVacuum) {
                            // 直接放入玩家背包，溢出掉落原地
                            for (ItemStack drop : drops) {
                                if (drop == null || drop.getAmount() <= 0) continue;
                                Map<Integer, ItemStack> remaining = player.getInventory().addItem(drop.clone());
                                for (ItemStack leftover : remaining.values()) {
                                    if (leftover != null && leftover.getAmount() > 0) {
                                        world.dropItemNaturally(block.getLocation(), leftover);
                                    }
                                }
                            }
                        } else {
                            // 自然掉落
                            for (ItemStack drop : drops) {
                                if (drop != null && drop.getAmount() > 0) {
                                    world.dropItemNaturally(block.getLocation(), drop);
                                }
                            }
                        }
                        block.setType(Material.AIR);
                        if (tool.getType().getMaxDurability() > 0) {
                            tool.damage(2, player);
                        }

                        anyBroken = true;
                    }
                }
            }
        } finally {
            // 清除处理标志
            processing.set(false);
        }
        return anyBroken;
    }

    /**
     * 使用原版 Tag 系统判断工具是否适合该方块。
     */
    private boolean isCorrectTool(Block block, ItemStack tool) {
        Material toolMat = tool.getType();
        if (Tag.MINEABLE_PICKAXE.isTagged(block.getType())) {
            return Tag.ITEMS_PICKAXES.isTagged(toolMat);
        } else if (Tag.MINEABLE_SHOVEL.isTagged(block.getType())) {
            return Tag.ITEMS_SHOVELS.isTagged(toolMat);
        } else if (Tag.MINEABLE_AXE.isTagged(block.getType())) {
            return Tag.ITEMS_AXES.isTagged(toolMat);
        } else if (Tag.MINEABLE_HOE.isTagged(block.getType())) {
            return Tag.ITEMS_HOES.isTagged(toolMat);
        } else {
            // 无特定工具要求（如泥土、沙子、农作物等）
            return true;
        }
    }

    /**
     * 供监听器检查当前是否正在处理中，用于二次防御（可选）。
     */
    public static boolean isProcessing() {
        return processing.get();
    }
}
