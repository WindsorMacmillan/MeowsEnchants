package com.windsor.meowsEnchants.actions;

import com.windsor.meowsEnchants.actions.impl.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 动作注册表，用于根据类型名称创建对应的 Action 实例。
 */
public final class ActionRegistry {

    private static final Map<String, Function<Map<String, Object>, Action>> factories = new HashMap<>();

    static {
        factories.put("potion_effect", PotionEffectAction::new);
        factories.put("bonus_damage", BonusDamageAction::new);
        factories.put("healthsteal", HealthstealAction::new);
        factories.put("teleport", TeleportAction::new);
        factories.put("explosion", ExplosionAction::new);
        factories.put("velocity", VelocityAction::new);
        factories.put("shockwave", ShockwaveAction::new);
        factories.put("break_armor", BreakArmorAction::new);
        factories.put("drop_head", DropHeadAction::new);
        factories.put("attribute_modifier", AttributeModifierAction::new);
        factories.put("multi_break", MultiBreakAction::new);
        factories.put("vacuum_drops", VacuumDropsAction::new);
        factories.put("replant", ReplantAction::new);
        factories.put("multi_arrow", MultiArrowAction::new);
        factories.put("repair", RepairAction::new);
        factories.put("hook", HookAction::new);
        factories.put("feast", FeastAction::new);
        factories.put("deflect", DeflectAction::new);
        factories.put("till", TillAction::new);
        factories.put("reflect_damage", ReflectDamageAction::new);
        factories.put("command", CommandAction::new);
        factories.put("block_break", BlockBreakAction::new);
    }

    private ActionRegistry() {}

    /**
     * 根据类型名称和参数创建 Action 实例。
     *
     * @param type   动作类型（如 "potion_effect"）
     * @param params 动作参数 Map（从 YAML 解析得到）
     * @return Action 实例
     * @throws IllegalArgumentException 如果类型未注册
     */
    public static Action create(String type, Map<String, Object> params) {
        Function<Map<String, Object>, Action> factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown action type: " + type);
        }
        return factory.apply(params);
    }

    /**
     * 注册新的动作类型（可扩展）。
     */
    public static void register(String type, Function<Map<String, Object>, Action> factory) {
        factories.put(type, factory);
    }
}