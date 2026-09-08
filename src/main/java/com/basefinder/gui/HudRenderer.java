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
    
    // Позиции
    public int logoX = 20, logoY = 20;
    public int arrayListX = 20, arrayListY = 60;
    public int infoX = 20, infoY = -1;

    public boolean rainbow = true;
    public float hueOffset = 0f;
    public boolean shadow = true;

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
        int color = rainbow ? Color.HSBtoRGB(hueOffset, 0.8f, 1.0f) : 0xFF8A2BE2;
        if (shadow) ctx.drawTextWithShadow(mc.textRenderer, text, logoX + 1, logoY + 1, 0x000000);
        ctx.drawTextWithShadow(mc.textRenderer, text, logoX, logoY, color);
    }

    private void renderArrayList(DrawContext ctx) {
        if (BaseFinderClient.moduleManager == null) return;
        List<Module> active = new ArrayList<>();
        for (Module m : BaseFinderClient.moduleManager.getModules()) {
            if (m.isEnabled()) active.add(m);
        }
        active.sort((a, b) -> Integer.compare(mc.textRenderer.getWidth(b.getName()), mc.textRenderer.getWidth(a.getName())));

        int yOff = 0;
        for (Module mod : active) {
            String name = mod.getName();
            int w = mc.textRenderer.getWidth(name);
            int col = rainbow ? Color.HSBtoRGB((hueOffset + yOff * 0.05f) % 1.0f, 0.8f, 1.0f) : 0xFF8A2BE2;
            
            // Фон
            ctx.fill(arrayListX - 4, arrayListY + yOff - 2, arrayListX + w + 4, arrayListY + yOff + mc.textRenderer.fontHeight + 2, new Color(0,0,0,180).getRGB());
            // Полоска
            ctx.fill(arrayListX - 4, arrayListY + yOff - 2, arrayListX - 2, arrayListY + yOff + mc.textRenderer.fontHeight + 2, col);
            
            ctx.drawTextWithShadow(mc.textRenderer, name, arrayListX, arrayListY + yOff, 0xFFFFFF);
            yOff += mc.textRenderer.fontHeight + 3;
        }
    }

    private void renderInfoPanel(DrawContext ctx, int screenHeight) {
        int y = (infoY == -1) ? screenHeight - 40 : infoY;
        List<String> lines = new ArrayList<>();
        lines.add("FPS: " + MinecraftClient.getInstance().getCurrentFps());
        lines.add("XYZ: " + (int)mc.player.getX() + " " + (int)mc.player.getY() + " " + (int)mc.player.getZ());
        
        // Исправление получения пинга
        int ping = 0;
        if (mc.getNetworkHandler() != null && mc.player != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        lines.add("Ping: " + ping + "ms");

        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, mc.textRenderer.getWidth(s));

        ctx.fill(infoX - 5, y - 5, infoX + maxW + 5, y + (lines.size() * (mc.textRenderer.fontHeight + 2)) + 5, new Color(10,10,20,200).getRGB());
        
        for (int i = 0; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(": ");
            int lx = y + (i * (mc.textRenderer.fontHeight + 2));
            if (parts.length == 2) {
                int kw = mc.textRenderer.getWidth(parts[0] + ": ");
                ctx.drawTextWithShadow(mc.textRenderer, parts[0] + ": ", infoX, lx, 0xAAAAAA);
                ctx.drawTextWithShadow(mc.textRenderer, parts[1], infoX + kw, lx, 0xFFFFFF);
            }
        }
    }

    // Обработка мыши (упрощенная)
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (isHovered(mx, my, logoX, logoY, 100, 20)) { draggingLogo = true; dragOffsetX = (int)(mx-logoX); dragOffsetY = (int)(my-logoY); return true; }
        if (isHovered(mx, my, arrayListX, arrayListY, 200, 200)) { draggingArray = true; dragOffsetX = (int)(mx-arrayListX); dragOffsetY = (int)(my-arrayListY); return true; }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) { draggingLogo = false; draggingArray = false; return true; }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingLogo) { logoX = (int)(mx-dragOffsetX); logoY = (int)(my-dragOffsetY); }
        if (draggingArray) { arrayListX = (int)(mx-dragOffsetX); arrayListY = (int)(my-dragOffsetY); }
        return draggingLogo || draggingArray;
    }

    private boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x+w && my >= y && my <= y+h;
    }
}
