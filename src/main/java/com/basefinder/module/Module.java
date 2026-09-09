package com.basefinder.module;

import java.util.ArrayList;
import java.util.List;

public class Module {
    public String name;
    public String description;
    public Category category;
    public boolean toggled;
    public List<Object> settings = new ArrayList<>(); // Используем Object или создай интерфейс Setting

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.toggled = false;
    }

    public void toggle() {
        this.toggled = !this.toggled;
        if (this.toggled) onEnable();
        else onDisable();
    }

    public boolean isToggled() {
        return toggled;
    }

    public void addSetting(Object setting) {
        settings.add(setting);
    }

    public List<Object> getSettings() {
        return settings;
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    public enum Category {
        COMBAT, MOVEMENT, RENDER, WORLD, MISC
    }
}
