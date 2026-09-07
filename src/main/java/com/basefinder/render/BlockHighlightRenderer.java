package com.basefinder.render;

import com.basefinder.scanner.BlockScanner;
import com.basefinder.scanner.ScanResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

/**
 * Renders block highlights and tracers for found blocks.
 */
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

        Tessellator tessellator = Tessellator.getInstance();

        // Render for each found block
        for (ScanResult result : foundBlocks) {
            BlockPos pos = result.getPosition();
            Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d relativePos = blockPos.subtract(cameraPos);

            // Distance culling
            double distance = relativePos.length();
            if (distance > 500) continue;

            // Get color based on block type
            Color color = getColorForBlock(result);

            if (showBoxes) {
                renderBox(matrices, tessellator, relativePos, color);
            }

            if (showTracers) {
                renderTracer(matrices, tessellator, relativePos, color);
            }
        }
    }

    private void renderBox(MatrixStack matrices, Tessellator tessellator, Vec3d pos, Color color) {
        matrices.push();
        matrices.translate(pos.x, pos.y, pos.z);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = boxAlpha;

        // Draw filled box faces
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, a);
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, a);

        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a);

        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, a);

        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a);
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, a);
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a);

        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a);
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, a);
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, a);

        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, a);
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, a);
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Draw outline
        BufferBuilder outlineBuffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        float oa = 1.0f;

        outlineBuffer.vertex(matrix, 0, 0, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 0, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 0, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 0, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 0, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 0, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 0, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 0, 0).color(r, g, b, oa);

        outlineBuffer.vertex(matrix, 0, 1, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 1, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 1, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 1, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 1, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 1, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 1, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 1, 0).color(r, g, b, oa);

        outlineBuffer.vertex(matrix, 0, 0, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 1, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 0, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 1, 0).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 0, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 1, 1, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 0, 1).color(r, g, b, oa);
        outlineBuffer.vertex(matrix, 0, 1, 1).color(r, g, b, oa);

        BufferRenderer.drawWithGlobalProgram(outlineBuffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void renderTracer(MatrixStack matrices, Tessellator tessellator, Vec3d blockPos, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;

        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, 0.0f);
        buffer.vertex(matrix, (float) blockPos.x + 0.5f, (float) blockPos.y + 0.5f, (float) blockPos.z + 0.5f)
                .color(r, g, b, tracerAlpha);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
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
        if (blockId.contains("ender_chest")) return new Color(128, 0, 255);
        if (blockId.contains("beacon")) return new Color(255, 255, 128);

        return new Color(255, 165, 0);
    }

    public boolean isShowTracers() { return showTracers; }
    public void setShowTracers(boolean show) { this.showTracers = show; }
    public boolean isShowBoxes() { return showBoxes; }
    public void setShowBoxes(boolean show) { this.showBoxes = show; }
    public float getBoxAlpha() { return boxAlpha; }
    public void setBoxAlpha(float alpha) { this.boxAlpha = alpha; }
}
package com.basefinder.render;

import com.basefinder.scanner.BlockScanner;
import com.basefinder.scanner.ScanResult;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
