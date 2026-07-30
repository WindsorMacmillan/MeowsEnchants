package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ReplantAction implements Action {
    private final boolean fertilizer;

    public ReplantAction(Map<String, Object> params) {
        this.fertilizer = params.containsKey("fertilizer") && (boolean) params.get("fertilizer");
    }

    @Override
    public boolean execute(ActionContext context) {
        if (!(context.getOriginalEvent() instanceof BlockBreakEvent event)) {
            return false;
        }
        if (event.isCancelled()) {
            return false;
        }

        Block block = event.getBlock();
        CropType cropType = CropType.fromMaterial(block.getType());
        if (cropType == null) {
            return false;
        }
        if (!(block.getBlockData() instanceof Ageable)) {
            return false;
        }

        Player player = context.getPlayer();
        if (player == null) return false;
        if (!isSuitableSoil(block.getRelative(BlockFace.DOWN).getType(), cropType)) {
            return false;
        }

        Material seedMaterial = cropType.getSeedMaterial();
        if (!player.getInventory().contains(seedMaterial)) {
            return false;
        }

        Plugin plugin = context.getPlugin();
        if (plugin == null) return false;

        Material cropMaterial = cropType.getCropMaterial();
        SchedulerCompat.runDelayed(player, plugin, _ -> {
            if (!player.isOnline()) return;
            tryReplant(player, block, cropType, seedMaterial, cropMaterial);
        }, 3L);
        return true;
    }

    private void tryReplant(Player player, Block targetBlock, CropType cropType,
                            Material seedMaterial, Material cropMaterial) {
        Block currentBlock = targetBlock.getWorld().getBlockAt(targetBlock.getLocation());
        if (!currentBlock.isEmpty()) {
            return;
        }
        if (!isSuitableSoil(currentBlock.getRelative(BlockFace.DOWN).getType(), cropType)) {
            return;
        }
        if (!player.getInventory().contains(seedMaterial)) {
            return;
        }

        player.getInventory().removeItem(new ItemStack(seedMaterial, 1));

        BlockData cropData = cropMaterial.createBlockData();
        if (!(cropData instanceof Ageable ageable)) {
            return;
        }

        if (fertilizer && consumeBoneMeal(player.getInventory(), 5)) {
            ageable.setAge(ageable.getMaximumAge());
        } else {
            ageable.setAge(0);
        }

        currentBlock.setBlockData(ageable);
    }

    private boolean consumeBoneMeal(PlayerInventory inv, int amount) {
        int boneMealCount = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.BONE_MEAL) {
                boneMealCount += item.getAmount();
            }
        }
        if (boneMealCount < amount) {
            return false;
        }

        int toRemove = amount;
        ItemStack offhandItem = inv.getItemInOffHand();
        if (offhandItem.getType() == Material.BONE_MEAL) {
            int offhandAmount = offhandItem.getAmount();
            if (offhandAmount >= toRemove) {
                offhandItem.setAmount(offhandAmount - toRemove);
                toRemove = 0;
            } else {
                offhandItem.setAmount(0);
                toRemove -= offhandAmount;
            }
        }
        if (toRemove > 0) {
            inv.removeItem(new ItemStack(Material.BONE_MEAL, toRemove));
        }
        return true;
    }

    private boolean isSuitableSoil(Material belowType, CropType cropType) {
        if (cropType == CropType.NETHER_WART) {
            return belowType == Material.SOUL_SAND || belowType == Material.SOUL_SOIL;
        }
        return belowType == Material.FARMLAND;
    }

    private enum CropType {
        WHEAT(Material.WHEAT, Material.WHEAT_SEEDS),
        CARROTS(Material.CARROTS, Material.CARROT),
        POTATOES(Material.POTATOES, Material.POTATO),
        BEETROOTS(Material.BEETROOTS, Material.BEETROOT_SEEDS),
        NETHER_WART(Material.NETHER_WART, Material.NETHER_WART);

        private final Material cropMaterial;
        private final Material seedMaterial;

        CropType(Material cropMaterial, Material seedMaterial) {
            this.cropMaterial = cropMaterial;
            this.seedMaterial = seedMaterial;
        }

        public Material getSeedMaterial() {
            return seedMaterial;
        }

        public Material getCropMaterial() {
            return cropMaterial;
        }

        public static CropType fromMaterial(Material material) {
            for (CropType type : values()) {
                if (type.cropMaterial == material) {
                    return type;
                }
            }
            return null;
        }
    }
}
