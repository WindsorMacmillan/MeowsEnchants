package com.windsor.meowsEnchants.actions;

import org.bukkit.entity.Entity;

public interface Action {
    /**
     * 执行动作。
     * @param context 执行上下文
     * @return true 表示动作完整执行，false 表示中途返回（未执行）
     */
    boolean execute(ActionContext context);
}