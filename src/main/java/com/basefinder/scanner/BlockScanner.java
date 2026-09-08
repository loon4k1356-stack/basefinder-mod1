package com.basefinder.scanner;

import com.basefinder.BaseFinderClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class BlockScanner {

    private final List<BlockPos> selectedBlocks = new ArrayList<>();
    private boolean running = false;

    public List<BlockPos> getSelectedBlocks() {
        return selectedBlocks;
    }

    public void addSelectedBlock(BlockPos pos) {
        if (!selectedBlocks.contains(pos)) selectedBlocks.add(pos);
    }

    public void removeSelectedBlock(BlockPos pos) {
        selectedBlocks.remove(pos);
    }

    public void clearSelectedBlocks() {
        selectedBlocks.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public void toggleScan() {
        running = !running;
        if (running) {
            System.out.println("[freezdlc] Scan started!");
        } else {
            System.out.println("[freezdlc] Scan stopped!");
        }
    }

    // Метод для отрисовки ESP, вызываемый из BaseFinderClient
    public void renderESP(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || selectedBlocks.isEmpty()) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float r = 1.0f, g = 0.0f, b = 0.0f, a = 0.6f;

        for (BlockPos pos : selectedBlocks) {
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

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionProgram);
    }

    private void addEdge(BufferBuilder builder, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        builder.vertex(x1, y1, z1).color(r, g, b, a);
        builder.vertex(x2, y2, z2).color(r, g, b, a);
    }
}
