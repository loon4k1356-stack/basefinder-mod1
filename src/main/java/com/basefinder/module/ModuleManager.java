package com.basefinder.module;

import com.basefinder.module.modules.BaseFinderModule;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {

    public List<Module> modules = new ArrayList<>();
    public BaseFinderModule baseFinder;

    public void registerModule(Module module) {
        modules.add(module);
        if (module instanceof BaseFinderModule) {
            this.baseFinder = (BaseFinderModule) module;
        }
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Module.Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }
}
