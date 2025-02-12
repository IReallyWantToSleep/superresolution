package io.homo.superresolution.common.gui.widgets;

import net.minecraft.Util;

public class SmoothValue {
    private double changeTime = 150;
    private double lastSetTime = 0;
    private double currentValue;
    private double targetValue;
    private double startValue;

    public SmoothValue(double value, double changeTime) {
        this.currentValue = value;
        this.targetValue = value;       // 初始目标值=当前值（无动画）
        this.startValue = value;
        this.changeTime = changeTime;
        this.lastSetTime = Util.getMillis();
    }

    public SmoothValue(double value) {
        this(value, 50);
    }

    public void setValue(double targetValue) {
        this.startValue = this.currentValue;
        this.targetValue = targetValue;
        this.lastSetTime = Util.getMillis();
    }

    public void addValue(double value) {
        this.setValue(this.targetValue + value);
    }

    public void reduceValue(double value) {
        this.setValue(this.targetValue - value);
    }

    public double value() {
        double currentTime = Util.getMillis();
        double elapsed = currentTime - lastSetTime;

        if (elapsed >= changeTime || startValue == targetValue) {
            currentValue = targetValue;
        } else {
            double progress = elapsed / changeTime;
            currentValue = startValue + (targetValue - startValue) * progress;
        }

        return currentValue;
    }

    public void jumpToTarget() {
        this.currentValue = targetValue;
        this.startValue = targetValue;
        this.lastSetTime = Util.getMillis();
    }

    public boolean isAnimating() {
        return currentValue != targetValue;
    }
}