package com.windsor.meowsEnchants.actions.impl;

import com.windsor.meowsEnchants.actions.Action;
import com.windsor.meowsEnchants.actions.ActionContext;
import com.windsor.meowsEnchants.actions.ScalingFunction;
import com.windsor.meowsEnchants.actions.ScalingFunctionLoader;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class AttributeModifierAction implements Action {
    private final Attribute attribute;
    private final ScalingFunction amount;
    private final AttributeModifier.Operation operation;
    private final String key;
    private final boolean equipped;

    public AttributeModifierAction(Map<String, Object> params) {
        String attrName = (String) params.get("attribute");
        if (attrName == null) {
            throw new IllegalArgumentException("Missing 'attribute' parameter");
        }
        NamespacedKey attrKey = NamespacedKey.minecraft(attrName.toLowerCase());
        Attribute attr = Registry.ATTRIBUTE.get(attrKey);
        if (attr == null) {
            throw new IllegalArgumentException("Unknown attribute: " + attrName);
        }
        this.attribute = attr;

        Object amountObj = params.get("amount");
        if (amountObj == null) {
            throw new IllegalArgumentException("Missing 'amount' parameter");
        }
        this.amount = ScalingFunctionLoader.load(amountObj);

        String opStr = (String) params.get("operation");
        if (opStr == null) {
            throw new IllegalArgumentException("Missing 'operation' parameter");
        }
        this.operation = getOperation(opStr);

        this.key = (String) params.get("key");
        if (this.key == null) {
            throw new IllegalArgumentException("Missing 'key' parameter");
        }

        Map<String, Object> conditions = (Map<String, Object>) params.get("conditions");
        this.equipped = conditions != null && conditions.containsKey("equipped") && (boolean) conditions.get("equipped");
    }

    private AttributeModifier.Operation getOperation(String op) {
        return switch (op.toUpperCase()) {
            case "ADD_NUMBER" -> AttributeModifier.Operation.ADD_NUMBER;
            case "ADD_SCALAR" -> AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> throw new IllegalArgumentException("Unknown operation: " + op);
        };
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        if (player == null) return false;

        Plugin plugin = context.getPlugin();
        if (plugin == null) return false;

        AttributeModifierHelper.applyModifierForPlayer(
                player,
                plugin,
                context.getEnchantKey(),
                attribute,
                amount,
                operation,
                key,
                equipped
        );
        return true;
    }
}