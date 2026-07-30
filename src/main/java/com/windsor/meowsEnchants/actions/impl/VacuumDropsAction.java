package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;

public class VacuumDropsAction implements Action {

    public VacuumDropsAction(Map<String, Object> params) {
        // 无参数
    }

    @Override
    public boolean execute(ActionContext context) {
        // 只做基本的合法性检查，实际收集由 BlockDropItemListener 完成
        if (!(context.getOriginalEvent() instanceof BlockBreakEvent event)) {
            return false;
        }
        return !event.isCancelled();
        // 动作触发成功，返回 true，表示附魔已生效（具体收集由监听器完成）
    }
}