package com.basefinder.module.settings;

public class ModeSetting extends Setting<Integer> {
    protected String[] modes;
    
    public ModeSetting(String name, String description, int defaultIndex, String... modes) {
        super(name, description, defaultIndex);
        this.modes = modes;
    }
    
    public int getIndex() { return value; }
    public String getMode() { return modes[value]; }
    public String[] getModes() { return modes; }
    public void setIndex(int index) { if (index >= 0 && index < modes.length) this.value = index; }
    public void cycle() { value = (value + 1) % modes.length; }
    public boolean is(String mode) { return modes[value].equalsIgnoreCase(mode); }
    
    @Override
    public String getValueAsString() { return modes[value]; }
}
