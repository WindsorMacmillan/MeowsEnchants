package com.windsor.meowsEnchants.actions;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 从 YAML 解析的动作参数加载 Action 实例。
 */
public final class ActionLoader {

    private ActionLoader() {}

    /**
     * 从动作参数列表加载所有 Action（适用于 actions 为 Map 格式）。
     *
     * @param actionsMap 通常从 YAML 的 actions 字段解析得到，格式为 Map<String, Map<String, Object>>
     *                   键是动作唯一标识（可忽略），值是动作参数（包含 type 等）。
     * @return Action 列表
     */
    public static List<Action> loadActions(Map<String, Object> actionsMap) {
        List<Action> actions = new ArrayList<>();
        if (actionsMap == null || actionsMap.isEmpty()) {
            return actions;
        }

        for (Map.Entry<String, Object> entry : actionsMap.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof Map)) {
                continue;
            }
            Map<String, Object> actionData = (Map<String, Object>) value;
            String type = (String) actionData.get("type");
            if (type == null || type.isEmpty()) {
                continue;
            }
            try {
                actions.add(ActionRegistry.create(type, actionData));
            } catch (IllegalArgumentException e) {
                // 记录错误（可使用日志），继续加载其他动作
            }
        }
        return actions;
    }

    /**
     * 从动作列表（List<Map>）加载，适用于 actions 为列表格式。
     */
    public static List<Action> loadActionsFromList(List<Map<String, Object>> actionDataList) {
        List<Action> actions = new ArrayList<>();
        if (actionDataList == null) {
            return actions;
        }
        for (Map<String, Object> actionData : actionDataList) {
            String type = (String) actionData.get("type");
            if (type == null) {
                continue;
            }
            try {
                actions.add(ActionRegistry.create(type, actionData));
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[ActionLoader] 创建动作失败: " + e.getMessage());
            }
        }
        return actions;
    }
}