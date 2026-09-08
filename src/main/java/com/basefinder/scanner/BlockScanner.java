package com.basefinder.scanner;

import com.basefinder.BaseFinderClient;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlockScanner {

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Set<Block> selectedBlocks = new HashSet<>();
    private final List<BlockPos> foundBlocks = new ArrayList<>();
    
    private boolean running = false;
    private int scanRadius = 64;
    private boolean liteMode = false;
    private int liteHeightLimit = 30;

    // Методы для ConfigManager и KeybindHandler
    public void setScanRadius(int radius) { this.scanRadius = radius; }
    public int getScanRadius() { return scanRadius; }
    public void setLiteMode(boolean lite) { this.liteMode = lite; }
    public boolean isLiteMode() { return liteMode; }
    public void setLiteHeightLimit(int limit) { this.liteHeightLimit = limit; }
    public int getLiteHeightLimit() { return liteHeightLimit; }
    
    public Set<Block> getSelectedBlocks() { return selectedBlocks; }
    public void addSelectedBlock(Block block) { selectedBlocks.add(block); }
    public void removeSelectedBlock(Block block) { selectedBlocks.remove(block); }
    public void clearSelectedBlocks() { selectedBlocks.clear(); }
    
    public List<BlockPos> getFoundBlocks() { return foundBlocks; }
    public boolean isRunning() { return running; }

    public void startScan() {
        if (running) return;
        running = true;
        foundBlocks.clear();
        BaseFinderClient.LOGGER.info("[BaseFinder] Scan started!");
        // Здесь должна быть логика сканирования в фоне
        // Для примера просто найдем блоки вокруг игрока один раз
        scanOnce(); 
    }

    public void stopScan() {
        running = false;
        BaseFinderClient.LOGGER.info("[BaseFinder] Scan stopped.");
    }

    private void scanOnce() {
        if (mc.world == null || mc.player == null) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        int r = scanRadius;
        
        // Простой скан по радиусу
        for (int x = -r; x <= r; x++) {
            for (int y = (liteMode ? -liteHeightLimit : -r); y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    
                    if (selectedBlocks.contains(block) && !block.equals(Blocks.AIR)) {
                        if (!foundBlocks.contains(pos)) {
                            foundBlocks.add(pos);
                        }
                    }
                }
            }
        }
        BaseFinderClient.LOGGER.info("[BaseFinder] Found {} blocks.", foundBlocks.size());
    }

    // Метод отрисовки, который мы вызываем из BaseFinderClient
    public void renderESP(WorldRenderContext context) {
        if (foundBlocks.isEmpty() || mc.world == null) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.polygonOffset(-100000, -100000);
        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = 1.0f, g = 0.2f, b = 0.0f, a = 0.7f;

        for (BlockPos pos : foundBlocks) {
            Box box = new Box(pos).expand(0.002);
            double x1 = box.minX - camPos.x;
            double y1 = box.minY - camPos.y;
            double z1 = box.minZ - camPos.z;
            double x2 = box.maxX - camPos.x;
            double y2 = box.maxY - camPos.y;
            double z2 = box.maxZ - camPos.z;

            // Рисуем куб (упрощенно)
            addEdge(buffer, x1, y1, z1, x2, y1, z1, r, g, b, a);
            addEdge(buffer, x2, y1, z1, x2, y1, z2, r, g, b, a);
            addEdge(buffer, x2, y1, z2, x1, y1, z2, r, g, b, a);
            addEdge(buffer, x1, y1, z2, x1, y1, z1, r, g, b, a);
            addEdge(buffer, x1, y2, z1, x2, y2, z1, r, g, b, a);
            addEdge(buffer, x2, y2, z1, x2, y2, z2, r, g, b, a);
            addEdge(buffer, x2, y2, z2, x1, y2, z2, r, g, b, a);
            addEdge(buffer, x1, y2, z2, x1, y2, z1, r, g, b, a);
            addEdge(buffer, x1, y1, z1, x1, y2, z1, r, g, b, a);
            addEdge(buffer, x2, y1, z1, x2, y2, z1, r, g, b, a);
            addEdge(buffer, x2, y1, z2, x2, y2, z2, r, g, b, a);
            addEdge(buffer, x1, y1, z2, x1, y2, z2, r, g, b, a);
        }

        try {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } catch (Exception ignored) {}

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.polygonOffset(0, 0);
        RenderSystem.disablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
    }

    private void addEdge(BufferBuilder builder, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        builder.vertex(x1, y1, z1).color(r, g, b, a);
        builder.vertex(x2, y2, z2).color(r, g, b, a);
    }
}
