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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClickGUI extends Screen {

    private ThemeManager theme;
    
    // Позиции панелей (теперь берутся из конфига)
    private int sidebarX, sidebarY;
    private int contentX, contentY;
    
    private int selectedCategory = 0;
    private int selectedModule = 0;
    private int panelMode = 0; // 0 = modules, 1 = settings, 2 = blocks
    private float animationTime = 0;
    private long lastFrameTime = 0;

    // Drag & Drop состояния
    private boolean draggingSidebar = false;
    private boolean draggingContent = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

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

    private final int SIDEBAR_WIDTH = 120;
    private final int MODULE_LIST_WIDTH = 200;
    private final int ITEM_HEIGHT = 22;
    private final int TOP_BAR_HEIGHT = 28;

    public ClickGUI() {
        super(Text.literal("freezdlc"));
        lastFrameTime = System.currentTimeMillis();
        
        // Инициализация темы и загрузка настроек
        this.theme = ThemeManager.getInstance();
        this.sidebarX = theme.sidebarX;
        this.sidebarY = theme.sidebarY;
        this.contentX = theme.contentX;
        this.contentY = theme.contentY;
        this.panelMode = theme.currentPanelMode;
        this.selectedCategory = theme.selectedCategory;

        allBlocks = new ArrayList<>();
        Registries.BLOCK.forEach(allBlocks::add);
        allBlocks.sort((a, b) -> Registries.BLOCK.getId(a).getPath().compareTo(Registries.BLOCK.getId(b).getPath()));
        filteredBlocks = new ArrayList<>(allBlocks);
    }

    @Override
    protected void init() {
        super.init();
        // Пересчитываем позицию поисковой строки относительно контентной панели
        int searchX = contentX + MODULE_LIST_WIDTH + 10; 
        // Ограничиваем, чтобы не уходила за экран
        if (searchX + 180 > width) searchX = width - 185;
        
        searchField = new TextFieldWidget(textRenderer, searchX, contentY + 5, 180, 18, Text.literal(""));
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

        // Темный фон на весь экран
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(0, 0, width, height, 0xCC0A0A0A);

        // Рендер верхней панели (привязана к верху экрана, но можно сделать перетаскиваемой)
        renderTopBar(ctx, mouseX, mouseY);

        // Рендер сайдбара (перетаскиваемый)
        renderSidebar(ctx, mouseX, mouseY);

        // Рендер контента (перетаскиваемый)
        switch (panelMode) {
            case 0: renderModuleList(ctx, mouseX, mouseY); break;
            case 1: renderSettingsPanel(ctx, mouseX, mouseY); break;
            case 2: renderBlockSelector(ctx, mouseX, mouseY); break;
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY) {
        // Топ бар всегда сверху
        ctx.fill(0, 0, width, TOP_BAR_HEIGHT, 0xFF1A1A2E);
        
        // Акцентная линия
        int accentColor = theme.rainbow ? getAccentColor() : theme.getColorWithAlpha(theme.hue, 0.7f, 0.9f);
        ctx.fill(0, TOP_BAR_HEIGHT - 2, width, TOP_BAR_HEIGHT, accentColor);

        ctx.drawTextWithShadow(textRenderer, Text.literal("freezdlc"), 10, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("v2.0"), 85, 10, 0x888888);

        String status = BaseFinderClient.scanner != null && BaseFinderClient.scanner.isRunning() ? "SCANNING" : "IDLE";
        int statusColor = status.equals("SCANNING") ? 0x55FF55 : 0xFF5555;
        ctx.drawTextWithShadow(textRenderer, Text.literal(status), width - 80, 10, statusColor);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Config: " + ConfigManager.getCurrentConfigName()), width - 200, 10, 0xAAAAAA);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        int x = sidebarX;
        int y = sidebarY;
        int w = SIDEBAR_WIDTH;
        int h = height - y;

        // Фон сайдбара
        int bgColor = new java.awt.Color(0, 0, 0, theme.alpha).getRGB();
        ctx.fill(x, y, x + w, y + h, bgColor);
        
        // Заголовок для перетаскивания
        boolean headerHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20;
        int headerColor = headerHovered ? new java.awt.Color(40, 40, 60, theme.alpha).getRGB() : new java.awt.Color(20, 20, 30, theme.alpha).getRGB();
        ctx.fill(x, y, x + w, y + 20, headerColor);
        
        if (draggingSidebar) {
             ctx.fill(x, y, x + w, y + 20, 0xFF3A3A5A); // Подсветка при перетаскивании
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("MENU"), x + 5, y + 6, 0xAAAAAA);

        // Кнопки режимов
        String[] modes = {"Modules", "Settings", "Blocks"};
        for (int i = 0; i < modes.length; i++) {
            int btnY = y + 25 + i * 28;
            boolean selected = (panelMode == i);
            boolean hovered = mouseX >= x + 5 && mouseX <= x + w - 5 && mouseY >= btnY && mouseY <= btnY + 24;

            int btnBg = selected ? new java.awt.Color(40, 40, 70, theme.alpha).getRGB() : (hovered ? new java.awt.Color(30, 30, 50, theme.alpha).getRGB() : 0x00000000);
            ctx.fill(x + 5, btnY, x + w - 5, btnY + 24, btnBg);

            if (selected) {
                int accentColor = theme.rainbow ? getAccentColor() : theme.getColorWithAlpha(theme.hue, 0.7f, 0.9f);
                ctx.fill(x + 5, btnY, x + 8, btnY + 24, accentColor);
            }

            String label = selected ? "> " + modes[i] : "  " + modes[i];
            ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 15, btnY + 8, selected ? 0xFFFFFF : 0x888888);
        }

        // Категории (только в режиме Modules)
        if (panelMode == 0) {
            int startY = y + 130;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Categories"), x + 10, startY - 15, 0x666666);

            Module.Category[] cats = Module.Category.values();
            for (int i = 0; i < cats.length; i++) {
                int catY = startY + i * 22;
                boolean selected = (selectedCategory == i);
                boolean hovered = mouseX >= x + 5 && mouseX <= x + w - 5 && mouseY >= catY && mouseY <= catY + 20;

                if (selected) {
                    ctx.fill(x + 5, catY, x + w - 5, catY + 20, new java.awt.Color(40, 40, 70, theme.alpha).getRGB());
                    int catColor = theme.rainbow ? getAccentColor() : cats[i].color;
                    ctx.fill(x + 5, catY, x + 8, catY + 20, catColor);
                } else if (hovered) {
                    ctx.fill(x + 5, catY, x + w - 5, catY + 20, new java.awt.Color(30, 30, 50, theme.alpha).getRGB());
                }

                String label = selected ? "> " + cats[i].displayName : "  " + cats[i].displayName;
                ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 15, catY + 6, selected ? 0xFFFFFF : 0x888888);
                
                // Count
                ModuleManager mm = BaseFinderClient.moduleManager;
                if (mm != null) {
                    int count = mm.getModulesByCategory(cats[i]).size();
                    ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(count)), x + w - 25, catY + 6, 0x555555);
                }
            }
        }
    }

    private void renderModuleList(DrawContext ctx, int mouseX, int mouseY) {
        int x = contentX;
        int y = contentY;
        int w = MODULE_LIST_WIDTH;
        int h = height - y - 5;

        // Фон списка
        int bgColor = new java.awt.Color(5, 5, 15, theme.alpha).getRGB();
        ctx.fill(x, y, x + w, y + h, bgColor);
        
        // Заголовок для перетаскивания
        boolean headerHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20;
        if (headerHovered || draggingContent) {
             ctx.fill(x, y, x + w, y + 20, new java.awt.Color(30, 30, 50, theme.alpha).getRGB());
        }
        
        Module.Category[] cats = Module.Category.values();
        if (selectedCategory < cats.length) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(cats[selectedCategory].displayName + " Modules"), x + 10, y + 8, 0xFFFFFF);
        }

        // Список модулей
        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null) return;

        List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
        int startY = y + 28;
        int visibleItems = (h - 35) / ITEM_HEIGHT;

        for (int i = 0; i < visibleItems && (i + moduleScrollOffset) < modules.size(); i++) {
            int idx = i + moduleScrollOffset;
            Module mod = modules.get(idx);
            int itemY = startY + i * ITEM_HEIGHT;

            boolean selected = (selectedModule == idx);
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;

            if (selected) {
                ctx.fill(x + 1, itemY, x + w - 1, itemY + ITEM_HEIGHT, new java.awt.Color(40, 40, 70, theme.alpha).getRGB());
                int accentColor = theme.rainbow ? getAccentColor() : theme.getColorWithAlpha(theme.hue, 0.7f, 0.9f);
                ctx.fill(x + 1, itemY, x + 4, itemY + ITEM_HEIGHT, accentColor);
            } else if (hovered) {
                ctx.fill(x + 1, itemY, x + w - 1, itemY + ITEM_HEIGHT, new java.awt.Color(20, 20, 40, theme.alpha).getRGB());
            }

            int nameColor = mod.isEnabled() ? 0x55FF55 : (selected ? 0xFFFFFF : 0xAAAAAA);
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), x + 12, itemY + 7, nameColor);

            String toggle = mod.isEnabled() ? "[ON]" : "[OFF]";
            int toggleColor = mod.isEnabled() ? 0x55FF55 : 0xFF5555;
            ctx.drawTextWithShadow(textRenderer, Text.literal(toggle), x + w - 40, itemY + 7, toggleColor);
        }
        
        renderInfoPanel(ctx, mouseX, mouseY, modules, x + w + 10);
    }

    private void renderInfoPanel(DrawContext ctx, int mouseX, int mouseY, List<Module> modules, int panelX) {
        int panelY = contentY;
        int panelWidth = width - panelX - 5;
        int panelHeight = height - panelY - 5;

        int bgColor = new java.awt.Color(5, 5, 15, theme.alpha).getRGB();
        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, bgColor);

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
        
        // ... (остальной код инфо панели можно оставить как был, сокращено для кратости)
    }

    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY) {
        int x = contentX;
        int y = contentY;
        int w = width - x - 5;
        int h = height - y - 5;

        int bgColor = new java.awt.Color(5, 5, 15, theme.alpha).getRGB();
        ctx.fill(x, y, x + w, y + h, bgColor);

        // Заголовок для перетаскивания
        boolean headerHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20;
        if (headerHovered || draggingContent) {
             ctx.fill(x, y, x + w, y + 20, new java.awt.Color(30, 30, 50, theme.alpha).getRGB());
        }
        ctx.drawTextWithShadow(textRenderer, Text.literal("Settings"), x + 10, y + 8, 0xFFFFFF);

        // Здесь должна быть логика рендера настроек (слайдеры и т.д.)
        // Для краткости опущено, но логика та же, что и в прошлом коде, только с использованием theme.alpha
    }

    private void renderBlockSelector(DrawContext ctx, int mouseX, int mouseY) {
        int x = contentX;
        int y = contentY;
        int w = width - x - 5;
        int h = height - y - 5;

        int bgColor = new java.awt.Color(5, 5, 15, theme.alpha).getRGB();
        ctx.fill(x, y, x + w, y + h, bgColor);
        
        // Заголовок для перетаскивания
        boolean headerHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 20;
        if (headerHovered || draggingContent) {
             ctx.fill(x, y, x + w, y + 20, new java.awt.Color(30, 30, 50, theme.alpha).getRGB());
        }
        ctx.drawTextWithShadow(textRenderer, Text.literal("Block Selector"), x + 10, y + 8, 0xFFFFFF);

        // Логика поиска и списка блоков (аналогично предыдущим версиям)
        // ...
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Логика перетаскивания САЙДБАРА
        if (mouseY >= sidebarY && mouseY <= sidebarY + 20 && mouseX >= sidebarX && mouseX <= sidebarX + SIDEBAR_WIDTH) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                draggingSidebar = true;
                dragOffsetX = (int)(mouseX - sidebarX);
                dragOffsetY = (int)(mouseY - sidebarY);
                return true;
            }
            // ПКМ для смены цвета (пример)
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                theme.rainbow = !theme.rainbow;
                theme.saveConfig();
                return true;
            }
        }

        // Логика перетаскивания КОНТЕНТА
        int contentHeaderY = contentY;
        int contentHeaderH = 20;
        int contentW = (panelMode == 0) ? MODULE_LIST_WIDTH + 300 : width - contentX - 5; // Примерная ширина

        if (mouseY >= contentY && mouseY <= contentY + contentHeaderH && mouseX >= contentX && mouseX <= contentX + contentW) {
             if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                draggingContent = true;
                dragOffsetX = (int)(mouseX - contentX);
                dragOffsetY = (int)(mouseY - contentY);
                return true;
            }
        }

        // Остальные клики (кнопки, модули)
        // ... (здесь должна быть логика кликов по кнопкам меню, модулям и т.д., как в оригинале)
        // Важно: проверять клики только если мы не тащим панель
        
        if (!draggingSidebar && !draggingContent) {
            // Клик по кнопкам режимов
            String[] modes = {"Modules", "Settings", "Blocks"};
            for (int i = 0; i < modes.length; i++) {
                int btnY = sidebarY + 25 + i * 28;
                if (mouseX >= sidebarX + 5 && mouseX <= sidebarX + SIDEBAR_WIDTH - 5 && mouseY >= btnY && mouseY <= btnY + 24) {
                    panelMode = i;
                    theme.currentPanelMode = i;
                    theme.saveConfig();
                    return true;
                }
            }
            
            // Клик по категориям и модулям (упрощено)
            if (panelMode == 0) {
                 // Логика выбора категории и модуля...
                 // При выборе категории:
                 // selectedCategory = i;
                 // theme.selectedCategory = i;
                 // theme.saveConfig();
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingSidebar = false;
            draggingContent = false;
            // Сохраняем позиции при отпускании
            theme.sidebarX = sidebarX;
            theme.sidebarY = sidebarY;
            theme.contentX = contentX;
            theme.contentY = contentY;
            theme.saveConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSidebar) {
            sidebarX = (int)(mouseX - dragOffsetX);
            sidebarY = (int)(mouseY - dragOffsetY);
            return true;
        }
        if (draggingContent) {
            contentX = (int)(mouseX - dragOffsetX);
            contentY = (int)(mouseY - dragOffsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        // Финальное сохранение
        theme.sidebarX = sidebarX;
        theme.sidebarY = sidebarY;
        theme.contentX = contentX;
        theme.contentY = contentY;
        theme.currentPanelMode = panelMode;
        theme.selectedCategory = selectedCategory;
        theme.saveConfig();
        
        ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
        MinecraftClient.getInstance().setScreen(null);
    }

    private int getAccentColor() {
        float hue = (animationTime * 0.1f) % 1.0f;
        return java.awt.Color.HSBtoRGB(hue, 0.7f, 0.9f);
    }
}
