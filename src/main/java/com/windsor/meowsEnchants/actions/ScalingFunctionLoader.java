package com.windsor.meowsEnchants.actions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public class ScalingFunctionLoader {

    public static ScalingFunction load(Object obj) {
        if (obj instanceof Number) {
            // 固定值
            return new FixedScaling(((Number) obj).doubleValue());
        } else if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            String scaling = (String) map.get("scaling");
            if ("fixed".equalsIgnoreCase(scaling)) {
                Object value = map.get("value");
                if (value instanceof Number) {
                    return new FixedScaling(((Number) value).doubleValue());
                }
            } else {
                // 默认线性 scaling: base, per_level
                Number base = (Number) map.get("base");
                Number perLevel = (Number) map.get("per_level");
                if (base != null && perLevel != null) {
                    return new LinearScaling(base.doubleValue(), perLevel.doubleValue());
                }
            }
        }
        throw new IllegalArgumentException("Invalid scaling function: " + obj);
    }
}