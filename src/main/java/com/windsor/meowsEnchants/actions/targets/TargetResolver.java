package com.windsor.meowsEnchants.actions.targets;

import com.windsor.meowsEnchants.actions.ActionContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import java.util.List;

public interface TargetResolver {
    List<Entity> resolve(Player player, ActionContext context);
}

