package com.basefinder.render;

import com.basefinder.scanner.BlockScanner;
import com.basefinder.scanner.ScanResult;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.List;

public class BlockHighlightRenderer {

    private boolean showTracers = true;
    private boolean showBoxes = true;
    private float boxAlpha = 0.4f;
    private float tracerAlpha = 0.7f;

    public void render(WorldRenderContext context, BlockScanner scanner) {
        if (scanner == null) return;

        List<ScanResult> foundBlocks = scanner.getFoundBlocks();
        if (foundBlocks.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();

        for (ScanResult result : foundBlocks) {
            BlockPos pos = result.getPosition();
            Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d relativePos = blockPos.subtract(cameraPos);

            double distance = relativePos.length();
            if (distance > 500) continue;

            Color color = getColorForBlock(result);

            if (showBoxes) {
                renderBox(matrices, relativePos, color);
            }
        }
    }

    private void renderBox(MatrixStack matrices, Vec3d pos, Color color) {
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = boxAlpha;

        VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders();
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        // Draw outline edges
        drawLine(lines, matrix, 0, 0, 0, 1, 0, 0, r, g, b, 1.0f);
        drawLine(lines, matrix, 1, 0, 0, 1, 0, 1, r, g, b, 1.0f);
        drawLine(lines, matrix, 1, 0, 1, 0, 0, 1, r, g, b, 1.0f);
        drawLine(lines, matrix, 0, 0, 1, 0, 0, 0, r, g, b, 1.0f);

        drawLine(lines, matrix, 0, 1, 0, 1, 1, 0, r, g, b, 1.0f);
        drawLine(lines, matrix, 1, 1, 0, 1, 1, 1, r, g, b, 1.0f);
        drawLine(lines, matrix, 1, 1, 1, 0, 1, 1, r, g, b, 1.0f);
        drawLine(lines, matrix, 0, 1, 1, 0, 1, 0, r, g, b, 1.0f);

        drawLine(lines, matrix, 0, 0, 0, 0, 1, 0, r, g, b, 1.0f);
        drawLine(lines, matrix, 1, 0, 0, 1, 1, 0, r, g, b, 1.0f);
        drawLine(lines, matrix, 1, 0, 1, 1, 1, 1, r, g, b, 1.0f);
        drawLine(lines, matrix, 0, 0, 1, 0, 1, 1, r, g, b, 1.0f);

        matrices.pop();
    }

    private void drawLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) { nx /= length; ny /= length; nz /= length; }

        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(nx, ny, nz);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(nx, ny, nz);
    }

    private Color getColorForBlock(ScanResult result) {
        String blockId = net.minecraft.registry.Registries.BLOCK.getId(result.getBlock()).getPath();
        if (blockId.contains("diamond")) return new Color(0, 255, 255);
        if (blockId.contains("emerald")) return new Color(0, 255, 0);
        if (blockId.contains("gold")) return new Color(255, 215, 0);
        if (blockId.contains("iron")) return new Color(220, 220, 220);
        if (blockId.contains("redstone")) return new Color(255, 0, 0);
        if (blockId.contains("lapis")) return new Color(0, 0, 255);
        if (blockId.contains("coal")) return new Color(50, 50, 50);
        if (blockId.contains("copper")) return new Color(184, 115, 51);
        if (blockId.contains("netherite") || blockId.contains("ancient_debris")) return new Color(60, 30, 30);
        if (blockId.contains("obsidian")) return new Color(30, 0, 50);
        if (blockId.contains("spawner")) return new Color(0, 255, 128);
        return new Color(255, 165, 0);
    }

    public boolean isShowTracers() { return showTracers; }
    public void setShowTracers(boolean show) { this.showTracers = show; }
    public boolean isShowBoxes() { return showBoxes; }
    public void setShowBoxes(boolean show) { this.showBoxes = show; }
    public float getBoxAlpha() { return boxAlpha; }
    public void setBoxAlpha(float alpha) { this.boxAlpha = alpha; }
}
