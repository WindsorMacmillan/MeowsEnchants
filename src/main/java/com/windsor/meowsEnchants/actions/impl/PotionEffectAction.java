package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import com.windsor.meowsEnchants.utils.SchedulerCompat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class PotionEffectAction implements Action {
    private final PotionEffectType effectType;
    private final ScalingFunction duration;
    private final ScalingFunction amplifier;
    private final boolean particles;
    private final boolean icon;

    public PotionEffectAction(Map<String, Object> params) {
        String effectName = (String) params.get("effect");
        if (effectName == null) {
            throw new IllegalArgumentException("Missing 'effect' parameter for potion_effect action");
        }
        this.effectType = PotionEffectType.getByName(effectName.toUpperCase());
        if (this.effectType == null) {
            throw new IllegalArgumentException("Unknown potion effect: " + effectName);
        }
        this.duration = ScalingFunctionLoader.load(params.get("duration"));
        this.amplifier = ScalingFunctionLoader.load(params.get("amplifier"));
        this.particles = params.containsKey("particles") ? (boolean) params.get("particles") : true;
        this.icon = params.containsKey("icon") ? (boolean) params.get("icon") : true;
    }

    @Override
    public boolean execute(ActionContext context) {
        int level = context.getEnchantLevel();
        int durationTicks = (int) duration.getValue(level);
        int amplifierValue = (int) amplifier.getValue(level);

        PotionEffect effect = new PotionEffect(effectType, durationTicks, amplifierValue,
                !particles, icon);

        for (Entity target : context.getTargets()) {
            if (!(target instanceof LivingEntity living)) continue;
            // 使用实体调度器确保在正确的线程上执行
            SchedulerCompat.run(living, context.getPlugin(),
                    scheduledTask -> living.addPotionEffect(effect));
        }
        return true;
    }
}
