package com.windsor.meowsEnchants.actions;

public class LinearScaling implements ScalingFunction {
    private final double base;
    private final double perLevel;

    public LinearScaling(double base, double perLevel) {
        this.base = base;
        this.perLevel = perLevel;
    }

    @Override
    public double getValue(int level) {
        return base + perLevel * (level - 1);
    }
}