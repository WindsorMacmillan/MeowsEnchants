package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BonusDamageAction implements Action {
    private final ScalingFunction amount;
    private final boolean multiplier;
    private final boolean sneaking;
    private final boolean nightTime;
    private final double lowHealth;
    private final String dimension;
    private final Set<String> targetTypes;
    private final boolean passiveMobs;

    @SuppressWarnings("unchecked")
    public BonusDamageAction(Map<String, Object> params) {
        // 解析 amount（必须）
        Object amountObj = params.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Missing 'amount' parameter for bonus_damage action");
        }
        this.amount = ScalingFunctionLoader.load(amountObj);

        // 解析 multiplier（可选）
        this.multiplier = params.containsKey("multiplier") && (boolean) params.get("multiplier");

        // 解析 conditions（可选）
        Map<String, Object> conditions = (Map<String, Object>) params.get("conditions");
        if (conditions != null) {
            this.sneaking = conditions.containsKey("sneaking") && (boolean) conditions.get("sneaking");
            this.nightTime = conditions.containsKey("night_time") && (boolean) conditions.get("night_time");
            this.lowHealth = conditions.containsKey("low_health") ?
                    ((Number) conditions.get("low_health")).doubleValue() : 0.0;
            this.dimension = (String) conditions.get("dimension");
            this.passiveMobs = conditions.containsKey("passive_mobs") && (boolean) conditions.get("passive_mobs");

            List<String> targetList = (List<String>) conditions.get("target_types");
            this.targetTypes = targetList != null ? new HashSet<>(targetList) : new HashSet<>();
        } else {
            this.sneaking = false;
            this.nightTime = false;
            this.lowHealth = 0.0;
            this.dimension = null;
            this.passiveMobs = false;
            this.targetTypes = new HashSet<>();
        }
    }

    @Override
    public boolean execute(ActionContext context) {
        // 必须为伤害事件
        if (!(context.getOriginalEvent() instanceof EntityDamageByEntityEvent event)) {
            return false;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return false;
        }

        // ---- 条件检查 ----
        if (sneaking && !player.isSneaking()) {
            return false;
        }

        if (nightTime) {
            World world = player.getWorld();
            long time = world.getTime();
            // 夜晚时间：13000 ~ 23000
            if (time < 13000 || time > 23000) {
                return false;
            }
        }

        if (lowHealth > 0 && player.getHealth() > lowHealth) {
            return false;
        }

        if (dimension != null && !dimension.isEmpty()) {
            String worldName = player.getWorld().getName();
            if (!worldName.equalsIgnoreCase(dimension)) {
                return false;
            }
        }

        Entity target = event.getEntity();
        if (!(target instanceof LivingEntity)) {
            return false;
        }

        // 目标类型过滤
        if (!targetTypes.isEmpty()) {
            String typeName = target.getType().name();
            if (!targetTypes.contains(typeName)) {
                return false;
            }
        }

        // 被动生物过滤
        if (passiveMobs && !isPassiveMob(target)) {
            return false;
        }

        // ---- 计算伤害 ----
        double baseDamage = event.getDamage();
        double amountValue = this.amount.getValue(context.getEnchantLevel());
        double finalDamage = multiplier ? baseDamage * amountValue : baseDamage + amountValue;
        if (finalDamage < 0) finalDamage = 0;
        event.setDamage(finalDamage);
        return true;
    }

    /**
     * 判断目标是否为被动生物（可扩展）。
     */
    private boolean isPassiveMob(Entity entity) {
        return entity instanceof Animals ||
                entity instanceof Ambient ||
                entity instanceof Fish ||
                entity instanceof Squid ||
                entity instanceof Snowman ||
                entity instanceof IronGolem ||
                entity instanceof Dolphin;
    }
}