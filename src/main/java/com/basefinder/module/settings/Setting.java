package com.basefinder.module.settings;

public abstract class Setting<T> {
    protected String name;
    protected String description;
    protected T value;
    protected T defaultValue;
    
    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public T getValue() { return value; }
    public T getDefaultValue() { return defaultValue; }
    public void setValue(T value) { this.value = value; }
    public void reset() { this.value = defaultValue; }
    public abstract String getValueAsString();
}
