package com.basefinder.render;

import com.basefinder.BaseFinderClient;
import com.basefinder.module.modules.BaseFinderModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public class WorldRenderEventHandler implements WorldRenderEvents.AfterEntities {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Override
    public void afterEntities(WorldRenderContext context) {
        if (mc.world == null || mc.player == null || mc.options.shouldRenderDebugInfo()) return;
        
        // Получаем модуль BaseFinder (или любой другой, где хранятся найденные блоки)
        if (BaseFinderClient.moduleManager == null || BaseFinderClient.moduleManager.baseFinder == null) return;

        BaseFinderModule module = BaseFinderClient.moduleManager.baseFinder;
        List<BlockPos> foundBlocks = module.getFoundBlocks(); // Предположим, что такой метод есть

        if (foundBlocks.isEmpty()) return;

        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        
        // Настройки рендера
        float red = 1.0f;   // Цвет линии (R)
        float green = 0.0f; // Цвет линии (G)
        float blue = 0.0f;  // Цвет линии (B)
        float alpha = 0.6f; // Прозрачность
        
        // Если хочешь брать цвет из темы, раскомментируй:
        // Color c = new Color(BaseFinderClient.themeManager.getColor());
        // red = c.getRed() / 255f; green = c.getGreen() / 255f; blue = c.getBlue() / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.polygonOffset(-100000, -100000);
        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (BlockPos pos : foundBlocks) {
            // Получаем блок в мире (для проверки, существует ли он еще)
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) continue;

            // Создаем бокс вокруг блока (чуть больше самого блока для красоты)
            Box box = new Box(pos).expand(0.002); 

            double x1 = box.minX - camPos.x;
            double y1 = box.minY - camPos.y;
            double z1 = box.minZ - camPos.z;
            double x2 = box.maxX - camPos.x;
            double y2 = box.maxY - camPos.y;
            double z2 = box.maxZ - camPos.z;

            // Рисуем линии куба (12 линий)
            addEdge(buffer, x1, y1, z1, x2, y1, z1, red, green, blue, alpha); // Низ перед
            addEdge(buffer, x2, y1, z1, x2, y1, z2, red, green, blue, alpha); // Низ право
            addEdge(buffer, x2, y1, z2, x1, y1, z2, red, green, blue, alpha); // Низ зад
            addEdge(buffer, x1, y1, z2, x1, y1, z1, red, green, blue, alpha); // Низ лево

            addEdge(buffer, x1, y2, z1, x2, y2, z1, red, green, blue, alpha); // Верх перед
            addEdge(buffer, x2, y2, z1, x2, y2, z2, red, green, blue, alpha); // Верх право
            addEdge(buffer, x2, y2, z2, x1, y2, z2, red, green, blue, alpha); // Верх зад
            addEdge(buffer, x1, y2, z2, x1, y2, z1, red, green, blue, alpha); // Верх лево

            addEdge(buffer, x1, y1, z1, x1, y2, z1, red, green, blue, alpha); // Лево перед верт
            addEdge(buffer, x2, y1, z1, x2, y2, z1, red, green, blue, alpha); // Право перед верт
            addEdge(buffer, x2, y1, z2, x2, y2, z2, red, green, blue, alpha); // Право зад верт
            addEdge(buffer, x1, y1, z2, x1, y2, z2, red, green, blue, alpha); // Лево зад верт
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

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
