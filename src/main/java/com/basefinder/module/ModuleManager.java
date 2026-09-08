package com.basefinder.module;

import com.basefinder.module.modules.BaseFinderModule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private List<Module> modules;
    public BaseFinderModule baseFinder;
    
    public ModuleManager() {
        modules = new ArrayList<>();
        baseFinder = new BaseFinderModule();
        modules.add(baseFinder);
    }
    
    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) module.onTick();
        }
    }
    
    public List<Module> getModules() { return modules; }
    
    public List<Module> getModulesByCategory(Module.Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }
    
    public Module getModuleByName(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
    
    public List<Module> getEnabledModules() {
        return modules.stream().filter(Module::isEnabled).collect(Collectors.toList());
    }
}
