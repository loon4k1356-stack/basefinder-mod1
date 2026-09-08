package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import com.basefinder.module.Module;
import com.basefinder.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HudRenderer {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Позиции элементов (можно двигать в игре)
    public float logoX = 20, logoY = 20;
    public float infoX = 20, infoY = 60;
    public float arrayListX = 20, arrayListY = 100;

    // Настройки
    public boolean enabled = true;
    public boolean rainbow = true;
    public int customColor = 0xFF8A2BE2; // Фиолетовый по умолчанию
    public float scale = 1.0f;
    
    // Анимация
    private float animationTimer = 0;
    private long lastFrameTime = 0;

    // Drag & Drop состояние
    private boolean draggingLogo = false;
    private boolean draggingInfo = false;
    private boolean draggingArrayList = false;
    private double lastMouseX, lastMouseY;

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!enabled || mc.player == null || mc.options.hudHidden) return;

        long now = System.currentTimeMillis();
        animationTimer += (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        TextRenderer textRenderer = mc.textRenderer;
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        context.getMatrices().push();
        context.getMatrices().scale(scale, scale, scale);

        // Отрисовка логотипа
        renderLogo(context, textRenderer);
        
        // Отрисовка информации
        renderInfo(context, textRenderer);
        
        // Отрисовка списка модулей (ArrayList)
        renderArrayList(context, textRenderer, width, height);

        context.getMatrices().pop();
    }

    private void renderLogo(DrawContext ctx, TextRenderer tr) {
        String text = "freezdlc";
        int color = rainbow ? getRainbowColor() : customColor;
        
        // Тень
        ctx.drawTextWithShadow(tr, text, (int)(logoX + 1), (int)(logoY + 1), 0x40000000);
        // Текст
        ctx.drawTextWithShadow(tr, text, (int)logoX, (int)logoY, color);
        
        // Подчеркивание
        int textWidth = tr.getWidth(text);
        ctx.fill((int)logoX, (int)(logoY + tr.fontHeight + 2), (int)(logoX + textWidth), (int)(logoY + tr.fontHeight + 3), color);
    }

    private void renderInfo(DrawContext ctx, TextRenderer tr) {
        List<String> infoLines = new ArrayList<>();
        infoLines.add("FPS: " + MinecraftClient.getInstance().getCurrentFps());
        infoLines.add("Ping: " + (mc.player != null ? mc.player.networkHandler.getPlayerLatency() : 0) + "ms");
        infoLines.add("XYZ: " + String.format("%.1f", mc.player.getX()) + ", " + 
                                     String.format("%.1f", mc.player.getY()) + ", " + 
                                     String.format("%.1f", mc.player.getZ()));
        
        int maxWidth = 0;
        for (String line : infoLines) {
            maxWidth = Math.max(maxWidth, tr.getWidth(line));
        }

        int padding = 4;
        int bgHeight = infoLines.size() * tr.fontHeight + padding * 2;
        int bgColor = 0xAA000000; // Полупрозрачный черный
        int accentColor = rainbow ? getRainbowColor() : customColor;

        // Фон
        ctx.fill((int)infoX - padding, (int)infoY - padding, 
                 (int)infoX + maxWidth + padding, (int)infoY + bgHeight + padding, bgColor);
        
        // Акцентная полоска слева
        ctx.fill((int)infoX - padding, (int)infoY - padding, 
                 (int)infoX - padding + 3, (int)infoY + bgHeight + padding, accentColor);

        // Текст
        for (int i = 0; i < infoLines.size(); i++) {
            ctx.drawTextWithShadow(tr, infoLines.get(i), (int)infoX, (int)(infoY + i * tr.fontHeight), 0xFFFFFF);
        }
    }

    private void renderArrayList(DrawContext ctx, TextRenderer tr, int screenWidth, int screenHeight) {
        if (BaseFinderClient.moduleManager == null) return;

        List<Module> activeModules = BaseFinderClient.moduleManager.getModules().stream()
                .filter(Module::isEnabled)
                .toList();

        if (activeModules.isEmpty()) return;

        int yOffset = 0;
        int maxWidth = 0;

        // Считаем максимальную ширину для фона
        for (Module mod : activeModules) {
            int w = tr.getWidth(mod.getName());
            maxWidth = Math.max(maxWidth, w);
        }

        int padding = 3;
        int accentColor = rainbow ? getRainbowColor() : customColor;

        for (Module mod : activeModules) {
            String name = mod.getName();
            int width = tr.getWidth(name);
            int x = (int)arrayListX;
            int y = (int)(arrayListY + yOffset);

            // Фон под модуль
            ctx.fill(x - padding, y - padding, x + width + padding, y + tr.fontHeight + padding, 0xAA000000);
            
            // Акцент слева
            ctx.fill(x - padding, y - padding, x - padding + 2, y + tr.fontHeight + padding, mod.getColor() != null ? mod.getColor() : accentColor);

            // Текст
            ctx.drawTextWithShadow(tr, name, x, y, 0xFFFFFF);

            yOffset += tr.fontHeight + 2;
        }
    }

    public int getRainbowColor() {
        return Color.HSBtoRGB((animationTimer * 0.5f) % 1.0f, 0.7f, 1.0f);
    }

    // Обработка мыши для перетаскивания
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || button != 0) return false; // Только ЛКМ
        
        // Проверяем нажатие Right Shift для драга (чтобы не мешать кликам по чату)
        if (!GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
            return false;
        }

        // Масштабируем координаты мыши под масштаб HUD
        double scaledX = mouseX / scale;
        double scaledY = mouseY / scale;

        // Проверка попадания в логотип
        if (isHovered(scaledX, scaledY, logoX, logoY, 60, 20)) {
            draggingLogo = true;
            lastMouseX = scaledX;
            lastMouseY = scaledY;
            return true;
        }

        // Проверка попадания в инфо панель (примерные размеры)
        if (isHovered(scaledX, scaledY, infoX - 4, infoY - 4, 100, 60)) {
            draggingInfo = true;
            lastMouseX = scaledX;
            lastMouseY = scaledY;
            return true;
        }

        // Проверка попадания в ArrayList (верхняя часть)
        if (isHovered(scaledX, scaledY, arrayListX, arrayListY, 150, 100)) {
            draggingArrayList = true;
            lastMouseX = scaledX;
            lastMouseY = scaledY;
            return true;
        }

        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingLogo = false;
            draggingInfo = false;
            draggingArrayList = false;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!draggingLogo && !draggingInfo && !draggingArrayList) return false;
        
        double scaledX = mouseX / scale;
        double scaledY = mouseY / scale;

        if (draggingLogo) {
            logoX += (scaledX - lastMouseX);
            logoY += (scaledY - lastMouseY);
        } else if (draggingInfo) {
            infoX += (scaledX - lastMouseX);
            infoY += (scaledY - lastMouseY);
        } else if (draggingArrayList) {
            arrayListX += (scaledX - lastMouseX);
            arrayListY += (scaledY - lastMouseY);
        }

        lastMouseX = scaledX;
        lastMouseY = scaledY;
        return true;
    }

    private boolean isHovered(double mx, double my, float x, float y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
