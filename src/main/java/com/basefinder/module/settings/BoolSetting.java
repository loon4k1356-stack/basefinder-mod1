package com.basefinder.module.settings;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }
    
    public boolean get() { return value; }
    public void set(boolean value) { this.value = value; }
    public void toggle() { value = !value; }
    
    @Override
    public String getValueAsString() { return value ? "§aON" : "§cOFF"; }
}
