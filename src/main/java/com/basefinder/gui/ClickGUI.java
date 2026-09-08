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
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClickGUI extends Screen {

    private int selectedCategory = 0;
    private int selectedModule = 0;
    private int panelMode = 0; // 0 = modules, 1 = settings, 2 = blocks
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

    // Design Constants
    private static final int SIDEBAR_WIDTH = 110;
    private static final int MODULE_LIST_WIDTH = 220;
    private static final int ITEM_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 24;
    
    // Colors (Modern Dark Theme)
    private static final int COLOR_BG_MAIN = 0xFF050505;
    private static final int COLOR_BG_PANEL = 0xAA121216; // Semi-transparent
    private static final int COLOR_BG_ELEMENT = 0xAA1E1E24;
    private static final int COLOR_TEXT_MAIN = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFF888899;
    private static final int COLOR_ACCENT_STATIC = 0xFF7B2CBF; // Purple base

    public ClickGUI() {
        super(Text.literal("BaseFinder"));
        lastFrameTime = System.currentTimeMillis();

        allBlocks = new ArrayList<>();
        Registries.BLOCK.forEach(allBlocks::add);
        allBlocks.sort((a, b) -> Registries.BLOCK.getId(a).getPath().compareTo(Registries.BLOCK.getId(b).getPath()));
        filteredBlocks = new ArrayList<>(allBlocks);
    }

    @Override
    protected void init() {
        super.init();
        // Modern styled search field
        searchField = new TextFieldWidget(textRenderer, width - 320, 38, 160, 18, Text.literal(""));
        searchField.setMaxLength(30);
        searchField.setChangedListener(q -> applyBlockFilter());
        searchField.setVisible(false); // We will render it manually or handle visibility
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

        // 1. Background Gradient (Deep Dark)
        ctx.fill(0, 0, width, height, COLOR_BG_MAIN);
        // Subtle gradient overlay
        int gradStart = getAccentColor(0.0f);
        int gradEnd = getAccentColor(0.5f);
        ctx.fillGradient(0, 0, width, height, gradStart & 0x10FFFFFF, gradEnd & 0x05FFFFFF);

        // 2. Header
        renderHeader(ctx, mouseX, mouseY);

        // 3. Sidebar (Navigation)
        renderSidebar(ctx, mouseX, mouseY);

        // 4. Main Content Area
        int contentX = SIDEBAR_WIDTH;
        ctx.enableScissor(contentX, HEADER_HEIGHT, width, height);
        switch (panelMode) {
            case 0: renderModuleList(ctx, mouseX, mouseY, contentX); break;
            case 1: renderSettingsPanel(ctx, mouseX, mouseY, contentX); break;
            case 2: renderBlockSelector(ctx, mouseX, mouseY, contentX); break;
        }
        ctx.disableScissor();

        // Render search field if in block mode
        if (panelMode == 2) {
            searchField.setVisible(true);
            searchField.render(ctx, mouseX, mouseY, delta);
        } else {
            searchField.setVisible(false);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext ctx, int mouseX, int mouseY) {
        // Glassy header bar
        ctx.fill(0, 0, width, HEADER_HEIGHT, 0xCC1A1A20);
        ctx.fill(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, getAccentColor(0.0f));

        // Title with modern font shadow
        ctx.drawTextWithShadow(textRenderer, Text.literal("BASEFINDER"), 12, 7, 0xFFFFFF);
        
        // Version badge
        ctx.fill(width - 60, 6, width - 10, 18, 0xFF2A2A35);
        ctx.drawTextWithShadow(textRenderer, Text.literal("v2.0"), width - 55, 8, getAccentColor(0.2f));

        // Status Indicator
        boolean scanning = BaseFinderClient.scanner != null && BaseFinderClient.scanner.isRunning();
        String status = scanning ? "SCANNING" : "IDLE";
        int statusCol = scanning ? 0xFF00FF88 : 0xFFFF4444;
        
        // Pulsing dot
        int pulse = (int) (Math.sin(animationTime * 5) * 3);
        ctx.fill(width - 85, 9 + (3-pulse)/2, width - 80, 14 + (3-pulse)/2, statusCol);
        
        ctx.drawTextWithShadow(textRenderer, Text.literal(status), width - 75, 8, statusCol);
        
        // Config Name
        ctx.drawTextWithShadow(textRenderer, Text.literal(ConfigManager.getCurrentConfigName()), 140, 8, COLOR_TEXT_DIM);
    }

    private void renderSidebar(DrawContext ctx, int mouseX, int mouseY) {
        // Sidebar Background
        ctx.fill(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height, 0xAA0F0F13);
        ctx.fill(SIDEBAR_WIDTH - 1, HEADER_HEIGHT, SIDEBAR_WIDTH, height, getAccentColor(0.0f) & 0x30FFFFFF);

        int y = HEADER_HEIGHT + 10;

        // Navigation Tabs
        String[] modes = {"MODULES", "SETTINGS", "BLOCKS"};
        String[] icons = {"⚡", "⚙", "🧊"};
        
        for (int i = 0; i < modes.length; i++) {
            int h = 30;
            boolean selected = (panelMode == i);
            boolean hovered = mouseX >= 10 && mouseX <= SIDEBAR_WIDTH - 10 && mouseY >= y && mouseY <= y + h;

            if (selected) {
                // Active Tab Background
                ctx.fill(5, y, SIDEBAR_WIDTH - 5, y + h, 0xFF252530);
                ctx.fill(5, y, 6, y + h, getAccentColor(0.0f));
            } else if (hovered) {
                ctx.fill(5, y, SIDEBAR_WIDTH - 5, y + h, 0xFF1A1A22);
            }

            String text = icons[i] + "  " + modes[i];
            int color = selected ? 0xFFFFFF : COLOR_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(text), 15, y + 10, color);
            
            y += h + 5;
        }

        // Categories (Only in Modules Mode)
        if (panelMode == 0) {
            y += 10;
            ctx.drawTextWithShadow(textRenderer, Text.literal("CATEGORIES"), 15, y, 0x555566);
            y += 15;

            Module.Category[] cats = Module.Category.values();
            for (int i = 0; i < cats.length; i++) {
                int h = 20;
                boolean selected = (selectedCategory == i);
                boolean hovered = mouseX >= 10 && mouseX <= SIDEBAR_WIDTH - 10 && mouseY >= y && mouseY <= y + h;

                if (selected) {
                    ctx.fill(8, y, SIDEBAR_WIDTH - 8, y + h, 0xFF2A2A35);
                    // Colored strip based on category or accent
                    ctx.fill(8, y, 9, y + h, cats[i].color != 0 ? cats[i].color : getAccentColor(0.0f));
                } else if (hovered) {
                    ctx.fill(8, y, SIDEBAR_WIDTH - 8, y + h, 0xFF1F1F28);
                }

                String label = cats[i].displayName;
                int color = selected ? 0xFFFFFF : COLOR_TEXT_DIM;
                ctx.drawTextWithShadow(textRenderer, Text.literal(label), 15, y + 5, color);
                
                y += h + 2;
            }
        }
    }

    private void renderModuleList(DrawContext ctx, int mouseX, int mouseY, int startX) {
        Module.Category[] cats = Module.Category.values();
        if (selectedCategory >= cats.length) return;
        
        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null) return;

        List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
        
        int listX = startX + 20;
        int listY = HEADER_HEIGHT + 20;
        int listWidth = MODULE_LIST_WIDTH;
        
        // Category Title
        ctx.drawTextWithShadow(textRenderer, Text.literal(cats[selectedCategory].displayName.toUpperCase()), listX, listY, getAccentColor(0.0f));
        
        int startY = listY + 15;
        int visibleItems = (height - startY - 20) / ITEM_HEIGHT;

        for (int i = 0; i < visibleItems && (i + moduleScrollOffset) < modules.size(); i++) {
            int idx = i + moduleScrollOffset;
            Module mod = modules.get(idx);
            int y = startY + i * ITEM_HEIGHT;

            boolean selected = (selectedModule == idx);
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + ITEM_HEIGHT;

            // Item Background
            if (selected) {
                ctx.fill(listX, y, listX + listWidth, y + ITEM_HEIGHT, 0xFF252530);
                ctx.fill(listX, y, listX + 2, y + ITEM_HEIGHT, getAccentColor(0.0f));
            } else if (hovered) {
                ctx.fill(listX, y, listX + listWidth, y + ITEM_HEIGHT, 0xFF1A1A22);
            }

            // Module Name
            int nameColor = mod.isEnabled() ? 0xFFFFFF : COLOR_TEXT_DIM;
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), listX + 10, y + 5, nameColor);

            // Modern Toggle Switch (Right side)
            drawToggleSwitch(ctx, listX + listWidth - 35, y + 4, mod.isEnabled());
        }

        // Info Panel (Right Side of Content)
        renderInfoPanel(ctx, mouseX, mouseY, modules, listX + listWidth + 30);
    }

    private void drawToggleSwitch(DrawContext ctx, int x, int y, boolean enabled) {
        int width = 30;
        int height = 14;
        // Background track
        ctx.fill(x, y, x + width, y + height, enabled ? getAccentColor(0.0f) : 0xFF333340);
        // Circle knob
        int knobX = enabled ? x + width - height - 1 : x + 1;
        ctx.fill(knobX, y, knobX + height, y + height, 0xFFFFFFFF);
    }

    private void renderInfoPanel(DrawContext ctx, int mouseX, int mouseY, List<Module> modules, int panelX) {
        int panelY = HEADER_HEIGHT + 20;
        int panelWidth = width - panelX - 20;
        int panelHeight = height - panelY - 20;

        // Panel Background (Glass)
        ctx.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_BG_PANEL);
        ctx.drawHorizontalLine(panelX, panelX + panelWidth, panelY, getAccentColor(0.0f) & 0x40FFFFFF);

        if (modules.isEmpty() || selectedModule >= modules.size()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Select a module to view details"), panelX + 20, panelY + 20, COLOR_TEXT_DIM);
            return;
        }

        Module mod = modules.get(selectedModule);

        // Header
        ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), panelX + 20, panelY + 20, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getDescription()), panelX + 20, panelY + 35, COLOR_TEXT_DIM);

        // Status Badge
        String status = mod.isEnabled() ? "ACTIVE" : "INACTIVE";
        int statusColor = mod.isEnabled() ? 0xFF00FF88 : 0xFFFF4444;
        ctx.fill(panelX + 20, panelY + 55, panelX + 70, panelY + 68, 0xFF222222);
        ctx.drawTextWithShadow(textRenderer, Text.literal(status), panelX + 25, panelY + 58, statusColor);

        // Specific Stats
        int sy = panelY + 85;
        if (mod instanceof BaseFinderModule) {
            BaseFinderModule bf = (BaseFinderModule) mod;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Found Blocks: " + bf.getFoundCount()), panelX + 20, sy, 0xFFDDDDDD);
            sy += 15;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Selected: " + bf.getSelectedCount()), panelX + 20, sy, getAccentColor(0.0f));
            sy += 25;
            
            ctx.drawTextWithShadow(textRenderer, Text.literal("--- Quick Settings ---"), panelX + 20, sy, COLOR_TEXT_DIM);
            sy += 15;

            // Mini settings preview
            for (Setting<?> s : bf.getSettings()) {
                if (sy > panelY + panelHeight - 20) break;
                ctx.drawTextWithShadow(textRenderer, Text.literal(s.getName() + ": " + s.getValueAsString()), panelX + 25, sy, 0xFFAAAAAA);
                sy += 14;
            }
        } else {
             ctx.drawTextWithShadow(textRenderer, Text.literal("No specific stats available."), panelX + 20, sy, COLOR_TEXT_DIM);
        }
    }

    private void renderSettingsPanel(DrawContext ctx, int mouseX, int mouseY, int startX) {
        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null || mm.baseFinder == null) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("BaseFinder module not loaded"), startX + 20, HEADER_HEIGHT + 40, 0xFF555555);
            return;
        }

        BaseFinderModule bf = mm.baseFinder;
        int panelX = startX + 20;
        int panelY = HEADER_HEIGHT + 20;
        
        ctx.drawTextWithShadow(textRenderer, Text.literal("GLOBAL SETTINGS"), panelX, panelY, getAccentColor(0.0f));
        
        int sy = panelY + 20;
        List<Setting<?>> settings = bf.getSettings();

        for (int i = 0; i < settings.size(); i++) {
            int idx = i + settingScrollOffset;
            if (idx >= settings.size()) break;
            
            Setting<?> setting = settings.get(idx);
            int y = sy + i * 45;

            if (y > height - 40) break;

            // Setting Card Background
            ctx.fill(panelX, y, width - 20, y + 40, COLOR_BG_ELEMENT);
            
            // Name & Desc
            ctx.drawTextWithShadow(textRenderer, Text.literal(setting.getName()), panelX + 15, y + 10, 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal(setting.getDescription()), panelX + 15, y + 24, COLOR_TEXT_DIM);

            // Controls
            int controlX = width - 180;
            int controlY = y + 12;

            if (setting instanceof BoolSetting) {
                BoolSetting bs = (BoolSetting) setting;
                drawToggleSwitch(ctx, controlX, controlY, bs.get());
            } else if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) setting;
                drawSlider(ctx, controlX, controlY, 120, 10, ns.getPercentage(), getAccentColor(0.0f));
                ctx.drawTextWithShadow(textRenderer, Text.literal(ns.getValueAsString()), controlX + 130, controlY - 2, 0xFFFFFF);
            } else if (setting instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) setting;
                ctx.fill(controlX, controlY - 2, controlX + 100, controlY + 12, 0xFF2A2A35);
                ctx.drawTextWithShadow(textRenderer, Text.literal(ms.getMode()), controlX + 10, controlY, getAccentColor(0.2f));
            }
        }
    }

    private void drawSlider(DrawContext ctx, int x, int y, int width, int height, double percent, int color) {
        // Track
        ctx.fill(x, y + height/2 - 1, x + width, y + height/2 + 1, 0xFF333340);
        // Fill
        int fillW = (int) (width * percent);
        ctx.fill(x, y + height/2 - 1, x + fillW, y + height/2 + 1, color);
        // Knob
        ctx.fill(x + fillW - 4, y + height/2 - 4, x + fillW + 4, y + height/2 + 4, 0xFFFFFFFF);
    }

    private void renderBlockSelector(DrawContext ctx, int mouseX, int mouseY, int startX) {
        int panelX = startX + 20;
        int panelY = HEADER_HEIGHT + 20;
        int panelWidth = width - panelX - 20;

        // Search Field Position Update
        searchField.setX(width - 320);
        searchField.setY(38);

        // Filter Chips
        int catY = 45;
        int chipW = 70;
        for (int i = 0; i < blockCategories.length; i++) {
            int x = panelX + 5 + i * (chipW + 5);
            boolean selected = (categoryFilter == i);
            boolean hovered = mouseX >= x && mouseX <= x + chipW && mouseY >= catY && mouseY <= catY + 20;

            int bgColor = selected ? getAccentColor(0.0f) : (hovered ? 0xFF2A2A35 : 0xFF1A1A22);
            int textColor = selected ? 0xFFFFFF : COLOR_TEXT_DIM;

            // Rounded look simulation
            ctx.fill(x, catY, x + chipW, catY + 20, bgColor);
            if(selected) ctx.fill(x, catY + 18, x + chipW, catY + 20, 0xFFFFFFFF); // Underline
            
            ctx.drawTextWithShadow(textRenderer, Text.literal(blockCategories[i]), x + 10, catY + 6, textColor);
        }

        // List
        int listY = catY + 35;
        int itemH = 26;
        int visible = (height - listY - 20) / itemH;

        for (int i = 0; i < visible && (i + blockScrollOffset) < filteredBlocks.size(); i++) {
            int idx = i + blockScrollOffset;
            Block block = filteredBlocks.get(idx);
            String blockName = block.getName().getString();
            String blockId = Registries.BLOCK.getId(block).getPath();

            int y = listY + i * itemH;
            boolean hovered = mouseX >= panelX && mouseX <= width - 20 && mouseY >= y && mouseY <= y + itemH;
            boolean isSelected = BaseFinderClient.scanner != null && BaseFinderClient.scanner.getSelectedBlocks().contains(block);

            // Row Background
            if (isSelected) {
                ctx.fill(panelX, y, width - 20, y + itemH, 0xAA1A3A2A); // Green tint
                ctx.fill(panelX, y, panelX + 3, y + itemH, 0xFF00FF88);
            } else if (hovered) {
                ctx.fill(panelX, y, width - 20, y + itemH, 0xAA2A2A35);
            }

            // Icon
            ItemStack stack = new ItemStack(block);
            ctx.drawItem(stack, panelX + 10, y + 4);

            // Text
            ctx.drawTextWithShadow(textRenderer, Text.literal(blockName), panelX + 40, y + 5, isSelected ? 0xFF00FF88 : 0xFFFFFF);
            ctx.drawTextWithShadow(textRenderer, Text.literal(blockId), panelX + 40, y + 16, COLOR_TEXT_DIM);
            
            // Selection Checkbox visual
            ctx.drawTextWithShadow(textRenderer, Text.literal(isSelected ? "✔" : "○"), width - 40, y + 6, isSelected ? 0xFF00FF88 : 0x555555);
        }
        
        // Footer count
        int count = BaseFinderClient.scanner != null ? BaseFinderClient.scanner.getSelectedBlocks().size() : 0;
        ctx.fill(0, height - 20, width, height, 0xCC0F0F13);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Selected Blocks: " + count + "   |   Total Visible: " + filteredBlocks.size()), 10, height - 16, COLOR_TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        // Sidebar clicks
        String[] modes = {"MODULES", "SETTINGS", "BLOCKS"};
        int y = HEADER_HEIGHT + 10;
        for (int i = 0; i < modes.length; i++) {
            int h = 30;
            if (mouseX >= 10 && mouseX <= SIDEBAR_WIDTH - 10 && mouseY >= y && mouseY <= y + h) {
                panelMode = i;
                return true;
            }
            y += h + 5;
        }

        // Category clicks
        if (panelMode == 0) {
            y += 25; // Skip title
            Module.Category[] cats = Module.Category.values();
            for (int i = 0; i < cats.length; i++) {
                int h = 20;
                if (mouseX >= 10 && mouseX <= SIDEBAR_WIDTH - 10 && mouseY >= y && mouseY <= y + h) {
                    selectedCategory = i;
                    selectedModule = 0;
                    moduleScrollOffset = 0;
                    return true;
                }
                y += h + 2;
            }

            // Module List Clicks
            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
                int listX = SIDEBAR_WIDTH + 40;
                int listY = HEADER_HEIGHT + 35;
                
                for (int i = 0; i < modules.size(); i++) {
                    int itemY = listY + i * ITEM_HEIGHT;
                    if (mouseX >= listX && mouseX <= listX + MODULE_LIST_WIDTH && mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT) {
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

        // Settings Clicks
        if (panelMode == 1) {
            handleSettingsClick(mouseX, mouseY, button);
        }

        // Block Selector Clicks
        if (panelMode == 2) {
            // Filter chips
            int catY = 45;
            int chipW = 70;
            int panelX = SIDEBAR_WIDTH + 40;
            for (int i = 0; i < blockCategories.length; i++) {
                int x = panelX + 5 + i * (chipW + 5);
                if (mouseX >= x && mouseX <= x + chipW && mouseY >= catY && mouseY <= catY + 20) {
                    categoryFilter = i;
                    applyBlockFilter();
                    return true;
                }
            }
            
            // Block items
            handleBlockClick(mouseX, mouseY, button, panelX, catY + 35);
        }

        return true;
    }

    private void handleSettingsClick(double mouseX, double mouseY, int button) {
        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null || mm.baseFinder == null) return;

        BaseFinderModule bf = mm.baseFinder;
        int panelX = SIDEBAR_WIDTH + 40;
        int panelY = HEADER_HEIGHT + 20;
        int sy = panelY + 20;

        List<Setting<?>> settings = bf.getSettings();
        for (int i = 0; i < settings.size(); i++) {
            int idx = i + settingScrollOffset;
            if (idx >= settings.size()) break;
            
            Setting<?> setting = settings.get(idx);
            int y = sy + i * 45;
            
            int controlX = width - 180;
            int controlY = y + 12;

            if (setting instanceof BoolSetting) {
                BoolSetting bs = (BoolSetting) setting;
                if (mouseX >= controlX && mouseX <= controlX + 30 && mouseY >= controlY && mouseY <= controlY + 14) {
                    bs.toggle();
                    return;
                }
            } else if (setting instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting) setting;
                int sliderW = 120;
                if (mouseX >= controlX && mouseX <= controlX + sliderW && mouseY >= controlY && mouseY <= controlY + 10) {
                    double pct = MathHelper.clamp((mouseX - controlX) / sliderW, 0.0, 1.0);
                    ns.setFromPercentage(pct);
                    return;
                }
            } else if (setting instanceof ModeSetting) {
                ModeSetting ms = (ModeSetting) setting;
                if (mouseX >= controlX && mouseX <= controlX + 100 && mouseY >= controlY - 2 && mouseY <= controlY + 12) {
                    ms.cycle();
                    return;
                }
            }
        }
    }

    private void handleBlockClick(double mouseX, double mouseY, int button, int panelX, int listY) {
        int itemH = 26;
        int visible = (height - listY - 20) / itemH;

        for (int i = 0; i < visible && (i + blockScrollOffset) < filteredBlocks.size(); i++) {
            int idx = i + blockScrollOffset;
            int y = listY + i * itemH;
            if (mouseX >= panelX && mouseX <= width - 20 && mouseY >= y && mouseY <= y + itemH) {
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
        if (panelMode == 0) {
            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                Module.Category[] cats = Module.Category.values();
                List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
                int maxScroll = Math.max(0, modules.size() - (height - 80) / ITEM_HEIGHT);
                moduleScrollOffset = (int) Math.max(0, Math.min(maxScroll, moduleScrollOffset - verticalAmount));
            }
        } else if (panelMode == 1) {
             ModuleManager mm = BaseFinderClient.moduleManager;
             if(mm != null && mm.baseFinder != null) {
                 int maxScroll = Math.max(0, mm.baseFinder.getSettings().size() - 10);
                 settingScrollOffset = (int) Math.max(0, Math.min(maxScroll, settingScrollOffset - verticalAmount));
             }
        } else if (panelMode == 2) {
            int maxScroll = Math.max(0, filteredBlocks.size() - (height - 100) / 26);
            blockScrollOffset = (int) Math.max(0, Math.min(maxScroll, blockScrollOffset - verticalAmount));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
        MinecraftClient.getInstance().setScreen(null);
    }

    // Helper for smooth color transition
    private int getAccentColor(float offset) {
        float hue = (animationTime * 0.15f + offset) % 1.0f;
        // Clamp saturation and brightness for a neon/pastel look suitable for dark themes
        return Color.HSBtoRGB(hue, 0.65f, 0.9f);
    }
}
