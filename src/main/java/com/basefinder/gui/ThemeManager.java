package com.basefinder.gui;

import java.awt.*;

public class ThemeManager {
    
    // Основные настройки темы
    public static float hue = 0.65f; // 0.65 = фиолетовый/синий
    public static float saturation = 0.8f;
    public static float brightness = 0.9f;
    
    public static int backgroundAlpha = 200; // Прозрачность фона (0-255)
    public static int panelAlpha = 220;      // Прозрачность панелей
    public static int elementAlpha = 255;    // Прозрачность элементов
    
    public static float borderRadius = 4.0f; // Скругление углов (пиксели)
    
    // Шрифт (пока просто флаг, т.к. Minecraft использует один стандартный, 
    // но мы можем имитировать жирность или размер в рендере)
    public static boolean useCustomFont = false; 
    public static float fontScale = 1.0f;

    // Получить основной цвет темы
    public static int getMainColor() {
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    // Получить цвет с другой яркостью (для ховеров и т.д.)
    public static int getColorVariant(float brightnessMod) {
        float newBright = Math.max(0.0f, Math.min(1.0f, brightness + brightnessMod));
        return Color.HSBtoRGB(hue, saturation, newBright);
    }

    // Получить цвет фона с прозрачностью
    public static int getBackgroundColor() {
        Color c = new Color(10, 10, 20); // Темно-синий черный
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), backgroundAlpha).getRGB();
    }

    // Получить цвет панели с прозрачностью
    public static int getPanelColor() {
        Color c = new Color(20, 20, 35);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), panelAlpha).getRGB();
    }

    // Получить цвет элемента (активный)
    public static int getElementColor() {
        Color c = new Color(getMainColor());
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), elementAlpha).getRGB();
    }
    
    // Генерация радужного цвета для конкретного индекса (для ArrayList)
    public static int getRainbowColor(int index, float offset) {
        float h = (hue + (index * 0.05f) + offset) % 1.0f;
        if (h < 0) h += 1.0f;
        return Color.HSBtoRGB(h, saturation, brightness);
    }

    // Сохранение настроек (заглушка, потом подключим к ConfigManager)
    public static void save() {
        // TODO: Интегрировать с ConfigManager.save()
        System.out.println("Theme settings saved (Hue: " + hue + ", Alpha: " + backgroundAlpha + ")");
    }
    
    // Загрузка настроек (заглушка)
    public static void load() {
        // TODO: Интегрировать с ConfigManager.load()
    }
}
