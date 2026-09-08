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
        LOGGER.info("[freezdlc] Initializing version 5.0...");

        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);
        
        scanner = new BlockScanner();
        hudRenderer = new HudRenderer();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.freezdlc.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, "category.freezdlc"));
        toggleScannerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.freezdlc.toggle", GLFW.GLFW_KEY_H, "category.freezdlc"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGUI());
            }
            while (toggleScannerKey.wasPressed()) {
                toggleScanner();
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) hudRenderer.render(drawContext, tickCounter);
        });

        WorldRenderEvents.AFTER_ENTITIES.register(this::renderESP);

        LOGGER.info("[freezdlc] Initialization complete!");
    }

    private void renderESP(WorldRenderContext context) {
        if (scanner == null) return;
        // Простая отрисовка, если нужно будет усложнить - сделаем в след. версии
    }

    public static void toggleScanner() {
        if (scanner == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (scanner.isRunning()) {
            scanner.stopScan();
            mc.player.sendMessage(Text.literal("§c[freezdlc] Scanner stopped"), true);
        } else {
            scanner.startScan();
            mc.player.sendMessage(Text.literal("§a[freezdlc] Scanner started"), true);
        }
    }

    // Заглушки для совместимости
    public static void openBlockSelectScreen() {}
}
