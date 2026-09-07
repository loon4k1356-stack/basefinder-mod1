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
