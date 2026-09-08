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
import java.util.Random;
import java.util.stream.Collectors;

public class ClickGUI extends Screen {

    private int selectedCategory = 0;
    private int selectedModule = 0;
    private int panelMode = 0; // 0 = modules, 1 = settings, 2 = blocks, 3 = theme
    
    // Animation & Particles
    private float animationTime = 0;
    private long lastFrameTime = 0;
    private float openAnimation = 0f; // 0 to 1
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    // Positions (Persisted)
    private int sidebarX = 0;
    private int sidebarY = 28;
    private int contentX = 125;
    private int contentY = 35;
    
    // Dragging state
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
    private int themeScrollOffset = 0;

    private final int SIDEBAR_WIDTH = 120;
    private final int MODULE_LIST_WIDTH = 200;
    private final int ITEM_HEIGHT = 22;
    
    // Theme Settings (Linked to ThemeManager if exists, else local)
    private float currentHue = 0.6f; // Purple default
    private float currentAlpha = 0.9f;
    private float cornerRadius = 4.0f;
    private float fontScale = 1.0f;

    public ClickGUI() {
        super(Text.literal("freezdlc"));
        lastFrameTime = System.currentTimeMillis();
        
        // Init particles
        for(int i=0; i<50; i++) {
            particles.add(new Particle());
        }

        allBlocks = new ArrayList<>();
        Registries.BLOCK.forEach(allBlocks::add);
        allBlocks.sort((a, b) -> Registries.BLOCK.getId(a).getPath().compareTo(Registries.BLOCK.getId(b).getPath()));
        filteredBlocks = new ArrayList<>(allBlocks);
        
        // Load saved positions (Mock implementation, replace with real Config loading)
        // sidebarX = Config.getInt("gui_sidebar_x", 0); 
        // ...
    }

    @Override
    protected void init() {
        super.init();
        // Search field positioned relative to content area
        int sfX = contentX + MODULE_LIST_WIDTH + 20; 
        searchField = new TextFieldWidget(textRenderer, sfX, contentY + 35, 180, 18, Text.literal(""));
        searchField.setMaxLength(30);
        searchField.setChangedListener(q -> applyBlockFilter());
        addDrawableChild(searchField);
        
        openAnimation = 0f;
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
        float frameDelta = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;
        
        // Update animations
        animationTime += frameDelta;
        if (openAnimation < 1.0f) openAnimation += frameDelta * 0.2f; // Smooth open
        
        // Update particles
        for (Particle p : particles) {
            p.update(frameDelta, width, height, mouseX, mouseY);
        }

        // Background with Gradient
        renderBackground(ctx, mouseX, mouseY, delta);
        
        // Draw Particles first (background layer)
        ctx.enableScissor(0, 0, width, height);
        for (Particle p : particles) {
            p.render(ctx, currentHue);
        }
        ctx.disableScissor();

        // Main GUI Background (Dark Glass)
        int bgAlpha = (int)(180 * currentAlpha);
        ctx.fill(0, 0, width, height, new Color(5, 5, 10, bgAlpha).getRGB());

        // Apply Open Animation Scale/Offset
        float animOffset = (1 - openAnimation) * 20;
        
        // Render Components with Animation Interpolation
        int sbX = sidebarX;
        int sbY = (int)(sidebarY + animOffset);
        int cntX = contentX;
        int cntY = (int)(contentY + animOffset);

        // Sidebar
        renderSidebar(ctx, sbX, sbY, mouseX, mouseY);

        // Content Area
        switch (panelMode) {
            case 0: renderModuleList(ctx, cntX, cntY, mouseX, mouseY); break;
            case 1: renderSettingsPanel(ctx, cntX, cntY, mouseX, mouseY); break;
            case 2: renderBlockSelector(ctx, cntX, cntY, mouseX, mouseY); break;
            case 3: renderThemePanel(ctx, cntX, cntY, mouseX, mouseY); break;
        }

        // Top Bar (Always on top)
        renderTopBar(ctx, mouseX, mouseY, sbX, cntX);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY, int sbX, int cntX) {
        int barHeight = 28;
        // Gradient Top Bar
        int color1 = new Color(20, 20, 35, 255).getRGB();
        int color2 = getAccentColor(0.5f);
        
        ctx.fillGradient(0, 0, width, barHeight, color1, color2);
        
        // Title
        ctx.drawTextWithShadow(textRenderer, Text.literal("freezdlc"), 10, 10, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("v2.1"), 85, 10, 0x888888);

        // Status
        String status = BaseFinderClient.scanner != null && BaseFinderClient.scanner.isRunning() ? "SCANNING" : "IDLE";
        int statusColor = status.equals("SCANNING") ? 0x55FF55 : 0xFF5555;
        ctx.drawTextWithShadow(textRenderer, Text.literal(status), width - 80, 10, statusColor);
        
        // Drag hints
        ctx.drawTextWithShadow(textRenderer, Text.literal("[Drag Sidebars]"), width/2 - 40, 10, 0x555555);
    }

    private void renderSidebar(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        int h = height - y;
        
        // Sidebar Background (Glass)
        int bgColor = new Color(10, 10, 20, (int)(200 * currentAlpha)).getRGB();
        fillRounded(ctx, x, y, x + SIDEBAR_WIDTH, y + h, (int)cornerRadius, bgColor);
        
        // Accent Line
        ctx.fill(x, y, x + 3, y + h, getAccentColor(0.8f));

        // Panel Mode Buttons
        String[] modes = {"Modules", "Settings", "Blocks", "Theme"};
        for (int i = 0; i < modes.length; i++) {
            int btnY = y + 35 + i * 32;
            boolean selected = (panelMode == i);
            boolean hovered = isHovered(mouseX, mouseY, x + 5, btnY, SIDEBAR_WIDTH - 10, 28);

            int btnColor = selected ? new Color(40, 40, 60, 255).getRGB() : (hovered ? new Color(30, 30, 50, 200).getRGB() : 0x00000000);
            
            if (selected || hovered) {
                fillRounded(ctx, x + 5, btnY, x + SIDEBAR_WIDTH - 5, btnY + 28, (int)cornerRadius, btnColor);
            }
            
            if (selected) {
                ctx.fill(x + 5, btnY + 14, x + 8, btnY + 16, getAccentColor(1.0f));
            }

            String label = selected ? "> " + modes[i] : "  " + modes[i];
            ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 15, btnY + 9, selected ? 0xFFFFFF : 0x888888);
        }

        // Category list (only in modules mode)
        if (panelMode == 0) {
            int startY = y + 140;
            ctx.drawTextWithShadow(textRenderer, Text.literal("Categories"), x + 10, startY - 15, 0x666666);

            Module.Category[] cats = Module.Category.values();
            for (int i = 0; i < cats.length; i++) {
                int catY = startY + i * 22;
                boolean selected = (selectedCategory == i);
                boolean hovered = isHovered(mouseX, mouseY, x + 5, catY, SIDEBAR_WIDTH - 10, 20);

                if (selected) {
                    ctx.fill(x + 5, catY, x + SIDEBAR_WIDTH - 5, catY + 20, new Color(40, 40, 60, 255).getRGB());
                    ctx.fill(x + 5, catY, x + 7, catY + 20, cats[i].color); // Category color
                } else if (hovered) {
                    ctx.fill(x + 5, catY, x + SIDEBAR_WIDTH - 5, catY + 20, new Color(30, 30, 50, 200).getRGB());
                }

                String label = selected ? "> " + cats[i].displayName : "  " + cats[i].displayName;
                ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 15, catY + 6, selected ? 0xFFFFFF : 0x888888);
            }
        }
        
        // Drag Handle (Top of sidebar)
        if (isHovered(mouseX, mouseY, x, y, SIDEBAR_WIDTH, 28)) {
             ctx.fill(x, y, x + SIDEBAR_WIDTH, y + 2, getAccentColor(0.5f));
        }
    }

    private void renderModuleList(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        int listWidth = MODULE_LIST_WIDTH;
        int listHeight = height - 40;

        // Background
        int bgColor = new Color(15, 15, 25, (int)(220 * currentAlpha)).getRGB();
        fillRounded(ctx, x, y, x + listWidth, y + listHeight, (int)cornerRadius, bgColor);

        // Header
        Module.Category[] cats = Module.Category.values();
        if (selectedCategory < cats.length) {
            ctx.drawTextWithShadow(textRenderer, Text.literal(cats[selectedCategory].displayName), x + 15, y + 10, getAccentColor(1.0f));
        }

        // List
        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null) return;

        List<Module> modules = mm.getModulesByCategory(cats[selectedCategory]);
        int startY = y + 35;
        int visibleItems = (listHeight - 40) / ITEM_HEIGHT;

        for (int i = 0; i < visibleItems && (i + moduleScrollOffset) < modules.size(); i++) {
            int idx = i + moduleScrollOffset;
            Module mod = modules.get(idx);
            int itemY = startY + i * ITEM_HEIGHT;

            boolean selected = (selectedModule == idx);
            boolean hovered = isHovered(mouseX, mouseY, x + 2, itemY, listWidth - 4, ITEM_HEIGHT);

            int itemColor = 0x00000000;
            if (selected) itemColor = new Color(40, 40, 60, 255).getRGB();
            else if (hovered) itemColor = new Color(30, 30, 50, 150).getRGB();

            if (itemColor != 0) {
                fillRounded(ctx, x + 2, itemY, x + listWidth - 2, itemY + ITEM_HEIGHT, (int)cornerRadius, itemColor);
            }

            // Name
            int nameColor = mod.isEnabled() ? 0x55FF55 : (selected ? 0xFFFFFF : 0xAAAAAA);
            ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), x + 12, itemY + 7, nameColor);

            // Toggle State
            String state = mod.isEnabled() ? "ON" : "OFF";
            int stateCol = mod.isEnabled() ? getAccentColor(0.8f) : 0xFF5555;
            ctx.drawTextWithShadow(textRenderer, Text.literal(state), x + listWidth - 30, itemY + 7, stateCol);
        }
        
        // Info Panel (Right side of content)
        renderInfoPanel(ctx, x + listWidth + 10, y, mouseX, mouseY, modules);
    }

    private void renderInfoPanel(DrawContext ctx, int x, int y, int mouseX, int mouseY, List<Module> modules) {
        int w = width - x - 10;
        int h = height - 40;
        
        int bgColor = new Color(15, 15, 25, (int)(220 * currentAlpha)).getRGB();
        fillRounded(ctx, x, y, x + w, y + h, (int)cornerRadius, bgColor);

        if (modules.isEmpty() || selectedModule >= modules.size()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Select a module to view details"), x + 20, y + 20, 0x666666);
            return;
        }

        Module mod = modules.get(selectedModule);
        ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getName()), x + 20, y + 20, getAccentColor(1.0f));
        ctx.drawTextWithShadow(textRenderer, Text.literal(mod.getDescription()), x + 20, y + 40, 0xAAAAAA);
        
        // Simple stats
        ctx.drawTextWithShadow(textRenderer, Text.literal("Status: " + (mod.isEnabled() ? "Enabled" : "Disabled")), x + 20, y + 60, mod.isEnabled() ? 0x55FF55 : 0xFF5555);
    }

    private void renderSettingsPanel(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        int w = width - x - 10;
        int h = height - 40;
        int bgColor = new Color(15, 15, 25, (int)(220 * currentAlpha)).getRGB();
        fillRounded(ctx, x, y, x + w, y + h, (int)cornerRadius, bgColor);

        ModuleManager mm = BaseFinderClient.moduleManager;
        if (mm == null || mm.baseFinder == null) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("BaseFinder not initialized"), x+20, y+20, 0xFF5555);
            return;
        }

        BaseFinderModule bf = mm.baseFinder;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Global Settings"), x + 20, y + 15, 0xFFFFFF);

        List<Setting<?>> settings = bf.getSettings();
        int sy = y + 40;
        
        // Simple rendering loop for settings (can be expanded)
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            if (sy > y + h - 20) break;
            
            ctx.drawTextWithShadow(textRenderer, Text.literal(s.getName()), x + 20, sy, 0xDDDDDD);
            ctx.drawTextWithShadow(textRenderer, Text.literal(s.getValueAsString()), x + w - 80, sy, getAccentColor(0.8f));
            sy += 25;
            
            // Interactive area simplified for brevity
            if (isHovered(mouseX, mouseY, x+20, sy-25, w-40, 25)) {
                 // Hover effect logic could go here
            }
        }
    }
    
    private void renderBlockSelector(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
         // Reuse logic from original but with rounded backgrounds
         int w = width - x - 10;
         int h = height - 65;
         int bgColor = new Color(15, 15, 25, (int)(220 * currentAlpha)).getRGB();
         fillRounded(ctx, x, y+25, x + w, y+25 + h, (int)cornerRadius, bgColor);
         
         // Render search field manually if needed or let widget handle it
         searchField.setX(x + w - 190);
         searchField.setY(y + 40);
         
         ctx.drawTextWithShadow(textRenderer, Text.literal("Block Selector (" + filteredBlocks.size() + ")"), x + 20, y + 45, 0xFFFFFF);
         
         // List rendering (simplified)
         int listY = y + 70;
         int itemH = 24;
         int visible = h / itemH;
         
         for(int i=0; i<visible && (i+blockScrollOffset) < filteredBlocks.size(); i++) {
             int idx = i + blockScrollOffset;
             Block b = filteredBlocks.get(idx);
             int ly = listY + i * itemH;
             
             boolean selected = BaseFinderClient.scanner != null && BaseFinderClient.scanner.getSelectedBlocks().contains(b);
             boolean hovered = isHovered(mouseX, mouseY, x+10, ly, w-20, itemH);
             
             int col = selected ? new Color(20, 50, 20, 200).getRGB() : (hovered ? new Color(30,30,40, 150).getRGB() : 0x00000000);
             if(col != 0) fillRounded(ctx, x+10, ly, x+w-10, ly+itemH, (int)cornerRadius, col);
             
             ItemStack stack = new ItemStack(b);
             ctx.drawItem(stack, x + 20, ly + 3);
             ctx.drawTextWithShadow(textRenderer, Text.literal(b.getName().getString()), x + 45, ly + 6, selected ? 0x55FF55 : 0xDDDDDD);
         }
    }

    private void renderThemePanel(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        int w = width - x - 10;
        int h = height - 40;
        int bgColor = new Color(15, 15, 25, (int)(220 * currentAlpha)).getRGB();
        fillRounded(ctx, x, y, x + w, y + h, (int)cornerRadius, bgColor);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Theme Customization"), x + 20, y + 20, getAccentColor(1.0f));
        
        int sy = y + 50;
        
        // Hue Slider
        ctx.drawTextWithShadow(textRenderer, Text.literal("Color Hue"), x + 20, sy, 0xFFFFFF);
        drawSlider(ctx, x + 20, sy + 15, w - 40, 10, currentHue, 0x00000000); // Rainbow gradient handled in drawSlider
        if (isHovered(mouseX, mouseY, x+20, sy+15, w-40, 10) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            currentHue = (float)(mouseX - (x + 20)) / (w - 40);
            currentHue = MathHelper.clamp(currentHue, 0, 1);
        }
        sy += 40;

        // Alpha Slider
        ctx.drawTextWithShadow(textRenderer, Text.literal("Transparency: " + (int)(currentAlpha * 255)), x + 20, sy, 0xFFFFFF);
        drawSlider(ctx, x + 20, sy + 15, w - 40, 10, currentAlpha, 0xFFFFFFFF);
        if (isHovered(mouseX, mouseY, x+20, sy+15, w-40, 10) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            currentAlpha = (float)(mouseX - (x + 20)) / (w - 40);
            currentAlpha = MathHelper.clamp(currentAlpha, 0.5f, 1.0f);
        }
        sy += 40;
        
        // Corner Radius
        ctx.drawTextWithShadow(textRenderer, Text.literal("Roundness: " + (int)cornerRadius), x + 20, sy, 0xFFFFFF);
        drawSlider(ctx, x + 20, sy + 15, w - 40, 10, cornerRadius / 20.0f, 0xFFFFFFFF);
        if (isHovered(mouseX, mouseY, x+20, sy+15, w-40, 10) && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            cornerRadius = ((float)(mouseX - (x + 20)) / (w - 40)) * 20.0f;
            cornerRadius = MathHelper.clamp(cornerRadius, 0, 20);
        }
        sy += 40;
        
        ctx.drawTextWithShadow(textRenderer, Text.literal("Changes are applied instantly!"), x + 20, sy + 20, 0x888888);
    }

    private void drawSlider(DrawContext ctx, int x, int y, int w, int h, float val, int baseColor) {
        ctx.fill(x, y, x + w, y + h, 0xFF222222);
        int fillW = (int)(w * val);
        
        if (baseColor == 0x00000000) {
            // Rainbow gradient for Hue
            for(int i=0; i<fillW; i++) {
                float hue = (float)i / w;
                ctx.fill(x + i, y, x + i + 1, y + h, Color.HSBtoRGB(hue, 1.0f, 1.0f));
            }
        } else {
            ctx.fill(x, y, x + fillW, y + h, getAccentColor(0.8f));
        }
        
        // Knob
        ctx.fill(x + fillW - 2, y - 2, x + fillW + 2, y + h + 2, 0xFFFFFFFF);
    }

    // Helper: Rounded Rectangles (Simulated with fill for performance, real rounding needs custom drawing)
    private void fillRounded(DrawContext ctx, int x1, int y1, int x2, int y2, int radius, int color) {
        // Simple approximation: Main rect + corners (Full implementation requires drawing arcs)
        // For now, standard fill with slight transparency looks good enough with the glass effect
        ctx.fill(x1, y1, x2, y2, color);
        
        // If you want real rounding, we need to draw 4 circles at corners and a cross in middle
        // But standard Minecraft DrawContext doesn't have drawArc easily without mods.
        // This placeholder ensures the code compiles and looks decent.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchField.mouseClicked(mouseX, mouseY, button)) return true;

        // Drag Logic Sidebar
        if (button == 0 && isHovered(mouseX, mouseY, sidebarX, sidebarY, SIDEBAR_WIDTH, 28)) {
            draggingSidebar = true;
            dragOffsetX = (int)(mouseX - sidebarX);
            dragOffsetY = (int)(mouseY - sidebarY);
            return true;
        }
        
        // Drag Logic Content (Top bar of content area)
        if (button == 0 && isHovered(mouseX, mouseY, contentX, contentY, width - contentX, 28)) {
            draggingContent = true;
            dragOffsetX = (int)(mouseX - contentX);
            dragOffsetY = (int)(mouseY - contentY);
            return true;
        }

        // Sidebar Clicks
        String[] modes = {"Modules", "Settings", "Blocks", "Theme"};
        for (int i = 0; i < modes.length; i++) {
            int btnY = sidebarY + 35 + i * 32;
            if (isHovered(mouseX, mouseY, sidebarX + 5, btnY, SIDEBAR_WIDTH - 10, 28)) {
                panelMode = i;
                return true;
            }
        }
        
        // Category Clicks
        if (panelMode == 0) {
            Module.Category[] cats = Module.Category.values();
            int startY = sidebarY + 140;
            for (int i = 0; i < cats.length; i++) {
                int catY = startY + i * 22;
                if (isHovered(mouseX, mouseY, sidebarX + 5, catY, SIDEBAR_WIDTH - 10, 20)) {
                    selectedCategory = i;
                    selectedModule = 0;
                    return true;
                }
            }
        }
        
        // Module Clicks
        if (panelMode == 0) {
             ModuleManager mm = BaseFinderClient.moduleManager;
             if (mm != null) {
                 List<Module> mods = mm.getModulesByCategory(Module.Category.values()[selectedCategory]);
                 int startY = contentY + 35;
                 for(int i=0; i<mods.size(); i++) {
                     int my = startY + (i - moduleScrollOffset) * ITEM_HEIGHT;
                     if (isHovered(mouseX, mouseY, contentX + 2, my, MODULE_LIST_WIDTH - 4, ITEM_HEIGHT)) {
                         if (button == 0) selectedModule = i + moduleScrollOffset;
                         if (button == 1) mods.get(i + moduleScrollOffset).toggle();
                         return true;
                     }
                 }
             }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSidebar = false;
        draggingContent = false;
        searchField.mouseReleased(mouseX, mouseY, button);
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
            // Update search field pos
            if(searchField != null) {
                searchField.setX(contentX + MODULE_LIST_WIDTH + 20);
                searchField.setY(contentY + 35);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (panelMode == 0) {
            ModuleManager mm = BaseFinderClient.moduleManager;
            if (mm != null) {
                List<Module> mods = mm.getModulesByCategory(Module.Category.values()[selectedCategory]);
                int max = Math.max(0, mods.size() - 10);
                moduleScrollOffset = (int)MathHelper.clamp(moduleScrollOffset - verticalAmount, 0, max);
            }
        } else if (panelMode == 2) {
            int max = Math.max(0, filteredBlocks.size() - 15);
            blockScrollOffset = (int)MathHelper.clamp(blockScrollOffset - verticalAmount, 0, max);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        // Save Config Here
        // Config.save("gui_sidebar_x", sidebarX); ...
        super.close();
    }

    private int getAccentColor(float brightness) {
        return Color.HSBtoRGB(currentHue, 0.8f, brightness);
    }

    private boolean isHovered(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    // Particle Class
    private static class Particle {
        float x, y, vx, vy, size;
        float life, maxLife;
        
        public Particle() {
            reset((int)(Math.random() * 1000), (int)(Math.random() * 1000));
        }
        
        public void reset(int w, int h) {
            x = (float)(Math.random() * w);
            y = (float)(Math.random() * h);
            vx = (float)((Math.random() - 0.5) * 0.5);
            vy = (float)((Math.random() - 0.5) * 0.5);
            size = (float)(Math.random() * 3 + 1);
            life = 0;
            maxLife = (float)(Math.random() * 200 + 100);
        }
        
        public void update(float dt, int w, int h, int mx, int my) {
            x += vx * (dt * 60);
            y += vy * (dt * 60);
            life++;
            
            // Mouse interaction
            float dx = mx - x;
            float dy = my - y;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            if(dist < 100) {
                x -= dx * 0.01;
                y -= dy * 0.01;
            }
            
            if (life > maxLife || x < 0 || x > w || y < 0 || y > h) {
                reset(w, h);
            }
        }
        
        public void render(DrawContext ctx, float hue) {
            int alpha = (int)(255 * (1 - life/maxLife) * 0.5f);
            int col = new Color(Color.HSBtoRGB((hue + life/500f)%1.0f, 0.8f, 1.0f) & 0x00FFFFFF | (alpha << 24)).getRGB();
            ctx.fill((int)x, (int)y, (int)(x+size), (int)(y+size), col);
        }
    }
}
