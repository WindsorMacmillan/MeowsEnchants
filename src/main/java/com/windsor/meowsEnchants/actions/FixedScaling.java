package com.windsor.meowsEnchants.actions;

public class FixedScaling implements ScalingFunction {
    private final double value;

    public FixedScaling(double value) {
        this.value = value;
    }

    @Override
    public double getValue(int level) {
        return value;
    }
}