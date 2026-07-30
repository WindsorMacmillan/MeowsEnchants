package com.windsor.meowsEnchants.actions.targets;

import com.windsor.meowsEnchants.actions.ActionContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

// ALL_ENTITIES – 以玩家为中心半径20格内的所有实体
public class AllEntitiesTarget implements TargetResolver {
    @Override public List<Entity> resolve(Player player, ActionContext context) {
        return player.getNearbyEntities(20, 20, 20);
    }
}
