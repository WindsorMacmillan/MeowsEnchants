package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.Random;

public class ReflectDamageAction implements Action {
    private final ScalingFunction percentage;
    private final Random random = new Random();

    public ReflectDamageAction(Map<String, Object> params) {
        Object percentageObj = params.get("percentage");
        if (percentageObj == null) {
            throw new IllegalArgumentException("Missing 'percentage' parameter for reflect_damage action");
        }
        this.percentage = ScalingFunctionLoader.load(percentageObj);
    }

    @Override
    public boolean execute(ActionContext context) {
        // 必须为实体伤害事件
        if (!(context.getOriginalEvent() instanceof EntityDamageByEntityEvent event)) {
            return false;
        }

        // 如果事件已被其他插件取消，不执行反弹
        if (event.isCancelled()) {
            return false;
        }

        // 只处理近战伤害（排除投射物）
        Entity damager = event.getDamager();
        if (damager instanceof Projectile) {
            return false; // 投射物伤害不反弹
        }
        if (!(damager instanceof LivingEntity)) {
            return false; // 非生物攻击不反弹（如环境伤害）
        }

        // 受害者为玩家（由触发器保证，但二次检查）
        if (!(event.getEntity() instanceof Player player)) {
            return false;
        }

        // 概率判定
        double chance = this.percentage.getValue(context.getEnchantLevel());
        if (chance <= 0 || random.nextDouble() >= chance) {
            return false; // 未触发反弹
        }

        // 取消原伤害
        event.setCancelled(true);

        // 对攻击者造成同等伤害（取消后原伤害值仍可用）
        double damage = event.getDamage();
        if (damage > 0) {
            ((LivingEntity) damager).damage(damage, player);
        }
        return true;
    }
}