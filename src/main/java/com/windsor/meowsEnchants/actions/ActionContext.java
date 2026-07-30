package com.windsor.meowsEnchants.actions;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

public class ActionContext {
    private final Plugin plugin;
    private final Player player;
    private final Key enchantKey;          // 新增
    private final int enchantLevel;
    private final String triggerName;
    private final List<Entity> targets;
    private final Event originalEvent;
    private final Object extra;
    private final String targetType;

    public ActionContext(Plugin plugin, Player player, Key enchantKey, int enchantLevel,
                         String triggerName, List<Entity> targets, Event originalEvent,
                         Object extra, String targetType) {
        this.plugin = plugin;
        this.player = player;
        this.enchantKey = enchantKey;
        this.enchantLevel = enchantLevel;
        this.triggerName = triggerName;
        this.targets = targets != null ? List.copyOf(targets) : Collections.emptyList();
        this.originalEvent = originalEvent;
        this.extra = extra;
        this.targetType = targetType;
    }

    public Plugin getPlugin() { return plugin; }
    public Player getPlayer() { return player; }
    public Key getEnchantKey() { return enchantKey; }
    public int getEnchantLevel() { return enchantLevel; }
    public String getTriggerName() { return triggerName; }
    public List<Entity> getTargets() { return targets; }
    public Event getOriginalEvent() { return originalEvent; }
    public Object getExtra() { return extra; }
    public String getTargetType() { return targetType; }
    public Entity getPrimaryTarget() { return targets.isEmpty() ? null : targets.get(0); }
}