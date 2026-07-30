package com.windsor.meowsEnchants.listeners;

import com.windsor.meowsEnchants.actions.impl.MultiBreakAction;
import com.windsor.meowsEnchants.actions.impl.TillAction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class BlockInteractionListener implements Listener {

    // 所有应被忽略的交互方块类型（右键点击时不应触发附魔）
    private static final Set<Material> INTERACTABLE_BLOCKS = new HashSet<>();

    static {
        // ---- 容器类（打开 GUI） ----
        INTERACTABLE_BLOCKS.add(Material.CHEST);
        INTERACTABLE_BLOCKS.add(Material.TRAPPED_CHEST);
        INTERACTABLE_BLOCKS.add(Material.ENDER_CHEST);
        INTERACTABLE_BLOCKS.add(Material.BARREL);
        INTERACTABLE_BLOCKS.addAll(Tag.SHULKER_BOXES.getValues());
        INTERACTABLE_BLOCKS.add(Material.FURNACE);
        INTERACTABLE_BLOCKS.add(Material.BLAST_FURNACE);
        INTERACTABLE_BLOCKS.add(Material.SMOKER);
        INTERACTABLE_BLOCKS.add(Material.BREWING_STAND);
        INTERACTABLE_BLOCKS.add(Material.HOPPER);
        INTERACTABLE_BLOCKS.add(Material.DISPENSER);
        INTERACTABLE_BLOCKS.add(Material.DROPPER);
        INTERACTABLE_BLOCKS.add(Material.LECTERN);
        INTERACTABLE_BLOCKS.add(Material.LOOM);
        INTERACTABLE_BLOCKS.add(Material.CARTOGRAPHY_TABLE);
        INTERACTABLE_BLOCKS.add(Material.SMITHING_TABLE);
        INTERACTABLE_BLOCKS.add(Material.STONECUTTER);
        INTERACTABLE_BLOCKS.add(Material.GRINDSTONE);
        INTERACTABLE_BLOCKS.add(Material.ANVIL);
        INTERACTABLE_BLOCKS.add(Material.CHIPPED_ANVIL);
        INTERACTABLE_BLOCKS.add(Material.DAMAGED_ANVIL);
        INTERACTABLE_BLOCKS.add(Material.ENCHANTING_TABLE);
        INTERACTABLE_BLOCKS.add(Material.CRAFTING_TABLE);
        INTERACTABLE_BLOCKS.add(Material.BEACON);

        // ---- 红石元件 ----
        INTERACTABLE_BLOCKS.addAll(Tag.BUTTONS.getValues());
        INTERACTABLE_BLOCKS.addAll(Tag.PRESSURE_PLATES.getValues());
        INTERACTABLE_BLOCKS.add(Material.LEVER);
        INTERACTABLE_BLOCKS.add(Material.TRIPWIRE_HOOK);
        INTERACTABLE_BLOCKS.add(Material.REPEATER);
        INTERACTABLE_BLOCKS.add(Material.COMPARATOR);
        INTERACTABLE_BLOCKS.add(Material.NOTE_BLOCK);
        INTERACTABLE_BLOCKS.add(Material.JUKEBOX);
        INTERACTABLE_BLOCKS.add(Material.DAYLIGHT_DETECTOR);

        // ---- 门、活板门、栅栏门 ----
        INTERACTABLE_BLOCKS.addAll(Tag.DOORS.getValues());
        INTERACTABLE_BLOCKS.addAll(Tag.TRAPDOORS.getValues());
        INTERACTABLE_BLOCKS.addAll(Tag.FENCE_GATES.getValues());

        // ---- 床 ----
        INTERACTABLE_BLOCKS.addAll(Tag.BEDS.getValues());

        // ---- 其他常用交互方块 ----
        INTERACTABLE_BLOCKS.add(Material.CAULDRON);
        INTERACTABLE_BLOCKS.add(Material.COMPOSTER);
        INTERACTABLE_BLOCKS.add(Material.RESPAWN_ANCHOR);
        INTERACTABLE_BLOCKS.add(Material.BELL);
        INTERACTABLE_BLOCKS.add(Material.CAKE);
        INTERACTABLE_BLOCKS.add(Material.TNT);
        INTERACTABLE_BLOCKS.add(Material.CAMPFIRE);
        INTERACTABLE_BLOCKS.add(Material.SOUL_CAMPFIRE);
        // 潜影盒已通过 Tag 加入
        // 箱子等已加入
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeftClickBlock(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;
        // 防止在方块被破坏后仍触发（但在 LEFT_CLICK_BLOCK 时方块还在）
        Player player = event.getPlayer();
        // 快速检查玩家是否有 block_break 动作，如果没有则直接返回（性能优化）
        if (!EnchantTriggerHelper.hasActionOnPlayer(player, "block_break")) {
            return;
        }
        // 获取被点击的方块
        Block block = event.getClickedBlock();
        if (block == null) return;

        // 传递方块作为 extra
        EnchantTriggerHelper.checkAndTrigger(player, "BLOCK_DIG",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, block); // extra 为 Block 对象
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // 如果 MultiBreakAction 正在处理中，跳过附魔触发，防止递归
        if (MultiBreakAction.isProcessing()) {
            return;
        }
        Player player = event.getPlayer();
        EnchantTriggerHelper.checkAndTrigger(player, "BLOCK_BREAK",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        EnchantTriggerHelper.checkAndTrigger(player, "BLOCK_PLACE",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    // 处理空中右键：ignoreCancelled = false（默认），允许被取消的事件仍触发
    @EventHandler
    public void onRightClickAir(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return;
        if (TillAction.isProcessing()) return;
        Player player = event.getPlayer();
        EnchantTriggerHelper.checkAndTrigger(player, "RIGHT_CLICK",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    // 处理方块右键：ignoreCancelled = true，高优先级，确保保护插件先处理
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClickBlock(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (TillAction.isProcessing()) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查是否应忽略该交互
        if (shouldIgnoreInteraction(block, item)) {
            return;
        }

        EnchantTriggerHelper.checkAndTrigger(player, "RIGHT_CLICK",
                EnchantTriggerHelper.getSlotsForTrigger(player),
                event, null);
    }

    /**
     * 判断是否应忽略本次右键点击（不触发附魔）。
     * 规则：
     * - 如果是可耕方块（泥土、草方块等）且手持锄头 → 不忽略（允许 TillAction 处理）
     * - 如果方块属于 INTERACTABLE_BLOCKS 集合 → 忽略
     * - 否则 → 不忽略
     */
    private boolean shouldIgnoreInteraction(Block block, ItemStack item) {
        if (block == null) return false;
        Material type = block.getType();

        // 特权：锄头犁地允许触发
        if (isTillable(type) && isHoe(item)) {
            return false;
        }

        return INTERACTABLE_BLOCKS.contains(type);
    }

    /**
     * 判断方块是否可被锄头犁地（转化为耕地）。
     */
    private boolean isTillable(Material type) {
        return type == Material.DIRT ||
                type == Material.GRASS_BLOCK ||
                type == Material.COARSE_DIRT ||
                type == Material.ROOTED_DIRT;
    }

    /**
     * 判断物品是否为锄头（通过物品名包含 "HOE"）。
     */
    private boolean isHoe(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        String name = item.getType().name();
        return name.contains("HOE");
    }
}