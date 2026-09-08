package com.basefinder;

import com.basefinder.config.ConfigManager;
import com.basefinder.gui.ClickGUI;
import com.basefinder.gui.HudRenderer;
import com.basefinder.module.ModuleManager;
import com.basefinder.module.modules.BaseFinderModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BaseFinderClient implements ClientModInitializer {

    public static ModuleManager moduleManager;
    public static BaseFinderModule baseFinderModule;
    public static Scanner scanner;
    public static HudRenderer hudRenderer;

    private static KeyBinding openGuiKey;

    @Override
    public void onInitialize() {
        System.out.println("[freezdlc] Initializing client...");

        // Инициализация менеджеров
        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);
        
        // Инициализация сканера
        scanner = new Scanner();

        // Инициализация HUD
        hudRenderer = new HudRenderer();

        // Регистрация клавиши открытия GUI (Правый Shift по умолчанию)
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.freezdlc.open_gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.freezdlc"
        ));

        // Обработка нажатия клавиши
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGUI());
                }
            }
        });

        // --- РЕГИСТРАЦИЯ HUD (Интерфейс поверх игры) ---
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) {
                hudRenderer.render(drawContext, tickCounter);
            }
        });

        // --- РЕГИСТРАЦИЯ 3D ESP (Отрисовка блоков в мире) ---
        WorldRenderEvents.AFTER_ENTITIES.register((context) -> {
            if (MinecraftClient.getInstance().world == null || MinecraftClient.getInstance().player == null) return;
            
            // Получаем список блоков для отрисовки из сканера
            if (scanner == null) return;
            List<BlockPos> blocksToRender = scanner.getSelectedBlocks();
            
            if (blocksToRender == null || blocksToRender.isEmpty()) return;

            Camera camera = context.camera();
            Vec3d camPos = camera.getPos();
            
            // Цвет боксов (Ярко-красный)
            float r = 1.0f;
            float g = 0.0f;
            float b = 0.0f;
            float alpha = 0.7f;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(MinecraftClient.IS_SYSTEM_MAC);
            RenderSystem.polygonOffset(-100000, -100000);
            RenderSystem.enablePolygonOffset();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (BlockPos pos : blocksToRender) {
                BlockState state = MinecraftClient.getInstance().world.getBlockState(pos);
                if (state.isAir()) continue;

                Box box = new Box(pos).expand(0.002); 

                double x1 = box.minX - camPos.x;
                double y1 = box.minY - camPos.y;
                double z1 = box.minZ - camPos.z;
                double x2 = box.maxX - camPos.x;
                double y2 = box.maxY - camPos.y;
                double z2 = box.maxZ - camPos.z;

                // Рисуем линии куба (верх и низ)
                addEdge(buffer, x1, y1, z1, x2, y1, z1, r, g, b, alpha);
                addEdge(buffer, x2, y1, z1, x2, y1, z2, r, g, b, alpha);
                addEdge(buffer, x2, y1, z2, x1, y1, z2, r, g, b, alpha);
                addEdge(buffer, x1, y1, z2, x1, y1, z1, r, g, b, alpha);

                addEdge(buffer, x1, y2, z1, x2, y2, z1, r, g, b, alpha);
                addEdge(buffer, x2, y2, z1, x2, y2, z2, r, g, b, alpha);
                addEdge(buffer, x2, y2, z2, x1, y2, z2, r, g, b, alpha);
                addEdge(buffer, x1, y2, z2, x1, y2, z1, r, g, b, alpha);

                // Рисуем вертикальные линии
                addEdge(buffer, x1, y1, z1, x1, y2, z1, r, g, b, alpha);
                addEdge(buffer, x2, y1, z1, x2, y2, z1, r, g, b, alpha);
                addEdge(buffer, x2, y1, z2, x2, y2, z2, r, g, b, alpha);
                addEdge(buffer, x1, y1, z2, x1, y2, z2, r, g, b, alpha);
            }

            try {
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            } catch (Exception e) {
                // Тихий игнор ошибок буфера
            }

            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.polygonOffset(0, 0);
            RenderSystem.disablePolygonOffset();
            RenderSystem.setShader(GameRenderer::getPositionProgram);
        });

        System.out.println("[freezdlc] Initialization complete! Ready to find bases.");
    }

    // Вспомогательный метод для рисования линий
    private void addEdge(BufferBuilder builder, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        builder.vertex(x1, y1, z1).color(r, g, b, a);
        builder.vertex(x2, y2, z2).color(r, g, b, a);
    }
}
