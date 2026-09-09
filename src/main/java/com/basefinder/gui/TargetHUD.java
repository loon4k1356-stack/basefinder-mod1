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
    
    // Позиция
    public int x = 100;
    public int y = 100;
    
    // Состояние
    private LivingEntity currentTarget = null;
    private long lastHitTime = 0;
    private float particleTimer = 0f;

    // Настройки стиля
    public boolean rainbow = true;
    public float hueOffset = 0f;

    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        float delta = tickCounter.getTickDelta(true);
        
        // Обновление радуги
        if (rainbow) hueOffset = (hueOffset + 0.005f) % 1.0f;

        // Поиск цели, если нет текущей или она мертва
        if (currentTarget == null || currentTarget.isDead() || currentTarget.getHealth() <= 0) {
            currentTarget = findNearestEnemy();
        }

        if (currentTarget == null) return;

        // Анимация исчезновения, если цель потеряна давно (опционально)
        // Но пока рисуем, пока цель жива

        int width = 160;
        int height = 50;
        int colorBg = new Color(0, 0, 0, 180).getRGB();
        int colorAccent = getColor(0);

        // Фон
        ctx.fill(x, y, x + width, y + height, colorBg);
        ctx.fill(x, y, x + width, y + 3, colorAccent); // Верхняя полоска

        // 2D Голова (Скин)
        Identifier skin = currentTarget instanceof PlayerEntity ? 
            ((PlayerEntity) currentTarget).getSkinTextures().texture() : null;
        
        // Рисуем голову (используем стандартный рендер лица из инвентаря для простоты или текстуру)
        // Для простоты нарисуем цветной квадрат с буквой или используем ItemRenderer для головы, 
        // но тут проще нарисовать "лицо" программно или использовать текстурный пак.
        // Самый надежный способ без сложных миксинов - нарисовать имя и HP, а вместо скина - цветной аватар.
        // НО ты просил скин. Используем DrawContext.drawTexture с координатами скина.
        
        if (skin != null) {
            // Координаты лица на скине (8, 8, размер 8x8) -> масштабируем до 40x40
            ctx.drawTexture(skin, x + 5, y + 5, 8, 8, 8, 8, 64, 64);
            // Если это второй слой (шлем/наушники), можно нарисовать сверху
             ctx.drawTexture(skin, x + 5, y + 5, 40, 8, 8, 8, 64, 64);
        } else {
            // Заглушка если скин не загружен
            ctx.fill(x + 5, y + 5, x + 45, y + 45, 0xFF555555);
        }

        // Имя
        String name = currentTarget.getName().getString();
        ctx.drawTextWithShadow(mc.textRenderer, name, x + 50, y + 5, 0xFFFFFF);

        // HP Bar
        float maxHp = currentTarget.getMaxHealth();
        float hp = currentTarget.getHealth();
        float hpPercent = MathHelper.clamp(hp / maxHp, 0.0f, 1.0f);
        
        int barX = x + 50;
        int barY = y + 20;
        int barW = 100;
        int barH = 10;

        // Фон бара
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        // Заполнение (цвет меняется от HP)
        int hpColor = hpPercent > 0.5 ? 0xFF00FF00 : (hpPercent > 0.25 ? 0xFFFFAA00 : 0xFFFF0000);
        ctx.fill(barX, barY, barX + (int)(barW * hpPercent), barY + barH, hpColor);

        // Текст HP
        String hpText = String.format("%.1f / %.1f", hp, maxHp);
        ctx.drawTextWithShadow(mc.textRenderer, hpText, barX, barY + 22, 0xAAAAAA);

        // Частицы при попадании
        if (now - lastHitTime < 500) { // 0.5 секунды после удара
            renderParticles(ctx, x + width / 2, y + height / 2, now - lastHitTime);
        }
    }

    private void renderParticles(DrawContext ctx, int centerX, int centerY, long timeOffset) {
        // Простая имитация частиц кружочками
        int count = 5;
        for (int i = 0; i < count; i++) {
            float angle = (timeOffset / 10.0f) + (i * (360 / count));
            double rad = Math.toRadians(angle);
            int radius = 20 + (int)(Math.sin(timeOffset / 50.0) * 10);
            
            int px = centerX + (int)(Math.cos(rad) * radius);
            int py = centerY + (int)(Math.sin(rad) * radius);
            
            ctx.fill(px - 2, py - 2, px + 2, py + 2, getColor(i));
        }
    }

    private LivingEntity findNearestEnemy() {
        if (mc.world == null) return null;
        LivingEntity nearest = null;
        double minDist = Double.MAX_VALUE;

        for (var e : mc.world.getEntities()) {
            if (e instanceof LivingEntity le && !le.isDead() && le != mc.player) {
                if (e instanceof PlayerEntity) {
                    double d = mc.player.squaredDistanceTo(e);
                    if (d < minDist) {
                        minDist = d;
                        nearest = le;
                    }
                }
            }
        }
        return nearest;
    }

    private int getColor(int index) {
        if (rainbow) {
            float hue = (hueOffset + (index * 0.05f)) % 1.0f;
            return Color.HSBtoRGB(hue, 0.8f, 1.0f);
        }
        return 0xFF00AAFF;
    }

    // Метод вызова из модуля KillAura при ударе
    public void onHit() {
        lastHitTime = System.currentTimeMillis();
    }
    
    // Обработка мыши для перетаскивания (аналогично HudRenderer)
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTarget == null) return false;
        // Упрощенная проверка попадания по области HUD
        if (mouseX >= x && mouseX <= x + 160 && mouseY >= y && mouseY <= y + 50) {
            if (button == 0) { // ЛКМ
                dragging = true;
                dragOffsetX = (int)(mouseX - x);
                dragOffsetY = (int)(mouseY - y);
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
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
}
