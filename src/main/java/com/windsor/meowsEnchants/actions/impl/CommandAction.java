package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.utils.PlaceholderAPIHook;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CommandAction implements Action {
    private final String command;
    private final boolean runAsConsole;

    public CommandAction(Map<String, Object> params) {
        Object cmdObj = params.get("command");
        if (cmdObj == null) {
            throw new IllegalArgumentException("Missing 'command' parameter for command action");
        }
        this.command = cmdObj.toString();

        String runAs = params.containsKey("run_as") ? params.get("run_as").toString().toUpperCase() : "PLAYER";
        this.runAsConsole = "CONSOLE".equalsIgnoreCase(runAs);
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        // 1. 获取目标实体名称（用于 {target} 占位符）
        List<Entity> targets = context.getTargets();
        String targetName = "";
        if (targets != null && !targets.isEmpty()) {
            Entity target = targets.getFirst();
            if (target instanceof Player) {
                targetName = target.getName();
            } else {
                targetName = target.getType().name().toLowerCase();
            }
        }

        // 2. 替换内置占位符
        String parsedCommand = command
                .replace("{player}", player.getName())
                .replace("{target}", targetName)
                .replace("{level}", String.valueOf(context.getEnchantLevel()));

        // 3. 使用 PlaceholderAPI 解析（若已加载）
        if (PlaceholderAPIHook.isAvailable()) {
            parsedCommand = PlaceholderAPIHook.setPlaceholders(player, parsedCommand);
        }

        // 4. 执行命令
        boolean success = false;
        try {
            if (runAsConsole) {
                success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
            } else {
                success = player.performCommand(parsedCommand);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MeowsEnchants] 执行命令失败: " + parsedCommand + " - " + e.getMessage());
            return false;
        }

        return success;
    }
}