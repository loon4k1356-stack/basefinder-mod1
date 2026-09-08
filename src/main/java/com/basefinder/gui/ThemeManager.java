package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ThemeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "theme.json";
    
    // Настройки внешнего вида
    public float hue = 0.5f;          // Цвет (0.0 - 1.0)
    public int alpha = 200;           // Прозрачность фона (0 - 255)
    public int borderRadius = 4;      // Скругление углов (0 - 10)
    public float fontScale = 1.0f;    // Масштаб шрифта
    public boolean rainbow = false;   // Радужный режим
    
    // Позиции панелей (относительно экрана)
    public int sidebarX = 0;
    public int sidebarY = 28; // Отступ сверху для топ-бара
    public int contentX = 125; // SIDEBAR_WIDTH + отступ
    public int contentY = 35;
    
    // Состояние
    public int currentPanelMode = 0; // 0 = Modules, 1 = Settings, 2 = Blocks
    public int selectedCategory = 0;

    private static ThemeManager instance;
    private File configFile;

    private ThemeManager() {
        loadConfig();
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void loadConfig() {
        Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("freezdlc");
        configFile = configDir.resolve(CONFIG_FILE_NAME).toFile();

        if (configFile.exists()) {
            try (Reader reader = new FileReader(configFile)) {
                ThemeData data = GSON.fromJson(reader, ThemeData.class);
                if (data != null) {
                    this.hue = data.hue;
                    this.alpha = data.alpha;
                    this.borderRadius = data.borderRadius;
                    this.fontScale = data.fontScale;
                    this.rainbow = data.rainbow;
                    this.sidebarX = data.sidebarX;
                    this.sidebarY = data.sidebarY;
                    this.contentX = data.contentX;
                    this.contentY = data.contentY;
                    this.currentPanelMode = data.currentPanelMode;
                    this.selectedCategory = data.selectedCategory;
                }
            } catch (IOException e) {
                System.err.println("[freezdlc] Error loading theme config: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Создаем директорию и файл если нет
            try {
                Files.createDirectories(configDir);
                saveConfig();
            } catch (IOException e) {
                System.err.println("[freezdlc] Error creating config directory: " + e.getMessage());
            }
        }
    }

    public void saveConfig() {
        if (configFile == null) {
            Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("freezdlc");
            configFile = configDir.resolve(CONFIG_FILE_NAME).toFile();
        }

        ThemeData data = new ThemeData(
            this.hue, this.alpha, this.borderRadius, this.fontScale, this.rainbow,
            this.sidebarX, this.sidebarY, this.contentX, this.contentY,
            this.currentPanelMode, this.selectedCategory
        );

        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[freezdlc] Error saving theme config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Вспомогательный класс для сериализации
    private static class ThemeData {
        float hue;
        int alpha;
        int borderRadius;
        float fontScale;
        boolean rainbow;
        int sidebarX, sidebarY, contentX, contentY;
        int currentPanelMode, selectedCategory;

        public ThemeData(float h, int a, int br, float fs, boolean rb, int sx, int sy, int cx, int cy, int pm, int sc) {
            this.hue = h;
            this.alpha = a;
            this.borderRadius = br;
            this.fontScale = fs;
            this.rainbow = rb;
            this.sidebarX = sx;
            this.sidebarY = sy;
            this.contentX = cx;
            this.contentY = cy;
            this.currentPanelMode = pm;
            this.selectedCategory = sc;
        }
    }
    
    // Получить цвет с учетом прозрачности
    public int getColorWithAlpha(float h, float s, float b) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, b);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
