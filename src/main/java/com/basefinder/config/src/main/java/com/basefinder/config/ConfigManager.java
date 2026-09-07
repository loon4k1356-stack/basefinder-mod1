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
    private static final String CONFIG_FILE = "config/basefinder.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Config config;
    
    public static class Config {
        public int scanRadius = 300;
        public boolean liteMode = false;
        public int liteHeightLimit = 30;
        public List<String> selectedBlocks = new ArrayList<>();
        public boolean showTracers = true;
        public boolean showBoxes = true;
        public float boxAlpha = 0.4f;
        
        public Config() {}
    }
    
    public static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, Config.class);
                BaseFinderClient.LOGGER.info("[BaseFinder] Config loaded successfully");
            } catch (IOException e) {
                BaseFinderClient.LOGGER.error("[BaseFinder] Failed to load config", e);
                config = new Config();
            }
        } else {
            BaseFinderClient.LOGGER.info("[BaseFinder] No config found, using defaults");
            config = new Config();
            saveConfig();
        }
        
        applyConfig();
    }
    
    public static void saveConfig() {
        updateConfigFromScanner();
        
        File configFile = new File(CONFIG_FILE);
        configFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
            BaseFinderClient.LOGGER.info("[BaseFinder] Config saved successfully");
        } catch (IOException e) {
            BaseFinderClient.LOGGER.error("[BaseFinder] Failed to save config", e);
        }
    }
    
    private static void applyConfig() {
        if (BaseFinderClient.scanner == null) return;
        
        BaseFinderClient.scanner.setScanRadius(config.scanRadius);
        BaseFinderClient.scanner.setLiteMode(config.liteMode);
        BaseFinderClient.scanner.setLiteHeightLimit(config.liteHeightLimit);
        
        BaseFinderClient.scanner.clearSelectedBlocks();
        for (String blockId : config.selectedBlocks) {
            try {
                Identifier id = Identifier.of(blockId);
                Block block = Registries.BLOCK.get(id);
                if (block != null) {
                    BaseFinderClient.scanner.addSelectedBlock(block);
                }
            } catch (Exception e) {
                BaseFinderClient.LOGGER.warn("[BaseFinder] Invalid block in config: {}", blockId);
            }
        }
        
        if (BaseFinderClient.renderer != null) {
            BaseFinderClient.renderer.setShowTracers(config.showTracers);
            BaseFinderClient.renderer.setShowBoxes(config.showBoxes);
            BaseFinderClient.renderer.setBoxAlpha(config.boxAlpha);
        }
        
        BaseFinderClient.LOGGER.info("[BaseFinder] Config applied: radius={}, lite={}, blocks={}", 
            config.scanRadius, config.liteMode, config.selectedBlocks.size());
    }
    
    private static void updateConfigFromScanner() {
        if (BaseFinderClient.scanner == null) return;
        
        config.scanRadius = BaseFinderClient.scanner.getScanRadius();
        config.liteMode = BaseFinderClient.scanner.isLiteMode();
        config.liteHeightLimit = BaseFinderClient.scanner.getLiteHeightLimit();
        
        config.selectedBlocks.clear();
        for (Block block : BaseFinderClient.scanner.getSelectedBlocks()) {
            config.selectedBlocks.add(Registries.BLOCK.getId(block).toString());
        }
        
        if (BaseFinderClient.renderer != null) {
            config.showTracers = BaseFinderClient.renderer.isShowTracers();
            config.showBoxes = BaseFinderClient.renderer.isShowBoxes();
            config.boxAlpha = BaseFinderClient.renderer.getBoxAlpha();
        }
    }
    
    public static Config getConfig() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }
}
