package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
import com.basefinder.config.ConfigManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
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
    private float scrollAnimation = 0;
    private final int itemHeight = 24;
    private final int visibleItems = 12;
    
    // Tab system
    private int currentTab = 0;
    private float tabAnimation = 0;
    private final String[] tabs = {"⛏ Блоки", "💾 Конфиги", "⚙ Настройки"};
    
    // Category filter
    private int currentCategory = 0;
    private final String[] categories = {"Все", "💎 Руды", "📦 Хранилища", "⚡ Редстоун", "🎨 Декор", "⭐ Редкие"};
    
    // Config management
    private int configScrollOffset = 0;
    
    // Animation
    private float animationTime = 0;
    private long lastFrameTime = 0;

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
        lastFrameTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        
        // Search field
        searchField = new TextFieldWidget(textRenderer, width / 2 - 100, 70, 200, 20, Text.literal(""));
        searchField.setMaxLength(50);
        searchField.setChangedListener(this::onSearchChanged);
        addDrawableChild(searchField);

        // Start/Stop button
        addDrawableChild(ButtonWidget.builder(
                Text.literal(BaseFinderClient.scanner.isRunning() ? "⏹ Стоп" : "▶ Старт"),
                button -> { BaseFinderClient.toggleScanner(); close(); }
        ).dimensions(width / 2 - 100, height - 50, 95, 20).build());

        // Clear button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("🗑 Очистить"),
                button -> { BaseFinderClient.scanner.clearSelectedBlocks(); selectedBlocks.clear(); }
        ).dimensions(width / 2 + 5, height - 50, 95, 20).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(
                Text.literal("✕ Закрыть"),
                button -> close()
        ).dimensions(width / 2 - 50, height - 25, 100, 20).build());
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
                    
                    boolean matchesSearch = query.isEmpty() || 
                            blockId.contains(query) || blockName.contains(query);
                    
                    boolean matchesCategory = true;
                    switch (currentCategory) {
                        case 1:
                            matchesCategory = blockId.contains("ore") || blockId.contains("ancient_debris");
                            break;
                        case 2:
                            matchesCategory = blockId.contains("chest") || blockId.contains("barrel") || 
                                            blockId.contains("shulker") || blockId.contains("hopper");
                            break;
                        case 3:
                            matchesCategory = blockId.contains("redstone") || blockId.contains("piston") || 
                                            blockId.contains("observer") || blockId.contains("repeater") ||
                                            blockId.contains("comparator") || blockId.contains("dispenser") ||
                                            blockId.contains("dropper") || blockId.contains("target");
                            break;
                        case 4:
                            matchesCategory = blockId.contains("lantern") || blockId.contains("candle") || 
                                            blockId.contains("banner") || blockId.contains("concrete") ||
                                            blockId.contains("terracotta") || blockId.contains("glazed");
                            break;
                        case 5:
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
        // Update animation
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        lastFrameTime = currentTime;
        animationTime += deltaTime;
        
        // Background with gradient
        renderBackground(context, mouseX, mouseY, delta);
        
        // Animated background overlay
        int bgAlpha = (int)(Math.sin(animationTime * 2) * 10 + 20);
        context.fill(0, 0, width, height, 0x80000000 | (bgAlpha << 24));
        
        // Title with glow effect
        renderTitle(context);
        
        // Tabs
        renderTabs(context, mouseX, mouseY, delta);
        
        // Content based on tab
        switch (currentTab) {
            case 0:
                renderBlocksTab(context, mouseX, mouseY, delta);
                break;
            case 1:
                renderConfigsTab(context, mouseX, mouseY, delta);
                break;
            case 2:
                renderSettingsTab(context, mouseX, mouseY, delta);
                break;
        }

        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderTitle(DrawContext context) {
        // Glow effect
        int glowAlpha = (int)(Math.sin(animationTime * 3) * 30 + 50);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§l⚡ BaseFinder ⚡"), width / 2, 8, 0xFFFFFF | (glowAlpha << 24));
        
        // Config name
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7📁 " + ConfigManager.getCurrentConfigName()),
                width / 2, 22, 0xAAAAAA);
        
        // Stats bar
        String stats = String.format("§fРадиус: §e%d §7| §fРежим: §e%s §7| §fБлоков: §e%d",
                BaseFinderClient.scanner.getScanRadius(),
                BaseFinderClient.scanner.isLiteMode() ? "LITE" : "FULL",
                selectedBlocks.size());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(stats), width / 2, 36, 0xFFFFFF);
    }
    
    private void renderTabs(DrawContext context, int mouseX, int mouseY, float delta) {
        int tabWidth = 100;
        int tabHeight = 24;
        int startX = width / 2 - (tabs.length * tabWidth) / 2;
        int y = 48;
        
        for (int i = 0; i < tabs.length; i++) {
            int x = startX + i * tabWidth;
            boolean selected = (i == currentTab);
            boolean hovered = mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight;
            
            // Animated background
            int bgColor;
            if (selected) {
                int pulse = (int)(Math.sin(animationTime * 4 + i) * 20 + 80);
                bgColor = 0xFF000000 | (pulse << 16) | (pulse << 8) | pulse;
            } else if (hovered) {
                bgColor = 0xFF444444;
            } else {
                bgColor = 0xFF2A2A2A;
            }
            
            // Rounded corners effect
            context.fill(x + 2, y, x + tabWidth - 2, y + tabHeight, bgColor);
            context.fill(x, y + 2, x + tabWidth, y + tabHeight - 2, bgColor);
            
            // Border with glow
            int borderColor = selected ? 0xFFFFAA00 : (hovered ? 0xFF888888 : 0xFF555555);
            context.fill(x + 2, y, x + tabWidth - 2, y + 1, borderColor);
            context.fill(x + 2, y + tabHeight - 1, x + tabWidth - 2, y + tabHeight, borderColor);
            context.fill(x, y + 2, x + 1, y + tabHeight - 2, borderColor);
            context.fill(x + tabWidth - 1, y + 2, x + tabWidth, y + tabHeight - 2, borderColor);
            
            // Text
            String tabText = selected ? "§f§l" + tabs[i] : "§7" + tabs[i];
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(tabText), 
                    x + tabWidth / 2, y + 8, 0xFFFFFF);
        }
    }
    
    private void renderBlocksTab(DrawContext context, int mouseX, int mouseY, float delta) {
        // Categories
        renderCategories(context, mouseX, mouseY);
        
        // Block list with fancy background
        int listX = width / 2 - 160;
        int listY = 110;
        int listWidth = 320;
        int listHeight = visibleItems * itemHeight;
        
        // Gradient background
        for (int i = 0; i < listHeight; i++) {
            int alpha = 180 - (i * 50 / listHeight);
            context.fill(listX, listY + i, listX + listWidth, listY + i + 1, 0x000000 | (alpha << 24));
        }
        
        // Animated border
        int borderPulse = (int)(Math.sin(animationTime * 2) * 30 + 100);
        int borderColor = 0xFF000000 | (borderPulse << 16) | (borderPulse << 8) | borderPulse;
        context.fill(listX, listY, listX + listWidth, listY + 1, borderColor);
        context.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, borderColor);
        context.fill(listX, listY, listX + 1, listY + listHeight, borderColor);
        context.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, borderColor);

        // Render blocks
        for (int i = 0; i < visibleItems && (i + scrollOffset) < filteredBlocks.size(); i++) {
            int index = i + scrollOffset;
            Block block = filteredBlocks.get(index);
            String blockId = Registries.BLOCK.getId(block).getPath();
            String blockName = block.getName().getString();
            
            int y = listY + i * itemHeight;
            
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth &&
                            mouseY >= y && mouseY <= y + itemHeight;
            
            boolean isSelected = selectedBlocks.contains(block);
            
            // Row background
            if (isSelected) {
                int greenPulse = (int)(Math.sin(animationTime * 3 + i * 0.5) * 20 + 40);
                context.fill(listX + 1, y + 1, listX + listWidth - 1, y + itemHeight - 1, 
                        0x000000 | (greenPulse << 8));
            } else if (hovered) {
                context.fill(listX + 1, y + 1, listX + listWidth - 1, y + itemHeight - 1, 0x40FFFFFF);
            }
            
            // Selection indicator with animation
            if (isSelected) {
                context.drawTextWithShadow(textRenderer, Text.literal("§a✓"), listX + 8, y + 7, 0x55FF55);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal("§8○"), listX + 8, y + 7, 0x888888);
            }
            
            // Block icon
            ItemStack stack = new ItemStack(block);
            context.drawItem(stack, listX + 25, y + 3);
            
            // Block name
            String displayName = blockName;
            if (displayName.length() > 25) {
                displayName = displayName.substring(0, 22) + "...";
            }
            
            int textColor = isSelected ? 0x55FF55 : (hovered ? 0xFFFFFF : 0xCCCCCC);
            context.drawTextWithShadow(textRenderer, Text.literal(displayName), 
                    listX + 48, y + 7, textColor);
            
            // Block ID (smaller)
            String shortId = blockId;
            if (shortId.length() > 20) {
                shortId = shortId.substring(0, 17) + "...";
            }
            context.drawTextWithShadow(textRenderer, Text.literal("§7" + shortId), 
                    listX + 48, y + 16, 0x888888);
        }

        // Scrollbar with animation
        if (filteredBlocks.size() > visibleItems) {
            int scrollbarHeight = listHeight;
            int thumbHeight = Math.max(20, scrollbarHeight * visibleItems / filteredBlocks.size());
            int thumbY = listY + (scrollbarHeight - thumbHeight) * scrollOffset / 
                        Math.max(1, filteredBlocks.size() - visibleItems);
            
            // Scrollbar background
            context.fill(listX + listWidth - 8, listY, listX + listWidth - 2, listY + scrollbarHeight, 0x40FFFFFF);
            
            // Scrollbar thumb with gradient
            for (int i = 0; i < thumbHeight; i++) {
                int alpha = 200 - (i * 100 / thumbHeight);
                context.fill(listX + listWidth - 8, thumbY + i, listX + listWidth - 2, thumbY + i + 1, 
                        0xFFFFFF | (alpha << 24));
            }
        }
        
        // Counter
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(String.format("§a%d §7/ §f%d блоков", selectedBlocks.size(), filteredBlocks.size())),
                width / 2, listY + listHeight + 5, 0xFFFFFF);
    }
    
    private void renderCategories(DrawContext context, int mouseX, int mouseY) {
        int catWidth = 55;
        int catHeight = 18;
        int startX = width / 2 - (categories.length * catWidth) / 2;
        int y = 90;
        
        for (int i = 0; i < categories.length; i++) {
            int x = startX + i * catWidth;
            boolean selected = (i == currentCategory);
            boolean hovered = mouseX >= x && mouseX <= x + catWidth && mouseY >= y && mouseY <= y + catHeight;
            
            // Animated background
            int bgColor;
            if (selected) {
                int pulse = (int)(Math.sin(animationTime * 4 + i) * 30 + 100);
                bgColor = 0xFF000000 | (pulse << 16) | (pulse << 8) | pulse;
            } else if (hovered) {
                bgColor = 0xFF3A3A3A;
            } else {
                bgColor = 0xFF252525;
            }
            
            context.fill(x + 1, y, x + catWidth - 1, y + catHeight, bgColor);
            
            // Border
            int borderColor = selected ? 0xFFFFAA00 : (hovered ? 0xFF666666 : 0xFF444444);
            context.fill(x + 1, y, x + catWidth - 1, y + 1, borderColor);
            context.fill(x + 1, y + catHeight - 1, x + catWidth - 1, y + catHeight, borderColor);
            context.fill(x, y + 1, x + 1, y + catHeight - 1, borderColor);
            context.fill(x + catWidth - 1, y + 1, x + catWidth, y + catHeight - 1, borderColor);
            
            String catText = selected ? "§f" + categories[i] : "§7" + categories[i];
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(catText), 
                    x + catWidth / 2, y + 5, 0xFFFFFF);
        }
    }
    
    private void renderConfigsTab(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7💾 Управление конфигами"), width / 2, 95, 0xAAAAAA);
        
        List<String> configs = ConfigManager.listConfigs();
        String currentName = ConfigManager.getCurrentConfigName();
        
        int listX = width / 2 - 160;
        int listY = 110;
        int listWidth = 320;
        int listHeight = visibleItems * itemHeight;
        
        // Gradient background
        for (int i = 0; i < listHeight; i++) {
            int alpha = 180 - (i * 50 / listHeight);
            context.fill(listX, listY + i, listX + listWidth, listY + i + 1, 0x000000 | (alpha << 24));
        }
        
        // Border
        int borderPulse = (int)(Math.sin(animationTime * 2) * 30 + 100);
        int borderColor = 0xFF000000 | (borderPulse << 16) | (borderPulse << 8) | borderPulse;
        context.fill(listX, listY, listX + listWidth, listY + 1, borderColor);
        context.fill(listX, listY + listHeight - 1, listX + listWidth, listY + listHeight, borderColor);
        context.fill(listX, listY, listX + 1, listY + listHeight, borderColor);
        context.fill(listX + listWidth - 1, listY, listX + listWidth, listY + listHeight, borderColor);
        
        // Render configs
        for (int i = 0; i < visibleItems && (i + configScrollOffset) < configs.size(); i++) {
            int index = i + configScrollOffset;
            String configName = configs.get(index);
            
            int y = listY + i * itemHeight;
            
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth &&
                            mouseY >= y && mouseY <= y + itemHeight;
            
            boolean isCurrent = configName.equals(currentName);
            
            // Row background
            if (isCurrent) {
                int goldPulse = (int)(Math.sin(animationTime * 3 + i * 0.5) * 30 + 60);
                context.fill(listX + 1, y + 1, listX + listWidth - 1, y + itemHeight - 1, 
                        0x000000 | (goldPulse << 16) | (goldPulse << 8));
            } else if (hovered) {
                context.fill(listX + 1, y + 1, listX + listWidth - 1, y + itemHeight - 1, 0x40FFFFFF);
            }
            
            // Icon
            if (isCurrent) {
                context.drawTextWithShadow(textRenderer, Text.literal("§a▶"), listX + 8, y + 7, 0xFFAA00);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal("§7○"), listX + 8, y + 7, 0x888888);
            }
            
            // Config name
            String displayName = configName;
            if (displayName.length() > 30) {
                displayName = displayName.substring(0, 27) + "...";
            }
            
            int textColor = isCurrent ? 0xFFAA00 : (hovered ? 0xFFFFFF : 0xCCCCCC);
            context.drawTextWithShadow(textRenderer, Text.literal(displayName), 
                    listX + 25, y + 7, textColor);
            
            // Status
            if (isCurrent) {
                context.drawTextWithShadow(textRenderer, Text.literal("§a(текущий)"), 
                        listX + listWidth - 70, y + 7, 0x55FF55);
            }
        }
        
        if (configs.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("§7Нет сохранённых конфигов"), width / 2, listY + 50, 0xAAAAAA);
        }
        
        // Instructions
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7ЛКМ - загрузить | ПКМ - сохранить"),
                width / 2, listY + listHeight + 10, 0xAAAAAA);
    }
    
    private void renderSettingsTab(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int startY = 100;
        int lineSpacing = 28;
        
        // Scanner settings with fancy header
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§l⚙ Настройки сканера"), centerX, startY, 0xFFAA00);
        
        startY += lineSpacing;
        renderSettingRow(context, centerX, startY, "Радиус", 
                "§e" + BaseFinderClient.scanner.getScanRadius() + " блоков", mouseX, mouseY);
        
        startY += lineSpacing;
        String mode = BaseFinderClient.scanner.isLiteMode() ? 
                "§aLITE (Y < " + BaseFinderClient.scanner.getLiteHeightLimit() + ")" : "§bFULL";
        renderSettingRow(context, centerX, startY, "Режим", mode, mouseX, mouseY);
        
        startY += lineSpacing;
        renderSettingRow(context, centerX, startY, "Выбрано блоков", 
                "§e" + BaseFinderClient.scanner.getSelectedBlocks().size(), mouseX, mouseY);
        
        startY += lineSpacing * 1.5f;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§l🎮 Управление"), centerX, startY, 0xFFAA00);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f[O] §7- Открыть меню"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f[H] §7- Старт/Стоп сканера"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing * 1.5f;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§6§l💬 Команды"), centerX, startY, 0xFFAA00);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf start §7- Запустить"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf mode lite §7- Режим Y<30"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf cfg save <имя> §7- Сохранить"), centerX, startY, 0xFFFFFF);
        
        startY += lineSpacing;
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f/bf cfg load <имя> §7- Загрузить"), centerX, startY, 0xFFFFFF);
    }
    
    private void renderSettingRow(DrawContext context, int centerX, int y, String label, String value, int mouseX, int mouseY) {
        boolean hovered = Math.abs(mouseX - centerX) < 150 && Math.abs(mouseY - y) < 10;
        
        if (hovered) {
            context.fill(centerX - 150, y - 2, centerX + 150, y + 18, 0x20FFFFFF);
        }
        
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§f" + label + ": " + value), centerX, y, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Tab clicks
        int tabWidth = 100;
        int tabHeight = 24;
        int startX = width / 2 - (tabs.length * tabWidth) / 2;
        int y = 48;
        
        for (int i = 0; i < tabs.length; i++) {
            int x = startX + i * tabWidth;
            if (mouseX >= x && mouseX <= x + tabWidth && mouseY >= y && mouseY <= y + tabHeight) {
                currentTab = i;
                return true;
            }
        }
        
        // Category clicks
        if (currentTab == 0) {
            int catWidth = 55;
            int catHeight = 18;
            int catStartX = width / 2 - (categories.length * catWidth) / 2;
            int catY = 90;
            
            for (int i = 0; i < categories.length; i++) {
                int x = catStartX + i * catWidth;
                if (mouseX >= x && mouseX <= x + catWidth && mouseY >= catY && mouseY <= catY + catHeight) {
                    currentCategory = i;
                    applyFilters();
                    return true;
                }
            }
        }

        // Block list clicks
        if (currentTab == 0) {
            int listX = width / 2 - 160;
            int listY = 110;
            int listWidth = 320;
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
        
        // Config list clicks
        if (currentTab == 1) {
            List<String> configs = ConfigManager.listConfigs();
            int listX = width / 2 - 160;
            int listY = 110;
            int listWidth = 320;
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
                                Text.literal("§a[BaseFinder] ✓ Конфиг '" + configName + "' загружен!"), false);
                        }
                    } else if (button == 1) { // Right click - save
                        if (ConfigManager.saveConfig(configName)) {
                            ConfigManager.setCurrentConfigName(configName);
                            MinecraftClient.getInstance().player.sendMessage(
                                Text.literal("§a[BaseFinder] ✓ Конфиг '" + configName + "' сохранён!"), false);
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
