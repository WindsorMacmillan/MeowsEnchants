package com.windsor.meowsEnchants.gui;

import com.windsor.meowsEnchants.MeowsEnchants;
import com.windsor.meowsEnchants.config.EnchantConfig;
import com.windsor.meowsEnchants.utils.TextUtil;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 附魔查询界面：6 行箱子 GUI，第一行中间槽位用于放入物品筛选附魔，2~5 行展示附魔书，最后一行翻页。
 */
public final class EnchantBrowser implements Listener {

    /** 界面总槽位数（6 行）。 */
    public static final int INVENTORY_SIZE = 54;
    /** 第一行用于查询的槽位。 */
    public static final int QUERY_SLOT = 4;
    /** 附魔列表起始槽位（第 2 行行首）。 */
    public static final int CONTENT_START_SLOT = 9;
    /** 单页可展示的附魔数量（2~5 行）。 */
    public static final int PAGE_SIZE = 36;
    /** 上一页按钮槽位。 */
    public static final int PREVIOUS_SLOT = 47;
    /** 下一页按钮槽位。 */
    public static final int NEXT_SLOT = 51;

    private static final Component TITLE = TextUtil.parse("<dark_aqua>附魔查询");

    private static final List<String> CATEGORY_ORDER = List.of(
            "PICKAXES", "SHOVELS", "HOES", "FISHING_ROD", "TOOLS", "AXES", "SWORDS", "MACE", "TRIDENT",
            "MELEE_WEAPON", "SHARP_WEAPON", "BOW", "CROSSBOW", "RANGED", "WEAPON", "HELMETS", "CHESTPLATES",
            "LEGGINGS", "BOOTS", "ARMOR", "EQUIPPABLE", "ALL");

    private static final Map<String, Integer> CATEGORY_INDEX = createCategoryIndex();
    private static final int DEFAULT_CATEGORY_INDEX = CATEGORY_ORDER.indexOf("ALL");

    private static final List<TagKey<ItemType>> TOOL_TAGS = List.of(
            ItemTypeTagKeys.PICKAXES,
            ItemTypeTagKeys.SHOVELS,
            ItemTypeTagKeys.HOES,
            ItemTypeTagKeys.ENCHANTABLE_MINING,
            ItemTypeTagKeys.ENCHANTABLE_FISHING);

    private static final List<TagKey<ItemType>> WEAPON_TAGS = List.of(
            ItemTypeTagKeys.SWORDS,
            ItemTypeTagKeys.ENCHANTABLE_MACE,
            ItemTypeTagKeys.ENCHANTABLE_TRIDENT,
            ItemTypeTagKeys.ENCHANTABLE_MELEE_WEAPON,
            ItemTypeTagKeys.ENCHANTABLE_SHARP_WEAPON,
            ItemTypeTagKeys.ENCHANTABLE_WEAPON,
            ItemTypeTagKeys.ENCHANTABLE_BOW,
            ItemTypeTagKeys.ENCHANTABLE_CROSSBOW,
            ItemTypeTagKeys.WITHER_SKELETON_DISLIKED_WEAPONS);

    private static final List<TagKey<ItemType>> ARMOR_TAGS = List.of(
            ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR,
            ItemTypeTagKeys.ENCHANTABLE_CHEST_ARMOR,
            ItemTypeTagKeys.ENCHANTABLE_LEG_ARMOR,
            ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR,
            ItemTypeTagKeys.ENCHANTABLE_ARMOR,
            ItemTypeTagKeys.ENCHANTABLE_EQUIPPABLE);

    private final MeowsEnchants plugin;

    public EnchantBrowser(@NotNull MeowsEnchants plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开查询界面。命令执行线程即为玩家所在区域线程，无需额外调度。
     */
    public void open(@NotNull Player player, int page) {
        EnchantBrowserHolder holder = new EnchantBrowserHolder(player.getUniqueId(), page);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, TITLE);
        holder.setInventory(inventory);
        render(holder);
        InventoryView current = player.getOpenInventory();
        if (current.getTopInventory().getHolder() instanceof EnchantBrowserHolder) {
            returnQueryItem(player, current.getTopInventory());
        }
        player.openInventory(inventory);
    }

    /**
     * 插件卸载时归还查询槽内的物品，避免物品随界面一起丢失。
     * <p>
     * Folia 下读取或关闭其它区域玩家的容器会触发线程检查异常，因此这里只处理当前线程
     * 持有的区域；其余玩家残留的界面由 {@link #cleanupStaleViews()} 在插件重新启用后清理，
     * 若插件不再启用，玩家也可以直接从残留界面中把物品取回。
     */
    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Bukkit.isOwnedByCurrentRegion(player)) {
                closeOwnedView(player);
            }
        }
    }

    /**
     * 清理热重载后残留的旧界面。
     * <p>
     * 插件卸载时不会自动关闭插件创建的容器：服务端热重载后旧界面仍然打开，但旧类加载器里的
     * Holder 无法被新实例通过 {@code instanceof} 识别（点击监听也已失效）。这里按类名识别这类
     * 残留界面，先归还查询槽内的物品再关闭，避免物品丢失或界面处于失效状态。
     * <p>
     * 必须在插件启用后调用，且会切回各玩家所属区域执行。
     */
    public void cleanupStaleViews() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (Bukkit.isOwnedByCurrentRegion(player)) {
                closeStaleView(player);
            } else {
                player.getScheduler().execute(plugin, () -> closeStaleView(player), null, 0L);
            }
        }
    }

    /** 归还查询槽物品并关闭界面，必须在玩家所属区域的线程上调用。 */
    private void closeOwnedView(@NotNull Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof EnchantBrowserHolder holder
                && holder.getOwnerId().equals(player.getUniqueId())) {
            returnQueryItem(player, top);
            player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        }
    }

    /** 关闭上一次插件实例残留的查询界面，必须在玩家所属区域的线程上调用。 */
    private void closeStaleView(@NotNull Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        Object holder = top.getHolder();
        if (holder == null || holder instanceof EnchantBrowserHolder
                || !holder.getClass().getName().equals(EnchantBrowserHolder.class.getName())) {
            return;
        }
        returnQueryItem(player, top);
        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
    }

    private void render(@NotNull EnchantBrowserHolder holder) {
        Inventory inventory = holder.getInventory();
        ItemStack query = inventory.getItem(QUERY_SLOT);
        List<Match> matches = collectMatches(query);
        int maxPage = matches.isEmpty() ? 0 : (matches.size() - 1) / PAGE_SIZE;
        int page = Math.clamp((long) holder.getPage(), 0, maxPage);
        holder.setPage(page);

        ItemStack border = createBorder(styleFor(query));
        for (int slot = 0; slot < 9; slot++) {
            if (slot != QUERY_SLOT) {
                inventory.setItem(slot, border);
            }
        }
        for (int slot = 45; slot < INVENTORY_SIZE; slot++) {
            inventory.setItem(slot, border);
        }

        Set<String> owned = collectOwnedEnchantmentIds(query);
        for (int index = 0; index < PAGE_SIZE; index++) {
            int slot = CONTENT_START_SLOT + index;
            int matchIndex = page * PAGE_SIZE + index;
            if (matchIndex >= matches.size()) {
                inventory.setItem(slot, null);
                continue;
            }
            inventory.setItem(slot, createEnchantmentBook(matches.get(matchIndex), owned));
        }

        if (page > 0) {
            holder.setPreviousTarget(page - 1);
            inventory.setItem(PREVIOUS_SLOT, createPageButton(page, "上一页"));
        } else {
            holder.setPreviousTarget(null);
        }
        if (page < maxPage) {
            holder.setNextTarget(page + 1);
            inventory.setItem(NEXT_SLOT, createPageButton(page + 2, "下一页"));
        } else {
            holder.setNextTarget(null);
        }
    }

    private @NotNull List<Match> collectMatches(@Nullable ItemStack query) {
        Map<Key, EnchantConfig> configs = MeowsEnchants.getEnchantConfigs();
        List<Match> matches = new ArrayList<>(configs.size());
        boolean allEnchantments = query == null || query.getType().isAir()
                || query.getType() == Material.ENCHANTED_BOOK;
        Registry<Enchantment> registry = allEnchantments
                ? null
                : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

        for (Map.Entry<Key, EnchantConfig> entry : configs.entrySet()) {
            if (!allEnchantments) {
                Enchantment enchantment = registry.get(entry.getKey());
                if (enchantment == null || !enchantment.canEnchantItem(query)) {
                    continue;
                }
            }
            matches.add(new Match(entry.getKey(), entry.getValue()));
        }

        Collator collator = Collator.getInstance(Locale.CHINA);
        matches.sort((left, right) -> {
            int result = Integer.compare(categoryIndex(left.config().getApplicableItem()),
                    categoryIndex(right.config().getApplicableItem()));
            if (result != 0) {
                return result;
            }
            result = collator.compare(TextUtil.plain(left.config().getDisplayName()),
                    TextUtil.plain(right.config().getDisplayName()));
            if (result != 0) {
                return result;
            }
            return left.config().getId().compareTo(right.config().getId());
        });
        return matches;
    }

    private @NotNull Set<String> collectOwnedEnchantmentIds(@Nullable ItemStack query) {
        if (query == null || query.getType().isAir()) {
            return Collections.emptySet();
        }
        ItemMeta meta = query.getItemMeta();
        if (meta == null) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        for (Enchantment enchantment : meta.getEnchants().keySet()) {
            ids.add(enchantmentId(enchantment.getKey()));
        }
        if (meta instanceof EnchantmentStorageMeta storage) {
            for (Enchantment enchantment : storage.getStoredEnchants().keySet()) {
                ids.add(enchantmentId(enchantment.getKey()));
            }
        }
        return ids;
    }

    private @NotNull ItemStack createEnchantmentBook(@NotNull Match match, @NotNull Set<String> owned) {
        EnchantConfig config = match.config();
        Material material = owned.contains(enchantmentId(match.key()))
                ? Material.KNOWLEDGE_BOOK
                : Material.ENCHANTED_BOOK;
        ItemStack book = new ItemStack(material);
        ItemMeta meta = book.getItemMeta();
        meta.customName(TextUtil.parseFlat(config.getDisplayName()));
        List<String> description = config.getDescription();
        if (!description.isEmpty()) {
            List<Component> lore = new ArrayList<>(description.size());
            for (String line : description) {
                lore.add(TextUtil.parseFlat(line));
            }
            meta.lore(lore);
        }
        book.setItemMeta(meta);
        return book;
    }

    private @NotNull ItemStack createPageButton(int pageNumber, @NotNull String label) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.setAmount(Math.clamp(pageNumber, 1, arrow.getMaxStackSize()));
        ItemMeta meta = arrow.getItemMeta();
        meta.customName(TextUtil.parseFlat("<yellow>" + label + " <gray>第 " + pageNumber + " 页"));
        meta.lore(List.of(TextUtil.parseFlat("<gray>点击翻到第 <yellow>" + pageNumber + " <gray>页")));
        arrow.setItemMeta(meta);
        return arrow;
    }

    private @NotNull ItemStack createBorder(@NotNull BorderStyle style) {
        ItemStack pane = new ItemStack(style.material());
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        return pane;
    }

    private @NotNull BorderStyle styleFor(@Nullable ItemStack query) {
        if (query == null || query.getType().isAir()) {
            return BorderStyle.BLACK;
        }
        ItemType type = query.getType().asItemType();
        if (type == null) {
            return BorderStyle.BLACK;
        }
        if (isTagged(type, TOOL_TAGS)) {
            return BorderStyle.YELLOW;
        }
        if (isTagged(type, WEAPON_TAGS)) {
            return BorderStyle.RED;
        }
        if (isTagged(type, ARMOR_TAGS)) {
            return BorderStyle.BLUE;
        }
        return BorderStyle.BLACK;
    }

    private boolean isTagged(@NotNull ItemType type, @NotNull List<TagKey<ItemType>> tagKeys) {
        for (TagKey<ItemType> tagKey : tagKeys) {
            try {
                if (Registry.ITEM.getTagValues(tagKey).contains(type)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // 该版本不存在对应标签时跳过
            }
        }
        return false;
    }

    private void returnQueryItem(@NotNull Player player, @NotNull Inventory top) {
        ItemStack query = top.getItem(QUERY_SLOT);
        if (query == null) {
            return;
        }
        top.setItem(QUERY_SLOT, null);
        if (query.getType().isAir()) {
            return;
        }
        for (ItemStack leftover : player.getInventory().addItem(query).values()) {
            if (leftover == null || leftover.getType().isAir()) {
                continue;
            }
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    /**
     * Shift 点击查询格：把查询物品快速取回背包。背包放不下时保持原状，避免物品被丢弃或只剩一部分。
     */
    private void takeQueryItem(@NotNull Player player, @NotNull Inventory top) {
        ItemStack query = top.getItem(QUERY_SLOT);
        if (query == null || query.getType().isAir()) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (!fitsInInventory(inventory, query)) {
            return;
        }
        top.setItem(QUERY_SLOT, null);
        for (ItemStack leftover : inventory.addItem(query).values()) {
            // 理论上不会发生，兜底把放不下的部分退回查询格
            top.setItem(QUERY_SLOT, leftover);
        }
    }

    /**
     * Shift 点击背包内的物品：整叠快速放入查询格。查询格已有物品时与来源槽位直接对调，
     * 来源槽位本次操作后必然空出，因此对调一定放得下，不会出现物品丢失或残留。
     */
    private void quickMoveToQuery(@NotNull Player player, @NotNull EnchantBrowserHolder holder,
                                  @NotNull InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        Inventory top = holder.getInventory();
        ItemStack existing = top.getItem(QUERY_SLOT);
        ItemStack moved = clicked.clone();
        if (existing != null && !existing.getType().isAir()) {
            event.setCurrentItem(existing);
        } else {
            event.setCurrentItem(null);
        }
        top.setItem(QUERY_SLOT, moved);
        scheduleRefresh(player, holder);
    }

    /** 判断背包（不含盔甲与副手）能否完整容纳该物品，含可堆叠的剩余空间。 */
    private static boolean fitsInInventory(@NotNull PlayerInventory inventory, @NotNull ItemStack stack) {
        int remaining = stack.getAmount();
        int maxStackSize = stack.getMaxStackSize();
        for (ItemStack content : inventory.getStorageContents()) {
            if (remaining <= 0) {
                break;
            }
            if (content == null || content.getType().isAir()) {
                remaining -= maxStackSize;
            } else if (content.isSimilar(stack)) {
                remaining -= Math.max(0, maxStackSize - content.getAmount());
            }
        }
        return remaining <= 0;
    }

    private void scheduleRefresh(@NotNull Player player, @NotNull EnchantBrowserHolder holder) {
        player.getScheduler().run(plugin, task -> {
            if (player.getOpenInventory().getTopInventory().getHolder() == holder) {
                render(holder);
            }
        }, null);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantBrowserHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.getOwnerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean clickedTop = rawSlot >= 0 && rawSlot < INVENTORY_SIZE && event.getClickedInventory() == top;

        if (clickedTop && rawSlot == PREVIOUS_SLOT) {
            event.setCancelled(true);
            Integer target = holder.getPreviousTarget();
            if (target != null) {
                holder.setPage(target);
                render(holder);
            }
            return;
        }
        if (clickedTop && rawSlot == NEXT_SLOT) {
            event.setCancelled(true);
            Integer target = holder.getNextTarget();
            if (target != null) {
                holder.setPage(target);
                render(holder);
            }
            return;
        }

        if (clickedTop && rawSlot == QUERY_SLOT && !event.isShiftClick()
                && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
            scheduleRefresh(player, holder);
            return;
        }

        // Shift 点击：查询格快速取回，背包内物品快速放入查询格；其余格子（边框、列表、翻页）一律拦截
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            if (clickedTop && rawSlot == QUERY_SLOT) {
                takeQueryItem(player, top);
                scheduleRefresh(player, holder);
            } else if (!clickedTop && event.getClickedInventory() != null) {
                quickMoveToQuery(player, holder, event);
            }
            return;
        }
        // 双击收集会把列表内的展示物品当作真实物品搬走，必须拦截
        if (event.isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() == null) {
            return;
        }
        if (clickedTop) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof EnchantBrowserHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)
                || !holder.getOwnerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        Set<Integer> rawSlots = event.getRawSlots();
        for (int rawSlot : rawSlots) {
            if (rawSlot >= 0 && rawSlot < INVENTORY_SIZE && rawSlot != QUERY_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
        // 拖拽可能把查询物品放进或取出查询槽，统一在下一 tick 重新渲染
        scheduleRefresh(player, holder);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantBrowserHolder holder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)
                || !holder.getOwnerId().equals(player.getUniqueId())) {
            return;
        }
        returnQueryItem(player, top);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof EnchantBrowserHolder holder
                && holder.getOwnerId().equals(player.getUniqueId())) {
            returnQueryItem(player, top);
        }
    }

    private static @NotNull Map<String, Integer> createCategoryIndex() {
        Map<String, Integer> index = new HashMap<>();
        for (int position = 0; position < CATEGORY_ORDER.size(); position++) {
            index.put(CATEGORY_ORDER.get(position), position);
        }
        return index;
    }

    private static int categoryIndex(@Nullable String applicableItem) {
        if (applicableItem == null) {
            return DEFAULT_CATEGORY_INDEX;
        }
        Integer index = CATEGORY_INDEX.get(applicableItem.toUpperCase(Locale.ROOT));
        return index != null ? index : DEFAULT_CATEGORY_INDEX;
    }

    private static @NotNull String enchantmentId(@NotNull Key key) {
        return key.namespace() + ":" + key.value();
    }

    private enum BorderStyle {
        BLACK(Material.BLACK_STAINED_GLASS_PANE),
        YELLOW(Material.YELLOW_STAINED_GLASS_PANE),
        RED(Material.RED_STAINED_GLASS_PANE),
        BLUE(Material.BLUE_STAINED_GLASS_PANE);

        private final Material material;

        BorderStyle(@NotNull Material material) {
            this.material = material;
        }

        public @NotNull Material material() {
            return material;
        }
    }

    private record Match(@NotNull Key key, @NotNull EnchantConfig config) {
    }
}
