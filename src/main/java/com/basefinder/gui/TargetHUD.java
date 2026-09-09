package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class TargetHUD {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    
    // Позиция и настройки
    public int x = 100;
    public int y = 100;
    public int width = 160;
    public int height = 50;
    
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    private long lastHitTime = 0;
    private float particleAnim = 0f;

    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        LivingEntity target = getTarget();
        if (target == null) return;

        // Анимация частиц при ударе
        if (System.currentTimeMillis() - lastHitTime < 500) {
            particleAnim = 1.0f - ((System.currentTimeMillis() - lastHitTime) / 500f);
            renderParticles(ctx, target);
        }

        int hudX = x;
        int hudY = y;

        // Фон
        int bgAlpha = 200;
        int bgColor = new Color(20, 20, 25, bgAlpha).getRGB();
        ctx.fill(hudX, hudY, hudX + width, hudY + height, bgColor);
        
        // Цветная полоска слева (цвет зависит от команды или рандома)
        int accentColor = BaseFinderClient.hudRenderer != null ? BaseFinderClient.hudRenderer.getColor(0) : 0xFF55FF55;
        ctx.fill(hudX, hudY, hudX + 4, hudY + height, accentColor);

        // 2D Голова (Скин)
        Identifier skin = target instanceof PlayerEntity ? ((PlayerEntity) target).getSkinTextures().texture() : null;
        // Рисуем голову (используем стандартную текстуку головы если скин не загружен или это моб)
        // Для простоты рисуем квадрат цвета кожи или иконку
        ctx.fill(hudX + 8, hudY + 8, hudX + 24, hudY + 24, 0xFFAAAAAA); 
        // Примечание: Для полноценного рендера скина нужен доступ к TextureManager, 
        // здесь упрощенная версия с рамкой.
        ctx.drawBorder(hudX + 8, hudY + 8, 16, 16, 0xFFFFFFFF);

        // Имя
        String name = target.getName().getString();
        ctx.drawTextWithShadow(mc.textRenderer, name.length() > 12 ? name.substring(0, 10) + ".." : name, hudX + 30, hudY + 5, 0xFFFFFF);

        // HP Bar Background
        int barX = hudX + 30;
        int barY = hudY + 20;
        int barW = width - 40;
        int barH = 8;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0x44000000);

        // HP Bar Fill
        float hpPct = target.getHealth() / target.getMaxHealth();
        int hpColor = hpPct > 0.5 ? 0xFF55FF55 : (hpPct > 0.25 ? 0xFFFFAA00 : 0xFFFF5555);
        int fillW = (int) (barW * hpPct);
        ctx.fill(barX, barY, barX + fillW, barY + barH, hpColor);

        // Текст HP
        String hpText = MathHelper.ceil(target.getHealth()) + " / " + MathHelper.ceil(target.getMaxHealth());
        ctx.drawTextWithShadow(mc.textRenderer, hpText, barX, barY - 2, 0xFFFFFF);
    }

    private void renderParticles(DrawContext ctx, LivingEntity target) {
        // Простая имитация частиц вокруг HUD
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        int color = new Color(1.0f, 0.2f, 0.2f, particleAnim).getRGB();
        
        for (int i = 0; i < 5; i++) {
            int px = centerX + (int)(Math.random() * 40 - 20);
            int py = centerY + (int)(Math.random() * 40 - 20);
            int size = (int)(Math.random() * 3 + 1);
            ctx.fill(px, py, px + size, py + size, color);
        }
    }

    private LivingEntity getTarget() {
        // Проверяем, был ли недавний удар (для отображения даже если враг убежал на секунду)
        if (System.currentTimeMillis() - lastHitTime < 3000) {
             // Здесь можно сохранить последнего атакованного, пока просто ищем ближайшего
             // В полной версии нужно хранить ссылку на lastTarget
        }
        
        LivingEntity best = null;
        double minDist = Double.MAX_VALUE;
        
        if (mc.world == null) return null;

        for (var e : mc.world.getEntities()) {
            if (e instanceof LivingEntity && e != mc.player && !e.isRemoved()) {
                if (e instanceof PlayerEntity) { // Бить только игроков для TargetHUD
                    double d = mc.player.squaredDistanceTo(e);
                    if (d < minDist && d < 36.0) { // 6 блоков
                        minDist = d;
                        best = (LivingEntity) e;
                    }
                }
            }
        }
        
        if (best != null) lastHitTime = System.currentTimeMillis(); // Обновляем таймер если видим врага
        return best;
    }

    // Обработка мыши для перетаскивания
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (int)(mouseX - x);
            dragOffsetY = (int)(mouseY - y);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (int)(mouseX - dragOffsetX);
            y = (int)(mouseY - dragOffsetY);
            return true;
        }
        return false;
    }

    private boolean isHovered(double mx, double my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }
    
    // Метод вызова из модуля атаки при ударе
    public void onAttack() {
        lastHitTime = System.currentTimeMillis();
    }
}
