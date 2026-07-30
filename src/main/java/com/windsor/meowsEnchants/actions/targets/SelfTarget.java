package com.windsor.meowsEnchants.actions.targets;

import com.windsor.meowsEnchants.actions.ActionContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

// 示例实现：SELF
public class SelfTarget implements TargetResolver {
    @Override public List<Entity> resolve(Player player, ActionContext context) {
        return List.of(player);
    }
}
