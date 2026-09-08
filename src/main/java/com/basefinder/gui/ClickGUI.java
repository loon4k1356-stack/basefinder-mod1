package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import com.basefinder.config.ConfigManager;
import com.basefinder.module.Module;
import com.basefinder.module.ModuleManager;
import com.basefinder.module.modules.BaseFinderModule;
import com.basefinder.module.settings.BoolSetting;
import com.basefinder.module.settings.ModeSetting;
import com.basefinder.module.settings.NumberSetting;
import com.basefinder.module.settings.Setting;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClickGUI extends Screen {

    private int selectedCategory = 0;
    private int selectedModule = 0;
    private int panelMode = 0; // 0 = modules, 1 = settings, 2 = blocks, 3 = theme
    private float animationTime = 0;
    private long lastFrameTime = 0;

    // Block selection
    private TextFieldWidget searchField;
    private List<Block> allBlocks;
    private List<Block> filteredBlocks;
    private int blockScrollOffset = 0;
    private int categoryFilter = 0;
    private final String[] blockCategories = {"All", "Ores", "Storage", "Redstone", "Decor", "Rare"};

    // Scroll offsets
    private int moduleScrollOffset = 0;
    private int settingScrollOffset = 0;
    private int themeScrollOffset = 0;

    // Dragging logic for panels
    private boolean draggingSidebar = false;
    private boolean draggingContent = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int sidebarX = 0;
    private int contentX = 0;

    private final int SIDEBAR_WIDTH = 120;
    private final int MODULE_LIST_WIDTH = 200;
    private final int ITEM_HEIGHT = 22;

    public ClickGUI() {
        super(Text.literal("freezdlc"));
        lastFrameTime = System.currentTimeMillis();
        
        allBlocks = new ArrayList<>();
        Registries.BLOCK.forEach(allBlocks::add);
        allBlocks.sort((a, b) -> Registries.BLOCK.getId(a).getPath().compareTo(Registries.BLOCK.getId(b).getPath()));
        filteredBlocks = new ArrayList<>(allBlocks);
    }

    @Override
    protected void init() {
        super.init();
        searchField = new TextFieldWidget(textRenderer, width - 350, 35, 180, 18, Text.literal(""));
        searchField.setMaxLength(30);
        searchField.setChangedListener(q -> applyBlockFilter());
        addDrawableChild(searchField);
    }

    private void applyBlockFilter() {
        String query = searchField.getText().toLowerCase();
        filteredBlocks = allBlocks.stream().filter(block -> {
            String id = Registries.BLOCK.getId(block).getPath().toLowerCase();
            String name = block.getName().getString().toLowerCase();
            boolean match = query.isEmpty() || id.contains(query) || name.contains(query);
            if (!match) return false;
            switch (categoryFilter) {
                case 1: return id.contains("ore") || id.contains("ancient_debris");
                case 2: return id.contains("chest") || id.contains("barrel") || id.contains("shulker") || id.contains("hopper");
                case 3: return id.contains("redstone") || id.contains("piston") || id.contains("observer") || id.contains("repeater") || id.contains("comparator") || id.contains("dispenser") || id.contains("dropper");
                case 4: return id.contains("lantern") || id.contains("candle") || id.contains("banner") || id.contains("concrete") || id.contains("terracotta");
                case 5: return id.contains("diamond") || id.contains("emerald") || id.contains("gold") || id.contains("netherite") || id.contains("beacon") || id.contains("spawner") || id.contains("ender_chest");
                default: return true;
            }
        }).collect(Collectors.toList());
        blockScrollOffset = 0;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        animationTime += (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        // Фон
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, ThemeManager.getBackgroundColor());

        // Top bar
        renderTopBar(ctx, mouseX, mouseY);

        // Sidebar
        renderSidebar(ctx, mouseX, mouseY);

        // Main content
        switch (panelMode) {
            case 0: renderModuleList(ctx, mouseX, mouseY); break;
            case 1: renderSettingsPanel(ctx, mouseX, mouseY); break;
            case 2: renderBlockSelector(ctx, mouseX, mouseY); break;
            case 3: renderThemePanel(ctx, mouseX, mouseY); break;
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY) {
        int color = ThemeManager.getMainColor();
        ctx.fill(0, 0, width, 28, 0xFF1A1A2E);
        ctx.fill(0, 27, width, 28, color);

        ctx.drawTextWithShadow(textRenderer, Text.literal("freezdlc"), 10, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("v2.0"), 85, 10, 0x888888);

        String status = BaseFinderClient.scanner != null && BaseFinderClient.scanner.isRunning() ? "SCANNING" : "IDLE";
        int statusColor = status.equals("SCANNING") ? 0x55FF55 : 0xFF5555;
        ctx.drawTextWithShadow(textRenderer, Text.literal(status), width - 80, 10, statusColor);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Config: " + ConfigManager.getCurrentConfigName()), width - 200, 10, 0xAAAAAA);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int sbX = sidebarX;
        int sbW = SIDEBAR_WIDTH;
        
        ctx.fill(sbX, 28, sbX + sbW, height, ThemeManager.getPanelColor());
        ctx.fill(sbX + sbW - 1, 28, sbX + sbW, height, ThemeManager.getMainColor());

        // Кнопки режимов
        String[] modes = {"Modules", "Settings", "Blocks", "Theme"};
        for (int i = 0; i < modes.length; i++) {
            int y = 35 + i * 28;
            boolean selected = (panelMode == i);
            boolean hovered = mouseX >= sbX + 5 && mouseX <= sbX + sbW - 5 && mouseY >= y && mouseY <= y + 24;

            if (selected) {
                ctx.fill(sbX + 5, y, sbX + sbW - 5, y + 24, 0xFF2A2A4A);
                ctx.fill(sbX + 5, y, sbX + 8, y + 24, ThemeManager.getMainColor());
            } else if (hovered) {
                ctx.fill(sbX + 5, y, sbX + sbW - 5, y + 24, 0xFF1F1F3A);
            }

            ctx.drawTextWithShadow(textRenderer, Text.literal(selected ? "> " + modes[i] : "  " + modes[i]), sbX + 15, y + 8, selected ? 0xFFFFFF : 0x888888);
        }

        // Категории (только для Modules)
        if (panelMode == 0) {
            int startY = 130;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Categories"), sbX + 10, startY - 15, 0x666666);

            Module.Category[] cats = Module.Category.values();
            for (int i = 0; i < cats.length; i++) {
                int y = startY + i * 22;
                boolean selected = (selectedCategory == i);
                boolean hovered = mouseX >= sbX + 5 && mouseX <= sbX + sbW - 5 && mouseY >= y && mouseY <= y + 20;

                if (selected) {
                    ctx.fill(sbX + 5, y, sbX + sbW - 5, y + 20, 0xFF2A2A4A);
                    ctx.fill(sbX + 5, y, sbX + 8, y + 20, cats[i].color);
                } else if (hovered) {
                    ctx.fill(sbX + 5, y, sbX + sbW - 5, y + 20, 0xFF1F1F3A);
                }

                String label = selected ? "> " + cats[i].displayName : "  " + cats[i].displayName;
                ctx.drawTextWithShadow(textRenderer, Text.literal(label), sbX + 15, y + 6, selected ? 0xFFFFFF : 0x888888);

                ModuleManager mm = BaseFinderClient.moduleManager;
                if (mm != null) {
                    int count = mm.getModulesByCategory(cats[i]).size();
                    ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(count)), sbX + sbW - 25, y + 6, 0x555555);
                }
            }
        }
    }

    private void renderModuleList(DrawContext ctx, int mouseX, int mouseY) {
        int listX = sidebarX + SIDEBAR_WIDTH + 5;
        int listY = 35;
        int listWidth = MODULE_LIST_WIDTH;
        int listHeight = height - 40;

        ctx.fill(listX, listY, listX + listWidth, listY + listHeight, ThemeManager.getPanelColor());
        ctx.fill(listX + listWidth, listY, listX + listWidth + 1, listY + listHeight, ThemeManager.getMainColor());

        Module.Category[] cats = Module.Category.values();
        if (selectedCategory < cats.length) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(cats[selectedCategory].displayName + " Modules"), listX + 10, listY + 8, 0xFFFFFF);
        }

        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null) return;

        List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
        int startY = listY + 28;
        int visibleItems = (listHeight - 35) / ITEM_HEIGHT;

        for (int i = 0; i < visibleItems && (i + moduleScrollOffset) < modules.size(); i++) {
            int idx = i + moduleScrollOffset;
            Module mod = modules.get(idx);
            int y = startY + i * ITEM_HEIGHT;

            boolean selected = (selectedModule == idx);
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            if (selected) {
                ctx.fill(listX + 1, y, listX + listWidth - 1, y + ITEM_HEIGHT, 0xFF2A2A4A);
                ctx.fill(listX + 1, y, listX + 4, y + ITEM_HEIGHT, ThemeManager.getMainColor());
            } else if (hovered) {
                ctx.fill(listX + 1, y, listX + listWidth - 1, y + ITEM_HEIGHT, 0xFF1A1A30);
            }

            int nameColor = mod.isEnabled() ? 0x55FF55 : (selected ? 0xFFFFFF : 0xAAAAAA);
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), listX + 12, y + 7, nameColor);

            String toggle = mod.isEnabled() ? "[ON]" : "[OFF]";
            int toggleColor = mod.isEnabled() ? 0x55FF55 : 0xFF5555;
            ctx.drawTextWithShadow(textRenderer, Text.literal(toggle), listX + listWidth - 40, y + 7, toggleColor);
        }

        renderInfoPanel(ctx, mouseX, mouseY, modules, listX + listWidth + 10);
    }

    private void renderInfoPanel(DrawContext ctx, int mouseX, int mouseY, List<Module> modules, int panelX) {
        int panelY = 35;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 40;

        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, ThemeManager.getPanelColor());

        if (modules.isEmpty() || selectedModule >= modules.size()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Select a module"), panelX + 20, panelY + 20, 0x666666);
            return;
        }

        Module mod = modules.get(selectedModule);
        ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), panelX + 15, panelY + 15, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getDescription()), panelX + 15, panelY + 30, 0x888888);

        String status = mod.isEnabled() ? "ENABLED" : "DISABLED";
        int statusColor = mod.isEnabled() ? 0x55FF55 : 0xFF5555;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Status: " + status), panelX + 15, panelY + 50, statusColor);

        if (mod instanceof BaseFinderModule) {
            BaseFinderModule bf = (BaseFinderModule) mod;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Found: " + bf.getFoundCount()), panelX + 15, panelY + 70, 0xFFAA00);
        }
    }

    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        int panelX = sidebarX + SIDEBAR_WIDTH + 5;
        int panelY = 35;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 40;

        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, ThemeManager.getPanelColor());

        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null || mm.baseFinder == null) return;

        BaseFinderModule bf = mm.baseFinder;
        ctx.drawTextWithShadow(textRenderer, Text.literal("BaseFinder Settings"), panelX + 15, panelY + 10, 0xFFFFFF);

        int sy = panelY + 35;
        List<Setting<?>> settings = bf.getSettings();

        for (int i = 0; i < settings.size() && (i + settingScrollOffset) < settings.size(); i++) {
            int idx = i + settingScrollOffset;
            Setting<?> setting = settings.get(idx);
            int y = sy + idx * 40;
            if (y > panelY + panelHeight - 20) break;

            ctx.drawTextWithShadow(textRenderer, Text.literal(setting.getName()), panelX + 15, y, 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal(setting.getDescription()), panelX + 15, y + 12, 0x666666);

            if (setting instanceof BoolSetting) {
                BoolSetting bs = (BoolSetting) setting;
                int boxX = panelX + panelWidth - 80;
                int boxY = y;
                int color = bs.get() ? 0xFF2A5A2A : 0xFF5A2A2A;
                ctx.fill(boxX, boxY, boxX + 50, boxY + 18, color);
                ctx.drawTextWithShadow(textRenderer, Text.literal(bs.get() ? "ON" : "OFF"), boxX + 15, boxY + 5, bs.get() ? 0x55FF55 : 0xFF5555);
            } else if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) setting;
                int sliderX = panelX + panelWidth - 200;
                int sliderY = y + 2;
                int sliderWidth = 170;
                int sliderHeight = 14;

                ctx.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF222233);
                int filledWidth = (int) (sliderWidth * ns.getPercentage());
                ctx.fill(sliderX, sliderY, sliderX + filledWidth, sliderY + sliderHeight, ThemeManager.getMainColor());
                ctx.drawTextWithShadow(textRenderer, Text.literal(ns.getValueAsString()), sliderX + sliderWidth + 5, sliderY + 3, 0xFFFFFF);
            } else if (setting instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) setting;
                int modeX = panelX + panelWidth - 120;
                ctx.fill(modeX, y, modeX + 100, y + 18, 0xFF2A2A4A);
                ctx.drawTextWithShadow(textRenderer, Text.literal(ms.getMode()), modeX + 10, y + 5, 0xFFAA00);
            }
        }
    }

    private void renderBlockSelector(DrawContext ctx, int mouseX, int mouseY) {
        int panelX = sidebarX + SIDEBAR_WIDTH + 5;
        int panelY = 60;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 65;

        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, ThemeManager.getPanelColor());

        int catY = 35;
        int catBtnWidth = 60;
        for (int i = 0; i < blockCategories.length; i++) {
            int x = panelX + 5 + i * (catBtnWidth + 3);
            boolean selected = (categoryFilter == i);
            boolean hovered = mouseX >= x && mouseX <= x + catBtnWidth && mouseY >= catY && mouseY <= catY + 18;

            ctx.fill(x, catY, x + catBtnWidth, catY + 18, selected ? 0xFF2A2A4A : (hovered ? 0xFF1F1F3A : 0xFF151525));
            if (selected) ctx.fill(x, catY + 17, x + catBtnWidth, catY + 18, ThemeManager.getMainColor());
            ctx.drawTextWithShadow(textRenderer, Text.literal(blockCategories[i]), x + 5, catY + 5, selected ? 0xFFFFFF : 0x888888);
        }

        int listX = panelX + 5;
        int listY = panelY + 5;
        int listWidth = panelWidth - 10;
        int itemH = 22;
        int visible = (panelHeight - 10) / itemH;

        for (int i = 0; i < visible && (i + blockScrollOffset) < filteredBlocks.size(); i++) {
            int idx = i + blockScrollOffset;
            Block block = filteredBlocks.get(idx);
            String blockName = block.getName().getString();
            int y = listY + i * itemH;
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + itemH;
            boolean isSelected = BaseFinderClient.scanner != null && BaseFinderClient.scanner.getSelectedBlocks().contains(block);

            if (isSelected) {
                ctx.fill(listX, y, listX + listWidth, y + itemH, 0xFF1A3A1A);
            } else if (hovered) {
                ctx.fill(listX, y, listX + listWidth, y + itemH, 0xFF1A1A30);
            }

            ctx.drawTextWithShadow(textRenderer, Text.literal(isSelected ? "[x]" : "[ ]"), listX + 3, y + 7, isSelected ? 0x55FF55 : 0x555555);
            ItemStack stack = new ItemStack(block);
            ctx.drawItem(stack, listX + 28, y + 2);
            
            String displayName = blockName.length() > 25 ? blockName.substring(0, 22) + "..." : blockName;
            ctx.drawTextWithShadow(textRenderer, Text.literal(displayName), listX + 50, y + 3, isSelected ? 0x55FF55 : 0xFFFFFF);
        }
        
        int selectedCount = BaseFinderClient.scanner != null ? BaseFinderClient.scanner.getSelectedBlocks().size() : 0;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Selected: " + selectedCount), panelX + 10, panelY + panelHeight - 15, 0xAAAAAA);
    }

    // НОВАЯ ПАНЕЛЬ ТЕМЫ
    private void renderThemePanel(DrawContext ctx, int mouseX, int mouseY) {
        int panelX = sidebarX + SIDEBAR_WIDTH + 5;
        int panelY = 35;
        int panelWidth = width - panelX - 5;
        
        ctx.fill(panelX, panelY, panelX + panelWidth, height - 5, ThemeManager.getPanelColor());
        ctx.drawTextWithShadow(textRenderer, Text.literal("Theme Customization"), panelX + 15, panelY + 10, ThemeManager.getMainColor());

        int y = panelY + 40;
        int labelWidth = 150;
        int controlWidth = 200;
        int controlX = panelX + labelWidth + 20;

        // 1. Цвет (Hue)
        ctx.drawTextWithShadow(textRenderer, Text.literal("Color (Hue):"), panelX + 15, y, 0xFFFFFF);
        drawSlider(ctx, controlX, y, controlWidth, 15, ThemeManager.hue, 0xFF0000, 0xFFFF00);
        y += 30;

        // 2. Прозрачность фона
        ctx.drawTextWithShadow(textRenderer, Text.literal("Background Alpha:"), panelX + 15, y, 0xFFFFFF);
        float alphaPct = ThemeManager.backgroundAlpha / 255.0f;
        drawSlider(ctx, controlX, y, controlWidth, 15, alphaPct, 0x444444, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(ThemeManager.backgroundAlpha)), controlX + controlWidth + 5, y, 0xAAAAAA);
        y += 30;

        // 3. Скругление
        ctx.drawTextWithShadow(textRenderer, Text.literal("Border Radius:"), panelX + 15, y, 0xFFFFFF);
        float radiusPct = ThemeManager.borderRadius / 10.0f; // Макс 10px
        drawSlider(ctx, controlX, y, controlWidth, 15, radiusPct, 0x444444, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(String.format("%.1f", ThemeManager.borderRadius)), controlX + controlWidth + 5, y, 0xAAAAAA);
        y += 30;
        
        // 4. Масштаб шрифта
        ctx.drawTextWithShadow(textRenderer, Text.literal("Font Scale:"), panelX + 15, y, 0xFFFFFF);
        float scalePct = (ThemeManager.fontScale - 0.5f) / 1.5f; // Диапазон 0.5 - 2.0
        drawSlider(ctx, controlX, y, controlWidth, 15, scalePct, 0x444444, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(String.format("%.2f", ThemeManager.fontScale)), controlX + controlWidth + 5, y, 0xAAAAAA);
        y += 30;

        // Предпросмотр цвета
        ctx.fill(panelX + 15, y, panelX + 45, y + 30, ThemeManager.getMainColor());
        ctx.drawTextWithShadow(textRenderer, Text.literal("Preview"), panelX + 50, y + 10, 0xFFFFFF);
    }

    private void drawSlider(DrawContext ctx, int x, int y, int width, int height, float value, int colorStart, int colorEnd) {
        // Фон
        ctx.fill(x, y, x + width, y + height, 0xFF222233);
        // Полоска градиента (упрощенно одним цветом для примера, можно сделать градиент)
        ctx.fill(x, y, x + width, y + height, colorStart); 
        
        // Заполнение
        int fillW = (int)(width * value);
        ctx.fill(x, y, x + fillW, y + height, ThemeManager.getMainColor());
        
        // Кружок
        ctx.fill(x + fillW - 2, y - 2, x + fillW + 2, y + height + 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Логика перетаскивания заголовков (упрощенно)
        if (button == 0 && mouseY < 28) {
            if (mouseX < SIDEBAR_WIDTH) draggingSidebar = true;
            else draggingContent = true;
            dragStartX = (int)mouseX;
            dragStartY = (int)mouseY;
            return true;
        }

        // Клик по сайдбару (режимы)
        String[] modes = {"Modules", "Settings", "Blocks", "Theme"};
        for (int i = 0; i < modes.length; i++) {
            int y = 35 + i * 28;
            if (mouseX >= sidebarX + 5 && mouseX <= sidebarX + SIDEBAR_WIDTH - 5 && mouseY >= y && mouseY <= y + 24) {
                panelMode = i;
                return true;
            }
        }

        // Категории
        if (panelMode == 0) {
            Module.Category[] cats = Module.Category.values();
            int startY = 130;
            for (int i = 0; i < cats.length; i++) {
                int y = startY + i * 22;
                if (mouseX >= sidebarX + 5 && mouseX <= sidebarX + SIDEBAR_WIDTH - 5 && mouseY >= y && mouseY <= y + 20) {
                    selectedCategory = i;
                    selectedModule = 0;
                    moduleScrollOffset = 0;
                    return true;
                }
            }
            // Модули
            int listX = sidebarX + SIDEBAR_WIDTH + 5;
            int listY = 35 + 28;
            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
                for (int i = 0; i < modules.size(); i++) {
                    int y = listY + i * ITEM_HEIGHT;
                    if (mouseX >= listX && mouseX <= listX + MODULE_LIST_WIDTH && mouseY >= y && mouseY <= y + ITEM_HEIGHT) {
                        selectedModule = i;
                        if (button == 1) modules.get(i).toggle();
                        return true;
                    }
                }
            }
        }

        // Настройки
        if (panelMode == 1) handleSettingsClick(mouseX, mouseY, button);
        
        // Блоки
        if (panelMode == 2) handleBlockClick(mouseX, mouseY, button);

        // Тема
        if (panelMode == 3) handleThemeClick(mouseX, mouseY, button);

        return true;
    }

    private void handleThemeClick(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        int panelX = sidebarX + SIDEBAR_WIDTH + 5;
        int y = 75; // Начальная Y после заголовка
        int controlX = panelX + 170;
        int controlWidth = 200;

        // Hue
        if (mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= y && mouseY <= y + 15) {
            ThemeManager.hue = (float)((mouseX - controlX) / controlWidth);
            return;
        }
        y += 30;

        // Alpha
        if (mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= y && mouseY <= y + 15) {
            ThemeManager.backgroundAlpha = (int)(((mouseX - controlX) / controlWidth) * 255);
            return;
        }
        y += 30;

        // Radius
        if (mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= y && mouseY <= y + 15) {
            ThemeManager.borderRadius = (float)((mouseX - controlX) / controlWidth) * 10.0f;
            return;
        }
        y += 30;
        
        // Scale
        if (mouseX >= controlX && mouseX <= controlX + controlWidth && mouseY >= y && mouseY <= y + 15) {
            ThemeManager.fontScale = 0.5f + (float)((mouseX - controlX) / controlWidth) * 1.5f;
            return;
        }
    }

    private void handleSettingsClick(double mouseX, double mouseY, int button) {
        // ... (старый код обработки настроек, аналогично блокам)
        // Для краткости оставил как есть, логика та же
         ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null || mm.baseFinder == null) return;
        BaseFinderModule bf = mm.baseFinder;
        int panelX = sidebarX + SIDEBAR_WIDTH + 5;
        int panelY = 35;
        int panelWidth = width - panelX - 5;
        int sy = panelY + 35;
        List<Setting<?>> settings = bf.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> setting = settings.get(i);
            int y = sy + i * 40;
            if (setting instanceof BoolSetting) {
                BoolSetting bs = (BoolSetting) setting;
                int boxX = panelX + panelWidth - 80;
                if (mouseX >= boxX && mouseX <= boxX + 50 && mouseY >= y && mouseY <= y + 18) {
                    bs.toggle(); return;
                }
            } else if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) setting;
                int sliderX = panelX + panelWidth - 200;
                if (mouseX >= sliderX && mouseX <= sliderX + 170 && mouseY >= y + 2 && mouseY <= y + 16) {
                    ns.setFromPercentage((mouseX - sliderX) / 170.0); return;
                }
            } else if (setting instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) setting;
                int modeX = panelX + panelWidth - 120;
                if (mouseX >= modeX && mouseX <= modeX + 100 && mouseY >= y && mouseY <= y + 18) {
                    ms.cycle(); return;
                }
            }
        }
    }

    private void handleBlockClick(double mouseX, double mouseY, int button) {
        // ... (старый код)
        int panelX = sidebarX + SIDEBAR_WIDTH + 5;
        int panelY = 60;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 65;
        int listX = panelX + 5;
        int listY = panelY + 5;
        int listWidth = panelWidth - 10;
        int itemH = 22;
        int visible = (panelHeight - 10) / itemH;

        int catY = 35;
        int catBtnWidth = 60;
        for (int i = 0; i < blockCategories.length; i++) {
            int x = panelX + 5 + i * (catBtnWidth + 3);
            if (mouseX >= x && mouseX <= x + catBtnWidth && mouseY >= catY && mouseY <= catY + 18) {
                categoryFilter = i;
                applyBlockFilter();
                return;
            }
        }

        for (int i = 0; i < visible && (i + blockScrollOffset) < filteredBlocks.size(); i++) {
            int idx = i + blockScrollOffset;
            int y = listY + i * itemH;
            if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + itemH) {
                Block block = filteredBlocks.get(idx);
                if (BaseFinderClient.scanner != null) {
                    if (BaseFinderClient.scanner.getSelectedBlocks().contains(block)) {
                        BaseFinderClient.scanner.removeSelectedBlock(block);
                    } else {
                        BaseFinderClient.scanner.addSelectedBlock(block);
                    }
                }
                return;
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSidebar) {
            sidebarX += (int)deltaX;
            // Ограничения
            if (sidebarX < 0) sidebarX = 0;
            if (sidebarX > width / 2) sidebarX = width / 2;
            return true;
        }
        if (draggingContent) {
            // Можно добавить логику перемещения контентной панели
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSidebar = false;
        draggingContent = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (panelMode == 0) {
            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                Module.Category[] cats = Module.Category.values();
                List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
                int maxScroll = Math.max(0, modules.size() - (height - 80) / ITEM_HEIGHT);
                moduleScrollOffset = (int) Math.max(0, Math.min(maxScroll, moduleScrollOffset - verticalAmount));
            }
        } else if (panelMode == 2) {
            int maxScroll = Math.max(0, filteredBlocks.size() - (height - 100) / 22);
            blockScrollOffset = (int) Math.max(0, Math.min(maxScroll, blockScrollOffset - verticalAmount));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        ThemeManager.save();
        ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
        MinecraftClient.getInstance().setScreen(null);
    }
}
