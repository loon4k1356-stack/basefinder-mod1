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

        Tessellator tessellator = Tessellator.getInstance();

        for (ScanResult result : foundBlocks) {
            BlockPos pos = result.getPosition();
            Vec3d blockPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            Vec3d relativePos = blockPos.subtract(cameraPos);

            double distance = relativePos.length();
            if (distance > 500) continue;

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
        float a
