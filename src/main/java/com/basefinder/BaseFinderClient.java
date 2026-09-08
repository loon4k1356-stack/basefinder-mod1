package com.basefinder;

import com.basefinder.config.ConfigManager;
import com.basefinder.gui.ClickGUI;
import com.basefinder.gui.HudRenderer;
import com.basefinder.keybind.KeybindHandler;
import com.basefinder.module.ModuleManager;
import com.basefinder.module.modules.BaseFinderModule;
import com.basefinder.scanner.BlockScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
package com.basefinder;

import com.basefinder.gui.ClickGUI;
import com.basefinder.gui.HudRenderer;
import com.basefinder.module.ModuleManager;
import com.basefinder.module.modules.BaseFinderModule;
import com.basefinder.scanner.BlockScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BaseFinderClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("freezdlc");
    public static ModuleManager moduleManager;
    public static BaseFinderModule baseFinderModule;
    public static BlockScanner scanner;
    public static HudRenderer hudRenderer;

    private static KeyBinding openGuiKey;
    private static KeyBinding toggleScannerKey;

    @Override
    public void onInitialize() {
        LOGGER.info("[freezdlc] Initializing...");

        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);
        
        scanner = new BlockScanner();
        hudRenderer = new HudRenderer();

        // Клавиши
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.freezdlc.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, "category.freezdlc"));
        toggleScannerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.freezdlc.toggle", GLFW.GLFW_KEY_H, "category.freezdlc"));

        // Тик события
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGUI());
            }
            while (toggleScannerKey.wasPressed()) {
                toggleScanner();
            }
        });

        // HUD
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) hudRenderer.render(drawContext, tickCounter);
        });

        // 3D ESP (Упрощенный)
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderESP);

        LOGGER.info("[freezdlc] Initialized successfully!");
    }

    private void renderESP(WorldRenderContext context) {
        if (scanner == null || scanner.getSelectedBlocks().isEmpty()) return;
        
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider vertexConsumers = context.consumers();
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getLines());
        float r = 1.0f, g = 0.0f, b = 0.0f, a = 0.6f;

        for (BlockPos pos : scanner.getSelectedBlocks()) {
            Box box = new Box(pos).expand(0.002);
            double x1 = box.minX - camPos.x;
            double y1 = box.minY - camPos.y;
            double z1 = box.minZ - camPos.z;
            double x2 = box.maxX - camPos.x;
            double y2 = box.maxY - camPos.y;
            double z2 = box.maxZ - camPos.z;

            // Рисуем куб (упрощенно)
            drawBox(buffer, matrices, x1, y1, z1, x2, y2, z2, r, g, b, a);
        }
    }

    private void drawBox(VertexConsumer buffer, MatrixStack matrices, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        // Вершины куба
        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y1, (float)z1).color(r, g, b, a).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y1, (float)z1).color(r, g, b, a).next();
        
        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y1, (float)z1).color(r, g, b, a).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y1, (float)z2).color(r, g, b, a).next();

        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y1, (float)z2).color(r, g, b, a).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y1, (float)z2).color(r, g, b, a).next();

        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y1, (float)z2).color(r, g, b, a).next();
        buffer.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y1, (float)z1).color(r, g, b, a).next();
        
        // Верх и низ можно добавить аналогично, пока только основание для теста
    }

    // Методы-заглушки и помощники
    public static void toggleScanner() {
        if (scanner == null) return;
        if (scanner.isRunning()) {
            scanner.stopScan();
            MinecraftClient.getInstance().player.sendMessage(Text.literal("§c[freezdlc] Scanner stopped"), true);
        } else {
            scanner.startScan();
            MinecraftClient.getInstance().player.sendMessage(Text.literal("§a[freezdlc] Scanner started"), true);
        }
    }
    
    // Заглушки для удаленных классов, чтобы не было ошибок компиляции если где-то остались ссылки
    public static void openBlockSelectScreen() {} 
}

public class BaseFinderClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("BaseFinder");
    public static ModuleManager moduleManager;
    public static BaseFinderModule baseFinderModule;
    public static BlockScanner scanner;
    public static HudRenderer hudRenderer;

    private static KeyBinding openGuiKey;

    @Override
    public void onInitialize() {
        LOGGER.info("[freezdlc] Initializing client...");

        // Инициализация
        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);
        
        scanner = new BlockScanner();
        hudRenderer = new HudRenderer();

        // Клавиша GUI (Right Shift)
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.freezdlc.open_gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.freezdlc"
        ));

        // Обработка нажатия
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGUI());
                }
            }
        });

        // Регистрация HUD
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) hudRenderer.render(drawContext, tickCounter);
        });

        // Регистрация 3D ESP (вызываем метод из сканера)
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (scanner != null) scanner.renderESP(context);
        });

        // Регистрируем ключи
        KeybindHandler.register();

        LOGGER.info("[freezdlc] Initialization complete!");
    }
}
