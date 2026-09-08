package com.basefinder.config;

import com.basefinder.BaseFinderClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private static final String CONFIGS_DIR = "config/basefinder/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Config currentConfig;
    private static String currentConfigName = "default";

    public static class Config {
        public int scanRadius = 300;
        public boolean liteMode = false;
        public int liteHeightLimit = 30;
        public List<String> selectedBlocks = new ArrayList<>();

        public Config() {}

        public Config copy() {
            Config copy = new Config();
            copy.scanRadius = this.scanRadius;
            copy.liteMode = this.liteMode;
            copy.liteHeightLimit = this.liteHeightLimit;
            copy.selectedBlocks = new ArrayList<>(this.selectedBlocks);
            return copy;
        }
    }

    public static void init() {
        File configsDir = new File(CONFIGS_DIR);
        if (!configsDir.exists()) {
            configsDir.mkdirs();
            BaseFinderClient.LOGGER.info("[BaseFinder] Created configs directory");
        }
        loadConfig("default");
    }

    public static boolean loadConfig(String name) {
        String configPath = CONFIGS_DIR + name + ".json";
        File configFile = new File(configPath);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                currentConfig = GSON.fromJson(reader, Config.class);
                currentConfigName = name;
                applyConfig();
                BaseFinderClient.LOGGER.info("[BaseFinder] Config '{}' loaded", name);
                return true;
            } catch (IOException e) {
                BaseFinderClient.LOGGER.error("[BaseFinder] Failed to load config '{}'", name, e);
                return false;
            }
        } else {
            currentConfig = new Config();
            currentConfigName = name;
            saveConfig(name);
            applyConfig();
            return true;
        }
    }

    public static boolean saveConfig(String name) {
        updateConfigFromScanner();

        String configPath = CONFIGS_DIR + name + ".json";
        File configFile = new File(configPath);
        configFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(currentConfig, writer);
            BaseFinderClient.LOGGER.info("[BaseFinder] Config '{}' saved", name);
            return true;
        } catch (IOException e) {
            BaseFinderClient.LOGGER.error("[BaseFinder] Failed to save config '{}'", name, e);
            return false;
        }
    }

    public static boolean deleteConfig(String name) {
        String configPath = CONFIGS_DIR + name + ".json";
        File configFile = new File(configPath);
        if (configFile.exists()) {
            return configFile.delete();
        }
        return false;
    }

    public static List<String> listConfigs() {
        List<String> configs = new ArrayList<>();
        File configsDir = new File(CONFIGS_DIR);
        if (configsDir.exists() && configsDir.isDirectory()) {
            File[] files = configsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    configs.add(file.getName().replace(".json", ""));
                }
            }
        }
        return configs;
    }

    private static void applyConfig() {
        if (BaseFinderClient.scanner == null || currentConfig == null) return;

        BaseFinderClient.scanner.setScanRadius(currentConfig.scanRadius);
        BaseFinderClient.scanner.setLiteMode(currentConfig.liteMode);
        BaseFinderClient.scanner.setLiteHeightLimit(currentConfig.liteHeightLimit);

        BaseFinderClient.scanner.clearSelectedBlocks();
        for (String blockId : currentConfig.selectedBlocks) {
            try {
                Identifier id = Identifier.of(blockId);
                Block block = Registries.BLOCK.get(id);
                if (block != null) {
                  scanner.addSelectedBlock(BlockPos.of(block.getDefaultState().getBlock().getId()))? Нет, проще: если там цикл for (String blockId : list)
                }
            } catch (Exception e) {
                BaseFinderClient.LOGGER.warn("[BaseFinder] Invalid block: {}", blockId);
            }
        }
    }

    private static void updateConfigFromScanner() {
        if (BaseFinderClient.scanner == null) return;
        if (currentConfig == null) currentConfig = new Config();

        currentConfig.scanRadius = BaseFinderClient.scanner.getScanRadius();
        currentConfig.liteMode = BaseFinderClient.scanner.isLiteMode();
        currentConfig.liteHeightLimit = BaseFinderClient.scanner.getLiteHeightLimit();

        currentConfig.selectedBlocks.clear();
        for (Block block : BaseFinderClient.scanner.getSelectedBlocks()) {
            currentConfig.selectedBlocks.add(Registries.BLOCK.getId(block).toString());
        }
    }

    public static Config getCurrentConfig() {
        if (currentConfig == null) currentConfig = new Config();
        return currentConfig;
    }

    public static String getCurrentConfigName() {
        return currentConfigName;
    }

    public static void setCurrentConfigName(String name) {
        currentConfigName = name;
    }
}
