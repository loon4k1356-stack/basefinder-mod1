package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import com.basefinder.config.ConfigManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockSelectScreen extends Screen {
    private TextFieldWidget searchField;
    private List<Block> allBlocks;
    private List<Block> filteredBlocks;
    private List<Block> selectedBlocks;
    private int scrollOffset = 0;
    private final int itemHeight = 20;
    private final int visibleItems = 15;
    
    // Tab system
    private int currentTab = 0; // 0 = Blocks, 1 = Configs, 2 = Settings
    private final String[] tabs = {"Блоки", "Конфиги", "Настройки"};
    
    // Category filter
    private int currentCategory = 0;
    private final String[] categories = {"Все", "Руды", "Хранилища", "Редстоун", "Декор", "Редкие"};
    
    // Config management
    private TextFieldWidget configNameField;
    private int configScrollOffset = 0;

    public BlockSelectScreen() {
        super(Text.literal("BaseFinder"));
        allBlocks = new ArrayList<>();
        Registries.BLOCK.forEach(allBlocks::add);
        allBlocks.sort((a, b) -> {
            String nameA = Registries.BLOCK.getId(a).getPath();
            String nameB = Registries.BLOCK.getId(b).getPath();
            return nameA.compareTo(nameB);
        });
        filteredBlocks = new ArrayList<>(allBlocks);
        selectedBlocks = new ArrayList<>(BaseFinderClient.scanner.getSelectedBlocks());
    }

    @Override
    protected void init() {
        super.init();
        
        // Search field (only for blocks tab)
        searchField = new TextFieldWidget(textRenderer, width / 2 - 100, 50, 200, 20, Text.literal("Поиск..."));
        searchField.setMaxLength(50);
        searchField.setChangedListener(this::onSearchChanged);
        addDrawableChild(searchField);
        
        // Config name field (only for configs tab)
        configNameField = new TextFieldWidget(textRenderer, width / 2 - 100, 50, 200, 20, Text.literal("Имя конфига"));
        configNameField.setMaxLength(30);
        addDrawableChild(configNameField);

        // Start/Stop button
        addDrawableChild(ButtonWidget.builder(
                Text.literal(BaseFinderClient.scanner.isRunning() ? "§cОстановить" : "§aЗапустить"),
                button -> { BaseFinderClient.toggleScanner(); close(); }
        ).dimensions(width / 2 - 100, height - 60, 95, 20).build());

        // Clear selection button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§eОчистить"),
                button -> { BaseFinderClient.scanner.clearSelectedBlocks(); selectedBlocks.clear(); }
        ).dimensions(width / 2 + 5, height - 60, 95, 20).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§7Закрыть"),
                button -> close()
        ).dimensions(width / 2 - 50, height - 35, 100, 20).build());
    }

    private void onSearchChanged(String query) {
        applyFilters();
    }
    
    private void applyFilters() {
        String query = searchField != null ? searchField.getText().toLowerCase() : "";
        
        filteredBlocks = allBlocks.stream()
                .filter(block -> {
                    String blockId = Registries.BLOCK.getId(block).getPath().toLowerCase();
                    String blockName = block.getName().getString().toLowerCase();
                    
                    // Search filter
                    boolean matchesSearch = query.isEmpty() || 
                            blockId.contains(query) || blockName.contains(query);
                    
                    // Category filter
                    boolean matchesCategory = true;
                    switch (currentCategory) {
                        case 1: // Ores
                            matchesCategory = blockId.contains("ore") || blockId.contains("ancient_debris");
                            break;
                        case 2: // Storage
                            matchesCategory = blockId.contains("chest") || blockId.contains("barrel") || 
                                            blockId.contains("shulker") || blockId.contains("hopper");
                            break;
                        case 3: // Redstone
                            matchesCategory = blockId.contains("redstone") || blockId.contains("piston") || 
                                            blockId.contains("observer") || blockId.contains("repeater") ||
                                            blockId.contains("comparator") || blockId.contains("dispenser") ||
                                            blockId.contains("dropper") || blockId.contains("target");
                            break;
                        case 4: // Decor
                            matchesCategory = blockId.contains("lantern") || blockId.contains("candle") || 
                                            blockId.contains("banner") || blockId.contains("concrete") ||
                                            blockId.contains("terracotta") || blockId.contains("glazed");
                            break;
                        case 5: // Rare
                            matchesCategory = blockId.contains("diamond") || blockId.contains("emerald") || 
                                            blockId.contains("gold") || blockId.contains("netherite") ||
                                            blockId.contains("beacon") || blockId.contains("spawner") ||
                                            blockId.contains("ender_chest") || blockId.contains("dragon");
                            break;
                    }
                    
                    return matchesSearch && matchesCategory;
                })
                .collect(Collectors.toList());
        
        scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§lBaseFinder"), width / 2, 5, 0xFFFFFF);
        
        // Config name display
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Конфиг: §e" + ConfigManager.getCurrentConfigName()),
                width / 2, 15, 0xAAAAAA);
        
        // Tabs
        renderTabs(context, mouseX, mouseY);
        
        // Render current tab content
        switch (currentTab) {
            case 0:
                renderBlocksTab(context, mouseX, mouseY);
                break;
            case 1:
                renderConfigsTab(context, mouseX, mouseY);
                break;
            case 2:
                renderSettingsTab(context, mouseX, mouseY);
                break;
        }

        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        int tabWidth = 80;
        int tabHeight = 20;
        int startX = width / 2 - (tabs.length * tabWidth) / 2;
        int y = 25;
        
        for (int i = 0; i < tabs.length; i++) {
            int x = startX + i * tabWidth;
            boolean selected = (i == currentTab);
            boolean hovered = mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight;
            
            // Tab background
            int bgColor = selected ? 0xFF555555 : (hovered ? 0xFF444444 : 0xFF333333);
            context.fill(x, y, x + tabWidth, y + tabHeight, bgColor);
            
            // Tab border
            int borderColor = selected ? 0xFFFFAA00 : 0xFF666666;
            context.fill(x, y, x + tabWidth, y + 1, borderColor);
            context.fill(x, y + tabHeight - 1, x + tabWidth, y + tabHeight, borderColor);
            context.fill(x, y, x + 1, y + tabHeight, borderColor);
            context.fill(x + tabWidth - 1, y, x + tabWidth, y + tabHeight, borderColor);
            
            // Tab text
            String tabText = selected ? "§f" + tabs[i] : "§7" + tabs[i];
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(tabText), 
                    x + tabWidth / 2, y + 6, 0xFFFFFF);
        }
    }
    
    private void renderBlocksTab(DrawContext context, int mouseX, int mouseY) {
        // Selected blocks count
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§aВыбрано: " + selectedBlocks.size() + " блоков"),
                width / 2, 75, 0x55FF55);
        
        // Categories
        renderCategories(context, mouseX, mouseY);
        
        // Block list background
        int listX = width / 2 - 150;
        int listY = 100;
        int listWidth = 300;
        int listHeight = visibleItems * itemHeight;
        
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);
        
        // Border
        context.fill(listX, listY, listX + listWidth, listY + 1, 0xFF666666);
        context.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, 0xFF666666);
        context.fill(listX, listY, listX + 1, listY + listHeight, 0xFF666666);
        context.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, 0xFF666666);

        // Render visible blocks
        for (int i = 0; i < visibleItems && (i + scrollOffset) < filteredBlocks.size(); i++) {
            int index = i + scrollOffset;
            Block block = filteredBlocks.get(index);
            String blockId = Registries.BLOCK.getId(block).getPath();
            String blockName = block.getName().getString();
            
            int y = listY + i * itemHeight;
            
            // Hover effect
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth &&
                            mouseY >= y && mouseY <= y + itemHeight;
            
            if (hovered) {
                context.fill(listX + 1, y, listX + listWidth - 1, y + itemHeight, 0x40FFFFFF);
            }
            
            // Selection indicator
            boolean isSelected = selectedBlocks.contains(block);
            
            if (isSelected) {
                context.fill(listX + 1, y, listX + 25, y + itemHeight, 0x4055FF55);
                context.drawTextWithShadow(textRenderer, Text.literal("§a✓"), listX + 5, y + 5, 0x55FF55);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal("§7○"), listX + 5, y + 5, 0xAAAAAA);
            }
            
            // Block name
            String displayText = blockName + " §7(" + blockId + ")";
            context.drawTextWithShadow(textRenderer, Text.literal(displayText), 
                    listX + 30, y + 5, isSelected ? 0x55FF55 : 0xFFFFFF);
        }

        // Scrollbar
        if (filteredBlocks.size() > visibleItems) {
            int scrollbarHeight = listHeight;
            int thumbHeight = Math.max(10, scrollbarHeight * visibleItems / filteredBlocks.size());
            int thumbY = listY + (scrollbarHeight - thumbHeight) * scrollOffset / 
                        Math.max(1, filteredBlocks.size() - visibleItems);
            
            context.fill(listX + listWidth - 5, listY, listX + listWidth, listY + scrollbarHeight, 0x40FFFFFF);
            context.fill(listX + listWidth - 5, thumbY, listX + listWidth, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }
    
    private void renderCategories(DrawContext context, int mouseX, int mouseY) {
        int catWidth = 50;
        int catHeight = 16;
        int startX = width / 2 - (categories.length * catWidth) / 2;
        int y = 82;
        
        for (int i = 0; i < categories.length; i++) {
            int x = startX + i * catWidth;
            boolean selected = (i == currentCategory);
            boolean hovered = mouseX >= x && mouseX <= x + catWidth && mouseY >= y && mouseY <= y + catHeight;
            
            int bgColor = selected ? 0xFF555555 : (hovered ? 0xFF444444 : 0xFF333333);
            context.fill(x, y, x + catWidth, y + catHeight, bgColor);
            
            String catText = selected ? "§f" + categories[i] : "§7" + categories[i];
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(catText), 
                    x + catWidth / 2, y + 4, 0xFFFFFF);
        }
    }
    
    private void renderConfigsTab(DrawContext context, int mouseX, int mouseY) {
        // Instructions
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Управление конфигами"), width / 2, 75, 0xAAAAAA);
        
        // Config list
        List<String> configs = ConfigManager.listConfigs();
        String currentName = ConfigManager.getCurrentConfigName();
        
        int listX = width / 2 - 150;
        int listY = 100;
        int listWidth = 300;
        int listHeight = visibleItems * itemHeight;
        
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);
        context.fill(listX, listY, listX + listWidth, listY + 1, 0xFF666666);
        context.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, 0xFF666666);
        context.fill(listX, listY, listX + 1, listY + listHeight, 0xFF666666);
        context.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, 0xFF666666);
        
        // Render configs
        for (int i = 0; i < visibleItems && (i + configScrollOffset) < configs.size(); i++) {
            int index = i + configScrollOffset;
            String configName = configs.get(index);
            
            int y = listY + i * itemHeight;
            
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth &&
                            mouseY >= y && mouseY <= y + itemHeight;
            
            if (hovered) {
                context.fill(listX + 1, y, listX + listWidth - 1, y + itemHeight, 0x40FFFFFF);
            }
            
            boolean isCurrent = configName.equals(currentName);
            
            if (isCurrent) {
                context.fill(listX + 1, y, listX + listWidth - 1, y + itemHeight, 0x40FFAA00);
                context.drawTextWithShadow(textRenderer, Text.literal("§a▶ " + configName + " §7(текущий)"), 
                        listX + 10, y + 5, 0xFFAA00);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal("§f  " + configName), 
                        listX + 10, y + 5, 0xFFFFFF);
            }
        }
        
        if (configs.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§7Нет сохранённых конфигов"), width / 2, listY + 50, 0xAAAAAA);
        }
        
        // Buttons
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Используй команды: §f/bf cfg save/load <имя>"),
                width / 2, listY + listHeight + 10, 0xAAAAAA);
    }
    
    private void renderSettingsTab(DrawContext context, int mouseX, int mouseY) {
        int centerX = width / 2;
        int startY = 80;
        int lineSpacing = 25;
        
        // Scanner settings
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§lНастройки сканера"), centerX, startY, 0xFFAA00);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§fРадиус: §e" + BaseFinderClient.scanner.getScanRadius() + " блоков"),
                centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        String mode = BaseFinderClient.scanner.isLiteMode() ? "§aLITE (Y < " + 
                BaseFinderClient.scanner.getLiteHeightLimit() + ")" : "§bFULL";
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§fРежим: " + mode), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§fВыбрано блоков: §e" + BaseFinderClient.scanner.getSelectedBlocks().size()),
                centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing * 2;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§lУправление"), centerX, startY, 0xFFAA00);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f[O] §7- Открыть это меню"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f[H] §7- Запустить/Остановить сканер"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing * 2;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§lКоманды"), centerX, startY, 0xFFAA00);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf start §7- Запустить поиск"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf mode lite §7- Режим Y < 30"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf cfg save <имя> §7- Сохранить конфиг"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf cfg load <имя> §7- Загрузить конфиг"), centerX, startY, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Tab clicks
        int tabWidth = 80;
        int tabHeight = 20;
        int startX = width / 2 - (tabs.length * tabWidth) / 2;
        int y = 25;
        
        for (int i = 0; i < tabs.length; i++) {
            int x = startX + i * tabWidth;
            if (mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight) {
                currentTab = i;
                return true;
            }
        }
        
        // Category clicks (only in blocks tab)
        if (currentTab == 0) {
            int catWidth = 50;
            int catHeight = 16;
            int catStartX = width / 2 - (categories.length * catWidth) / 2;
            int catY = 82;
            
            for (int i = 0; i < categories.length; i++) {
                int x = catStartX + i * catWidth;
                if (mouseX >= x && mouseX <= x + catWidth && mouseY >= catY && mouseY <= catY + catHeight) {
                    currentCategory = i;
                    applyFilters();
                    return true;
                }
            }
        }

        // Block list clicks (only in blocks tab)
        if (currentTab == 0) {
            int listX = width / 2 - 150;
            int listY = 100;
            int listWidth = 300;
            int listHeight = visibleItems * itemHeight;

            if (mouseX >= listX && mouseX <= listX + listWidth &&
                mouseY >= listY && mouseY <= listY + listHeight) {
                
                int relativeY = (int) (mouseY - listY);
                int index = relativeY / itemHeight + scrollOffset;
                
                if (index >= 0 && index < filteredBlocks.size()) {
                    Block block = filteredBlocks.get(index);
                    
                    if (selectedBlocks.contains(block)) {
                        selectedBlocks.remove(block);
                        BaseFinderClient.scanner.removeSelectedBlock(block);
                    } else {
                        selectedBlocks.add(block);
                        BaseFinderClient.scanner.addSelectedBlock(block);
                    }
                    
                    return true;
                }
            }
        }
        
        // Config list clicks (only in configs tab)
        if (currentTab == 1) {
            List<String> configs = ConfigManager.listConfigs();
            int listX = width / 2 - 150;
            int listY = 100;
            int listWidth = 300;
            int listHeight = visibleItems * itemHeight;
            
            if (mouseX >= listX && mouseX <= listX + listWidth &&
                mouseY >= listY && mouseY <= listY + listHeight) {
                
                int relativeY = (int) (mouseY - listY);
                int index = relativeY / itemHeight + configScrollOffset;
                
                if (index >= 0 && index < configs.size()) {
                    String configName = configs.get(index);
                    
                    if (button == 0) { // Left click - load
                        if (ConfigManager.loadConfig(configName)) {
                            MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("§a[BaseFinder] Конфиг '" + configName + "' загружен!"), false);
                        }
                    } else if (button == 1) { // Right click - save current
                        if (ConfigManager.saveConfig(configName)) {
                            ConfigManager.setCurrentConfigName(configName);
                            MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("§a[BaseFinder] Конфиг '" + configName + "' сохранён!"), false);
                        }
                    }
                    
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == 0) {
            int maxScroll = Math.max(0, filteredBlocks.size() - visibleItems);
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount));
        } else if (currentTab == 1) {
            List<String> configs = ConfigManager.listConfigs();
            int maxScroll = Math.max(0, configs.size() - visibleItems);
            configScrollOffset = (int) Math.max(0, Math.min(maxScroll, configScrollOffset - verticalAmount));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
        MinecraftClient.getInstance().setScreen(null);
    }
}
