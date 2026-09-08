package com.basefinder.gui;

import com.basefinder.module.Module;
import com.basefinder.module.ModuleManager;
import com.basefinder.BaseFinderClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;

public class ClickGUI extends Screen {

    private int selectedCategory = 0;
    private final int SIDEBAR_WIDTH = 120;
    private final int ITEM_HEIGHT = 20;

    public ClickGUI() {
        super(Text.literal("freezdlc"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Фон
        ctx.fill(0, 0, width, height, 0xCC0A0A0A);
        
        // Сайдбар
        ctx.fill(0, 0, SIDEBAR_WIDTH, height, 0xFF12121F);
        
        // Заголовок
        ctx.drawTextWithShadow(textRenderer, "freezdlc", 10, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, "v2.0", 10, 22, 0x888888);

        // Категории
        Module.Category[] cats = Module.Category.values();
        for (int i = 0; i < cats.length; i++) {
            int y = 60 + i * 25;
            boolean selected = (selectedCategory == i);
            ctx.drawTextWithShadow(textRenderer, cats[i].displayName, 15, y, selected ? 0x55FF55 : 0xAAAAAA);
            
            if (mouseX >= 0 && mouseX <= SIDEBAR_WIDTH && mouseY >= y && mouseY <= y + 20) {
                 // Подсветка при наведении (опционально)
            }
        }

        // Список модулей
        if (BaseFinderClient.moduleManager != null) {
            List<Module> modules = BaseFinderClient.moduleManager.getModulesByCategory(cats[selectedCategory]);
            int listX = SIDEBAR_WIDTH + 20;
            int listY = 60;
            
            ctx.drawTextWithShadow(textRenderer, cats[selectedCategory].displayName + " Modules", listX, 40, 0xFFFFFF);

            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int y = listY + i * 25;
                int color = mod.isEnabled() ? 0x55FF55 : 0xFFFFFF;
                String status = mod.isEnabled() ? "[ON]" : "[OFF]";
                
                ctx.drawTextWithShadow(textRenderer, mod.getName() + " " + status, listX, y, color);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Логика переключения категорий
        Module.Category[] cats = Module.Category.values();
        for (int i = 0; i < cats.length; i++) {
            int y = 60 + i * 25;
            if (mouseX >= 0 && mouseX <= SIDEBAR_WIDTH && mouseY >= y && mouseY <= y + 20) {
                selectedCategory = i;
                return true;
            }
        }

        // Логика включения модулей
        if (BaseFinderClient.moduleManager != null) {
            List<Module> modules = BaseFinderClient.moduleManager.getModulesByCategory(cats[selectedCategory]);
            int listX = SIDEBAR_WIDTH + 20;
            int listY = 60;
            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int y = listY + i * 25;
                if (mouseX >= listX && mouseY >= y && mouseX <= listX + 200 && mouseY <= y + 20) {
                    if (button == 0) { // ЛКМ
                        mod.toggle();
                        return true;
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        super.close();
    }
}
