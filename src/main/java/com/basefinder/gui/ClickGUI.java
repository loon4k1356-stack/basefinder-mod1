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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClickGUI extends Screen {

    private int selectedCategory = 0;
    private int selectedModule = 0;
    private int panelMode = 0; // 0 = modules, 1 = settings, 2 = blocks
    
    // Масштабирование
    private float scale = 1.0f;
    private final float MIN_SCALE = 0.5f;
    private final float MAX_SCALE = 2.0f;

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

    // Базовые размеры (будут умножаться на scale)
    private final int SIDEBAR_WIDTH_BASE = 120;
    private final int MODULE_LIST_WIDTH_BASE = 200;
    private final int ITEM_HEIGHT_BASE = 22;

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
        // Поле поиска тоже нужно масштабировать визуально, но его позиция задается в пикселях экрана
        // Мы будем пересчитывать его позицию при рендере или просто оставим как есть, 
        // но лучше пересоздавать его при изменении размера окна, если нужно строго.
        // Для простоты оставим стандартную инициализацию, а координаты будем корректировать в render/click
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

        // Темный фон поверх всего экрана (без масштаба)
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, 0xCC0A0A0A);

        MatrixStack matrices = ctx.getMatrices();
        matrices.push();
        
        // Применяем масштабирование от центра или от левого верхнего угла?
        // Обычно GUI масштабируют от левого верхнего угла для простоты логики координат
        matrices.scale(scale, scale, 1.0f);

        // Корректируем координаты мыши с учетом масштаба для логики отрисовки внутри матрицы
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        // Top bar
        renderTopBar(ctx, scaledMouseX, scaledMouseY);

        // Sidebar with categories
        renderSidebar(ctx, scaledMouseX, scaledMouseY);

        // Main content
        switch (panelMode) {
            case 0: renderModuleList(ctx, scaledMouseX, scaledMouseY); break;
            case 1: renderSettingsPanel(ctx, scaledMouseX, scaledMouseY); break;
            case 2: renderBlockSelector(ctx, scaledMouseX, scaledMouseY); break;
        }

        matrices.pop();

        // Рендерим поле поиска поверх масштаба (оно не масштабируется вместе с GUI, чтобы текст оставался четким)
        // Или можно масштабировать и его, но тогда нужно менять его позицию в init.
        // Для простоты оставим его как отдельный элемент, но если он попадает под матрицу, могут быть артефакты.
        // Лучший вариант для TextFieldWidget в масштабированном GUI - рендерить его отдельно после pop(),
        // но тогда его координаты должны совпадать с визуальным положением.
        // В данном случае, так как searchField добавлен через addDrawableChild, Minecraft сам его отрисурует после этого метода.
        // Нам нужно только убедиться, что он не перекрывается фоном. 
        
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY) {
        int h = (int)(28 * scale); // Высота топбара с учетом масштаба, но так как мы внутри matrix.scale, рисуем базовые значения
        // Внутри matrix.scale мы используем базовые координаты (как будто scale=1)
        
        ctx.fill(0, 0, width, 28, 0xFF1A1A2E);
        ctx.fill(0, 27, width, 28, getAccentColor());

        ctx.drawTextWithShadow(textRenderer, Text.literal("freezdlc"), 10, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("v2.0"), 85, 10, 0x888888);

        String status = BaseFinderClient.scanner != null && BaseFinderClient.scanner.isRunning() ? "SCANNING" : "IDLE";
        int statusColor = status.equals("SCANNING") ? 0x55FF55 : 0xFF5555;
        ctx.drawTextWithShadow(textRenderer, Text.literal(status), width - 80, 10, statusColor);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Config: " + ConfigManager.getCurrentConfigName()), width - 200, 10, 0xAAAAAA);
        
        // Подсказка по масштабу
        String scaleInfo = String.format("Scale: %.1fx (Ctrl+Scroll)", scale);
        ctx.drawTextWithShadow(textRenderer, Text.literal(scaleInfo), 10, 28 + 5, 0x5555FF);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int SIDEBAR_WIDTH = SIDEBAR_WIDTH_BASE;
        int ITEM_HEIGHT = ITEM_HEIGHT_BASE;

        ctx.fill(0, 28, SIDEBAR_WIDTH, height, 0xFF12121F);
        ctx.fill(SIDEBAR_WIDTH - 1, 28, SIDEBAR_WIDTH, height, 0xFF333355);

        String[] modes = {"Modules", "Settings", "Blocks"};
        for (int i = 0; i < modes.length; i++) {
            int y = 35 + i * 28;
            boolean selected = (panelMode == i);
            boolean hovered = mouseX >= 5 && mouseX <= SIDEBAR_WIDTH - 5 && mouseY >= y && mouseY <= y + 24;

            if (selected) {
                ctx.fill(5, y, SIDEBAR_WIDTH - 5, y + 24, 0xFF2A2A4A);
                ctx.fill(5, y, 8, y + 24, getAccentColor());
            } else if (hovered) {
                ctx.fill(5, y, SIDEBAR_WIDTH - 5, y + 24, 0xFF1F1F3A);
            }

            ctx.drawTextWithShadow(textRenderer, Text.literal(selected ? "> " + modes[i] : "  " + modes[i]), 15, y + 8, selected ? 0xFFFFFF : 0x888888);
        }

        if (panelMode == 0) {
            int startY = 130;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Categories"), 10, startY - 15, 0x666666);

            Module.Category[] cats = Module.Category.values();
            for (int i = 0; i < cats.length; i++) {
                int y = startY + i * 22;
                boolean selected = (selectedCategory == i);
                boolean hovered = mouseX >= 5 && mouseX <= SIDEBAR_WIDTH - 5 && mouseY >= y && mouseY <= y + 20;

                if (selected) {
                    ctx.fill(5, y, SIDEBAR_WIDTH - 5, y + 20, 0xFF2A2A4A);
                    ctx.fill(5, y, 8, y + 20, cats[i].color);
                } else if (hovered) {
                    ctx.fill(5, y, SIDEBAR_WIDTH - 5, y + 20, 0xFF1F1F3A);
                }

                String label = selected ? "> " + cats[i].displayName : "  " + cats[i].displayName;
                ctx.drawTextWithShadow(textRenderer, Text.literal(label), 15, y + 6, selected ? 0xFFFFFF : 0x888888);

                ModuleManager mm = BaseFinderClient.moduleManager;
                if (mm != null) {
                    int count = mm.getModulesByCategory(cats[i]).size();
                    ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(count)), SIDEBAR_WIDTH - 25, y + 6, 0x555555);
                }
            }
        }
    }

    private void renderModuleList(DrawContext ctx, int mouseX, int mouseY) {
        int SIDEBAR_WIDTH = SIDEBAR_WIDTH_BASE;
        int MODULE_LIST_WIDTH = MODULE_LIST_WIDTH_BASE;
        int ITEM_HEIGHT = ITEM_HEIGHT_BASE;

        int listX = SIDEBAR_WIDTH + 5;
        int listY = 35;
        int listWidth = MODULE_LIST_WIDTH;
        int listHeight = height - 40;

        ctx.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF0F0F1A);
        ctx.fill(listX + listWidth, listY, listX + listWidth + 1, listY + listHeight, 0xFF333355);

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
                ctx.fill(listX + 1, y, listX + 4, y + ITEM_HEIGHT, getAccentColor());
            } else if (hovered) {
                ctx.fill(listX + 1, y, listX + listWidth - 1, y + ITEM_HEIGHT, 0xFF1A1A30);
            }

            int nameColor = mod.isEnabled() ? 0x55FF55 : (selected ? 0xFFFFFF : 0xAAAAAA);
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), listX + 12, y + 7, nameColor);

            String toggle = mod.isEnabled() ? "[ON]" : "[OFF]";
            int toggleColor = mod.isEnabled() ? 0x55FF55 : 0xFF5555;
            ctx.drawTextWithShadow(textRenderer, Text.literal(toggle), listX + listWidth - 40, y + 7, toggleColor);
        }

        renderInfoPanel(ctx, mouseX, mouseY, modules);
    }

    private void renderInfoPanel(DrawContext ctx, int mouseX, int mouseY, List<Module> modules) {
        int SIDEBAR_WIDTH = SIDEBAR_WIDTH_BASE;
        int MODULE_LIST_WIDTH = MODULE_LIST_WIDTH_BASE;

        int panelX = SIDEBAR_WIDTH + MODULE_LIST_WIDTH + 10;
        int panelY = 35;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 40;

        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF0F0F1A);

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
            ctx.drawTextWithShadow(textRenderer, Text.literal("Found: " + bf.getFoundCount() + " blocks"), panelX + 15, panelY + 70, 0xFFAA00);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Selected: " + bf.getSelectedCount() + " blocks"), panelX + 15, panelY + 85, 0xFFAA00);
        }

        if (mod instanceof BaseFinderModule) {
            BaseFinderModule bf = (BaseFinderModule) mod;
            int sy = panelY + 110;
            ctx.drawTextWithShadow(textRenderer, Text.literal("--- Quick Settings ---"), panelX + 15, sy, 0x666666);
            sy += 18;

            for (Setting<?> s : bf.getSettings()) {
                ctx.drawTextWithShadow(textRenderer, Text.literal(s.getName() + ": " + s.getValueAsString()), panelX + 15, sy, 0xCCCCCC);
                sy += 15;
            }
        }
    }

    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        int SIDEBAR_WIDTH = SIDEBAR_WIDTH_BASE;
        int ITEM_HEIGHT = ITEM_HEIGHT_BASE;

        int panelX = SIDEBAR_WIDTH + 5;
        int panelY = 35;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 40;

        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF0F0F1A);

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
                ctx.fill(boxX, boxY, boxX + 50, boxY + 18, bs.get() ? 0xFF2A5A2A : 0xFF5A2A2A);
                ctx.drawTextWithShadow(textRenderer, Text.literal(bs.get() ? "ON" : "OFF"), boxX + 15, boxY + 5, bs.get() ? 0x55FF55 : 0xFF5555);
            } else if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) setting;
                int sliderX = panelX + panelWidth - 200;
                int sliderY = y + 2;
                int sliderWidth = 170;
                int sliderHeight = 14;

                ctx.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, 0xFF222233);
                int filledWidth = (int) (sliderWidth * ns.getPercentage());
                ctx.fill(sliderX, sliderY, sliderX + filledWidth, sliderY + sliderHeight, getAccentColor());
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
        int SIDEBAR_WIDTH = SIDEBAR_WIDTH_BASE;
        int ITEM_HEIGHT = ITEM_HEIGHT_BASE;

        int panelX = SIDEBAR_WIDTH + 5;
        int panelY = 60;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - 65;

        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF0F0F1A);

        int catY = 35;
        int catBtnWidth = 60;
        for (int i = 0; i < blockCategories.length; i++) {
            int x = panelX + 5 + i * (catBtnWidth + 3);
            boolean selected = (categoryFilter == i);
            boolean hovered = mouseX >= x && mouseX <= x + catBtnWidth && mouseY >= catY && mouseY <= catY + 18;

            ctx.fill(x, catY, x + catBtnWidth, catY + 18, selected ? 0xFF2A2A4A : (hovered ? 0xFF1F1F3A : 0xFF151525));
            if (selected) ctx.fill(x, catY + 17, x + catBtnWidth, catY + 18, getAccentColor());
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
            String blockId = Registries.BLOCK.getId(block).getPath();
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

            String shortId = blockId.length() > 25 ? blockId.substring(0, 22) + "..." : blockId;
            ctx.drawTextWithShadow(textRenderer, Text.literal(shortId), listX + 50, y + 13, 0x666666);
        }

        int selectedCount = BaseFinderClient.scanner != null ? BaseFinderClient.scanner.getSelectedBlocks().size() : 0;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Selected: " + selectedCount + " | Showing: " + filteredBlocks.size()), panelX + 10, panelY + panelHeight - 15, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Корректируем координаты мыши для логики кликов
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        String[] modes = {"Modules", "Settings", "Blocks"};
        for (int i = 0; i < modes.length; i++) {
            int y = 35 + i * 28;
            if (scaledMouseX >= 5 && scaledMouseX <= SIDEBAR_WIDTH_BASE - 5 && scaledMouseY >= y && scaledMouseY <= y + 24) {
                panelMode = i;
                return true;
            }
        }

        if (panelMode == 0) {
            Module.Category[] cats = Module.Category.values();
            int startY = 130;
            for (int i = 0; i < cats.length; i++) {
                int y = startY + i * 22;
                if (scaledMouseX >= 5 && scaledMouseX <= SIDEBAR_WIDTH_BASE - 5 && scaledMouseY >= y && scaledMouseY <= y + 20) {
                    selectedCategory = i;
                    selectedModule = 0;
                    moduleScrollOffset = 0;
                    return true;
                }
            }

            int listX = SIDEBAR_WIDTH_BASE + 5;
            int listY = 35 + 28;
            int listWidth = MODULE_LIST_WIDTH_BASE;

            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
                for (int i = 0; i < modules.size(); i++) {
                    int y = listY + i * ITEM_HEIGHT_BASE;
                    if (scaledMouseX >= listX && scaledMouseX <= listX + listWidth && scaledMouseY >= y && scaledMouseY <= y + ITEM_HEIGHT_BASE) {
                        if (button == 0) {
                            selectedModule = i;
                        } else if (button == 1) {
                            selectedModule = i;
                            modules.get(i).toggle();
                        }
                        return true;
                    }
                }
            }
        }

        if (panelMode == 1) {
            handleSettingsClick(scaledMouseX, scaledMouseY, button);
        }

        if (panelMode == 2) {
            handleBlockClick(scaledMouseX, scaledMouseY, button);
        }

        return true;
    }

    private void handleSettingsClick(int mouseX, int mouseY, int button) {
        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null || mm.baseFinder == null) return;

        BaseFinderModule bf = mm.baseFinder;
        int panelX = SIDEBAR_WIDTH_BASE + 5;
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
                    bs.toggle();
                    return;
                }
            } else if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) setting;
                int sliderX = panelX + panelWidth - 200;
                int sliderY = y + 2;
                int sliderWidth = 170;
                if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth && mouseY >= sliderY && mouseY <= sliderY + 14) {
                    double pct = (mouseX - sliderX) / sliderWidth;
                    ns.setFromPercentage(pct);
                    return;
                }
            } else if (setting instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) setting;
                int modeX = panelX + panelWidth - 120;
                if (mouseX >= modeX && mouseX <= modeX + 100 && mouseY >= y && mouseY <= y + 18) {
                    ms.cycle();
                    return;
                }
            }
        }
    }

    private void handleBlockClick(int mouseX, int mouseY, int button) {
        int panelX = SIDEBAR_WIDTH_BASE + 5;
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Изменение масштаба при зажатом Ctrl
        if (hasControlDown()) {
            if (verticalAmount > 0) {
                scale = Math.min(MAX_SCALE, scale + 0.1f);
            } else if (verticalAmount < 0) {
                scale = Math.max(MIN_SCALE, scale - 0.1f);
            }
            return true;
        }

        // Обычная прокрутка списков
        if (panelMode == 0) {
            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                Module.Category[] cats = Module.Category.values();
                List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
                int maxScroll = Math.max(0, modules.size() - (height - 80) / ITEM_HEIGHT_BASE);
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
        ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
        MinecraftClient.getInstance().setScreen(null);
    }

    private int getAccentColor() {
        float hue = (animationTime * 0.1f) % 1.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.7f, 0.9f);
    }
}
