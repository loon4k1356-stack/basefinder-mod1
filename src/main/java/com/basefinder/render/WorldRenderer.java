package com.basefinder.render;

import com.basefinder.BaseFinderClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public class WorldRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void renderESP(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (BaseFinderClient.scanner == null || !BaseFinderClient.scanner.isRunning()) return;

        List<BlockPos> foundBlocks = BaseFinderClient.scanner.getFoundBlocks(); // Предположим, такой метод есть
        if (foundBlocks.isEmpty()) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        
        // Настройки цвета (можно вынести в настройки)
        Color espColor = new Color(255, 0, 0, 100); // Красный полупрозрачный

        BufferBuilder buffer = vertexConsumers.getBuffer(RenderLayerLines.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (BlockPos pos : foundBlocks) {
            Box box = new Box(pos);
            double x = box.minX - cameraPos.x;
            double y = box.minY - cameraPos.y;
            double z = box.minZ - cameraPos.z;

            drawBox(matrix, buffer, x, y, z, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ, espColor);
        }
    }

    private static void drawBox(Matrix4f matrix, BufferBuilder buffer, double x, double y, double z, double w, double h, double d, Color color) {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // Рисуем линии коробки (упрощенно)
        buffer.vertex(matrix, (float)x, (float)y, (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)y, (float)z).color(r, g, b, a).next();
        
        buffer.vertex(matrix, (float)(x+w), (float)y, (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)y, (float)(z+d)).color(r, g, b, a).next();

        buffer.vertex(matrix, (float)(x+w), (float)y, (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)y, (float)(z+d)).color(r, g, b, a).next();

        buffer.vertex(matrix, (float)x, (float)y, (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)y, (float)z).color(r, g, b, a).next();

        // Вертикальные линии
        buffer.vertex(matrix, (float)x, (float)y, (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)(y+h), (float)z).color(r, g, b, a).next();

        buffer.vertex(matrix, (float)(x+w), (float)y, (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)(y+h), (float)z).color(r, g, b, a).next();

        buffer.vertex(matrix, (float)(x+w), (float)y, (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)(y+h), (float)(z+d)).color(r, g, b, a).next();

        buffer.vertex(matrix, (float)x, (float)y, (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)(y+h), (float)(z+d)).color(r, g, b, a).next();
        
        // Верхняя грань
        buffer.vertex(matrix, (float)x, (float)(y+h), (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)(y+h), (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)(y+h), (float)z).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)(y+h), (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)(x+w), (float)(y+h), (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)(y+h), (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)(y+h), (float)(z+d)).color(r, g, b, a).next();
        buffer.vertex(matrix, (float)x, (float)(y+h), (float)z).color(r, g, b, a).next();
    }
    
    // Вспомогательный класс для типа рендера линий
    private static class RenderLayerLines {
        public static RenderLayer getLines() {
            return RenderLayer.getLines();
        }
    }
}
