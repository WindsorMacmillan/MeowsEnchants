package com.windsor.meowsEnchants.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 附魔查询界面的 {@link InventoryHolder}。
 * <p>
 * 通过自定义 Holder 识别界面，避免使用标题匹配；同时保存翻页按钮当前指向的页码，
 * 使监听器无需重复计算过滤结果。该类只持有基本类型与界面引用，不会在插件卸载后滞留玩家对象。
 */
public final class EnchantBrowserHolder implements InventoryHolder {

    private final UUID ownerId;
    private int page;
    private Integer previousTarget;
    private Integer nextTarget;
    private Inventory inventory;

    public EnchantBrowserHolder(@NotNull UUID ownerId, int page) {
        this.ownerId = ownerId;
        this.page = Math.max(0, page);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    public @NotNull UUID getOwnerId() {
        return ownerId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public @Nullable Integer getPreviousTarget() {
        return previousTarget;
    }

    public void setPreviousTarget(@Nullable Integer previousTarget) {
        this.previousTarget = previousTarget;
    }

    public @Nullable Integer getNextTarget() {
        return nextTarget;
    }

    public void setNextTarget(@Nullable Integer nextTarget) {
        this.nextTarget = nextTarget;
    }
}
