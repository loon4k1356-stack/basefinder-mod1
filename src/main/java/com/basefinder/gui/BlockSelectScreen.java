package com.basefinder.gui;

import com.basefinder.BaseFinderClient;
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

    public BlockSelectScreen() {
        super(Text.literal("BaseFinder - Block Selection"));
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
        searchField = new TextFieldWidget(textRenderer, width / 2 - 100, 20, 200, 20, Text.literal("Search..."));
        searchField.setMaxLength(50);
        searchField.setChangedListener(this::onSearchChanged);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(
                Text.literal(BaseFinderClient.scanner.isRunning() ? "Stop Scanner" : "Start Scanner"),
                button -> { BaseFinderClient.toggleScanner(); close(); }
        ).dimensions(width / 2 - 100, height - 60, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Clear Selection"),
                button -> { BaseFinderClient.scanner.clearSelectedBlocks(); selectedBlocks.clear(); }
        ).dimensions(width / 2 + 5, height - 60, 95, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Close"),
                button -> close()
        ).dimensions(width / 2 - 50, height - 35, 100, 20).build());
    }

    private void onSearchChanged(String query) {
        if (query.isEmpty()) {
            filteredBlocks = new ArrayList<>(allBlocks);
        } else {
            String lowerQuery = query.toLowerCase();
            filteredBlocks = allBlocks.stream()
                    .filter(block -> {
                        String blockId = Registries.BLOCK.getId(block).getPath().toLowerCase();
                        String blockName = block.getName().getString().toLowerCase();
                        return blockId.contains(lowerQuery) || blockName.contains(lowerQuery);
                    })
                    .collect(Collectors.toList());
        }
        scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Select Blocks to Scan"), width / 2, 5, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Selected: " + selectedBlocks.size() + " blocks"),
                width / 2, 45, 0x55FF55);

        int listX = width / 2 - 150;
        int listY = 55;
        int listWidth = 300;
        int listHeight = visibleItems * itemHeight;
        context.fill(listX, listY, listX + listWidth, listY + listHeight, 0x80000000);

        for (int i = 0; i < visibleItems && (i + scrollOffset) < filteredBlocks.size(); i++) {
            int index = i + scrollOffset;
            Block block = filteredBlocks.get(index);
            String blockId = Registries.BLOCK.getId(block).getPath();
            String blockName = block.getName().getString();
            int y = listY + i * itemHeight;
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + itemHeight;
            if (hovered) {
                context.fill(listX, y, listX + listWidth, y + itemHeight, 0x40FFFFFF);
            }
            boolean isSelected = selectedBlocks.contains(block);
            if (isSelected) {
                context.drawTextWithShadow(textRenderer, Text.literal("[x]"), listX + 5, y + 5, 0x55FF55);
            } else {
                context.drawTextWithShadow(textRenderer, Text.literal("[ ]"), listX + 5, y + 5, 0xAAAAAA);
            }
            String displayText = blockName + " (" + blockId + ")";
            context.drawTextWithShadow(textRenderer, Text.literal(displayText),
                    listX + 30, y + 5, isSelected ? 0x55FF55 : 0xFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int listX = width / 2 - 150;
        int listY = 55;
        int listWidth = 300;
        int listHeight = visibleItems * itemHeight;
        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY <= listY + listHeight) {
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
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredBlocks.size() - visibleItems);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount));
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
