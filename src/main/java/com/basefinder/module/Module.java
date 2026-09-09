package com.basefinder.module;

import java.util.ArrayList;
import java.util.List;

public class Module {
    protected String name;
    protected String description;
    protected Category category;
    protected boolean enabled;
    protected List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    
    public boolean isEnabled() { return enabled; }
    
    public void toggle() {
        this.enabled = !this.enabled;
        onToggle();
    }

    protected void onToggle() {}
    public void onTick() {}
    public void onRender() {}

    public List<Setting<?>> getSettings() { return settings; }
    
    protected void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    public enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        RENDER("Render"),
        PLAYER("Player"),
        MISC("Misc");

        public final String displayName;
        Category(String name) { this.displayName = name; }
    }
}
