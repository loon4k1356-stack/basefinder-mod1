package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import com.basefinder.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HudRenderer {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    public int logoX = 20, logoY = 20;
    public int arrayListX = 20, arrayListY = 60;
    public int infoX = 20, infoY = -1;

    public boolean rainbow = true;
    public float hueOffset = 0f;
    public int customColor = 0xFF8A2BE2;
    public boolean shadow = true;
    public float scale = 1.0f;

    private long lastFrameTime = System.currentTimeMillis();
    private boolean draggingLogo = false, draggingArray = false, draggingInfo = false;
    private int dragOffsetX = 0, dragOffsetY = 0;

    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        float delta = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        if (rainbow) hueOffset = (hueOffset + delta * 0.5f) % 1.0f;

        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        renderLogo(ctx);
        renderArrayList(ctx);
        renderInfoPanel(ctx, height);
    }

    private void renderLogo(DrawContext ctx) {
        String text = "freezdlc";
        int color = getColor(0);
        if (shadow) ctx.drawTextWithShadow(mc.textRenderer, text, logoX + 1, logoY + 1, 0x000000);
        ctx.drawTextWithShadow(mc.textRenderer, text, logoX, logoY, color);
        int w = mc.textRenderer.getWidth(text);
        ctx.fill(logoX, logoY + mc.textRenderer.fontHeight + 2, logoX + w, logoY + mc.textRenderer.fontHeight + 3, color);
    }

    private void renderArrayList(DrawContext ctx) {
        if (BaseFinderClient.moduleManager == null) return;
        List<Module> active = new ArrayList<>();
        for (Module mod : BaseFinderClient.moduleManager.getModules()) {
            if (mod.isEnabled()) active.add(mod);
        }
        active.sort((a, b) -> Integer.compare(mc.textRenderer.getWidth(b.getName()), mc.textRenderer.getWidth(a.getName())));

        int yOffset = 0;
        for (int i = 0; i < active.size(); i++) {
            Module mod = active.get(i);
            String text = mod.getName();
            int color = getColor(i);
            int x = arrayListX;
            int y = arrayListY + yOffset;
            int w = mc.textRenderer.getWidth(text);
            
            ctx.fill(x - 4, y - 2, x + w + 4, y + mc.textRenderer.fontHeight + 2, new Color(0, 0, 0, 180).getRGB());
            ctx.fill(x - 4, y - 2, x - 2, y + mc.textRenderer.fontHeight + 2, color);
            
            if (shadow) ctx.drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFF);
            else ctx.drawText(mc.textRenderer, text, x, y, 0xFFFFFF);

            yOffset += mc.textRenderer.fontHeight + 3;
        }
    }

    private void renderInfoPanel(DrawContext ctx, int screenHeight) {
        int y = (infoY == -1) ? screenHeight - 40 : infoY;
        int x = infoX;

        int fps = MinecraftClient.getInstance().getCurrentFps();
        String coords = "XYZ: " + (int)mc.player.getX() + " " + (int)mc.player.getY() + " " + (int)mc.player.getZ();
        
        // Исправлено получение пинга
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        
        List<String> lines = List.of("FPS: " + fps, coords, "Ping: " + ping + "ms");
        
        int maxWidth = 0;
        for (String line : lines) maxWidth = Math.max(maxWidth, mc.textRenderer.getWidth(line));

        int bgAlpha = 200;
        int bgColor = new Color(10, 10, 20, bgAlpha).getRGB();
        int accent = getColor(5);

        ctx.fill(x - 5, y - 5, x + maxWidth + 5, y + (lines.size() * (mc.textRenderer.fontHeight + 2)) + 5, bgColor);
        ctx.fill(x - 5, y - 5, x + maxWidth + 5, y - 3, accent);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int ly = y + (i * (mc.textRenderer.fontHeight + 2));
            String[] parts = line.split(": ");
            if (parts.length == 2) {
                int kW = mc.textRenderer.getWidth(parts[0] + ": ");
                ctx.drawTextWithShadow(mc.textRenderer, parts[0] + ": ", x, ly, 0xAAAAAA);
                ctx.drawTextWithShadow(mc.textRenderer, parts[1], x + kW, ly, 0xFFFFFF);
            } else {
                ctx.drawTextWithShadow(mc.textRenderer, line, x, ly, 0xFFFFFF);
            }
        }
    }

    private int getColor(int index) {
        if (rainbow) return Color.HSBtoRGB((hueOffset + (index * 0.05f)) % 1.0f, 0.8f, 1.0f);
        return customColor;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (isHovered(mouseX, mouseY, logoX, logoY, mc.textRenderer.getWidth("freezdlc"), mc.textRenderer.fontHeight)) {
            draggingLogo = true; dragOffsetX = (int)(mouseX - logoX); dragOffsetY = (int)(mouseY - logoY); return true;
        }
        // Упрощенная проверка для списка (можно улучшить)
        if (isHovered(mouseX, mouseY, arrayListX, arrayListY, 100, 200)) { 
             draggingArray = true; dragOffsetX = (int)(mouseX - arrayListX); dragOffsetY = (int)(mouseY - arrayListY); return true;
        }
        
        int infoYReal = (infoY == -1) ? mc.getWindow().getScaledHeight() - 40 : infoY;
        if (isHovered(mouseX, mouseY, infoX, infoYReal, 100, 60)) {
            draggingInfo = true; dragOffsetX = (int)(mouseX - infoX); dragOffsetY = (int)(mouseY - infoYReal); return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingLogo = draggingArray = draggingInfo = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingLogo) { logoX = (int)(mouseX - dragOffsetX); logoY = (int)(mouseY - dragOffsetY); return true; }
        if (draggingArray) { arrayListX = (int)(mouseX - dragOffsetX); arrayListY = (int)(mouseY - dragOffsetY); return true; }
        if (draggingInfo) {
            infoX = (int)(mouseX - dragOffsetX);
            if (mouseY > mc.getWindow().getScaledHeight() - 50) infoY = -1;
            else infoY = (int)(mouseY - dragOffsetY);
            return true;
        }
        return false;
    }

    private boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    
    // Метод для обработки клавиш (вызывать из Mixin или главного класса при нажатии клавиш)
    public boolean shouldDrag() {
        // Исправлена логика проверки Shift
        return GLFW.glfwGetKey(mc.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}
