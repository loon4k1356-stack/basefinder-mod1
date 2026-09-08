package com.basefinder.config;

import com.basefinder.BaseFinderClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configDir;
    private static Config currentConfig;

    public static void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.runDirectory == null) return;
        
        configDir = mc.runDirectory.toPath().resolve("freezdlc");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        loadConfig("default");
    }

    public static void loadConfig(String name) {
        Path configFile = configDir.resolve(name + ".json");
        if (!Files.exists(configFile)) {
            currentConfig = new Config();
            saveConfig(name);
            if (BaseFinderClient.LOGGER != null) BaseFinderClient.LOGGER.info("[freezdlc] Created default config");
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            currentConfig = GSON.fromJson(reader, Config.class);
            if (BaseFinderClient.LOGGER != null) BaseFinderClient.LOGGER.info("[freezdlc] Loaded config: {}", name);
            
            // Применяем настройки к сканеру
            if (BaseFinderClient.scanner != null && currentConfig != null) {
                BaseFinderClient.scanner.setScanRadius(currentConfig.scanRadius);
                BaseFinderClient.scanner.setLiteMode(currentConfig.liteMode);
                BaseFinderClient.scanner.setLiteHeightLimit(currentConfig.liteHeightLimit);
                
                // Очищаем и загружаем блоки (исправлено: теперь работаем с координатами)
                BaseFinderClient.scanner.clearSelectedBlocks();
                if (currentConfig.selectedBlocks != null) {
                    for (BlockPosData posData : currentConfig.selectedBlocks) {
                        BaseFinderClient.scanner.addSelectedBlock(new BlockPos(posData.x, posData.y, posData.z));
                    }
                }
            }
        } catch (IOException e) {
            if (BaseFinderClient.LOGGER != null) BaseFinderClient.LOGGER.error("[freezdlc] Failed to load config", e);
            currentConfig = new Config();
        }
    }

    public static void saveConfig(String name) {
        if (currentConfig == null) currentConfig = new Config();
        if (BaseFinderClient.scanner != null) {
            currentConfig.scanRadius = BaseFinderClient.scanner.getScanRadius();
            currentConfig.liteMode = BaseFinderClient.scanner.isLiteMode();
            currentConfig.liteHeightLimit = BaseFinderClient.scanner.getLiteHeightLimit();
            
            // Сохраняем координаты (исправлено: конвертируем BlockPos в данные)
            currentConfig.selectedBlocks = new ArrayList<>();
            for (BlockPos pos : BaseFinderClient.scanner.getSelectedBlocks()) {
                currentConfig.selectedBlocks.add(new BlockPosData(pos.getX(), pos.getY(), pos.getZ()));
            }
        }

        Path configFile = configDir.resolve(name + ".json");
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(currentConfig, writer);
            if (BaseFinderClient.LOGGER != null) BaseFinderClient.LOGGER.info("[freezdlc] Saved config: {}", name);
        } catch (IOException e) {
            if (BaseFinderClient.LOGGER != null) BaseFinderClient.LOGGER.error("[freezdlc] Failed to save config", e);
        }
    }

    public static String getCurrentConfigName() {
        return "default"; // Можно расширить для переключения конфигов
    }

    // Внутренний класс для хранения конфига
    private static class Config {
        public int scanRadius = 64;
        public boolean liteMode = false;
        public int liteHeightLimit = 30;
        public List<BlockPosData> selectedBlocks = new ArrayList<>();
    }

    // Класс для сериализации координат (вместо сохранения самих объектов Block/BlockPos)
    private static class BlockPosData {
        public int x, y, z;
        public BlockPosData(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
