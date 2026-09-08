package com.basefinder.module.settings;

public class NumberSetting extends Setting<Double> {
    protected double min;
    protected double max;
    protected double increment;
    
    public NumberSetting(String name, String description, double defaultValue, double min, double max, double increment) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }
    
    public double get() { return value; }
    public int getInt() { return (int) Math.round(value); }
    public void set(double value) { this.value = Math.max(min, Math.min(max, value)); }
    
    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getIncrement() { return increment; }
    
    public double getPercentage() { return (value - min) / (max - min); }
    
    public void setFromPercentage(double percentage) {
        percentage = Math.max(0, Math.min(1, percentage));
        set(min + (max - min) * percentage);
    }
    
    @Override
    public String getValueAsString() {
        if (increment >= 1) return String.valueOf((int) Math.round(value));
        return String.format("%.1f", value);
    }
}
