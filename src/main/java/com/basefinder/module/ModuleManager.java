public List<Module> getModulesByCategory(Module.Category category) {
    List<Module> result = new ArrayList<>();
    for (Module mod : modules) { // Предполагаем, что список называется modules
        if (mod.getCategory() == category) {
            result.add(mod);
        }
    }
    return result;
}
