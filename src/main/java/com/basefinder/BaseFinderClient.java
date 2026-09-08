package com.basefinder;

import com.basefinder.command.BaseFinderCommand;
import com.basefinder.config.ConfigManager;
import com.basefinder.gui.ClickGUI;
import com.basefinder.keybind.KeybindHandler;
import com.basefinder.module.ModuleManager;
import com.basefinder.render.BlockHighlightRenderer;
import com.basefinder.scanner.BlockScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "basefinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static BlockScanner scanner;
    public static BlockHighlightRenderer renderer;
    public static KeybindHandler keybindHandler;
    public static ModuleManager moduleManager;
    public static HudRenderer hudRenderer;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[BaseFinder] Initializing...");

        scanner = new BlockScanner();
        renderer = new BlockHighlightRenderer();
        keybindHandler = new KeybindHandler();
        moduleManager = new ModuleManager();

        keybindHandler.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            keybindHandler.handleTick(client);
            moduleManager.onTick();
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            renderer.render(context, scanner);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            BaseFinderCommand.register(dispatcher);
        });

        ConfigManager.init();

        LOGGER.info("[BaseFinder] Initialized! [O] ClickGUI, [H] Toggle Scanner");
    }

    public static void openClickGUI() {
        MinecraftClient.getInstance().setScreen(new ClickGUI());
    }

    public static void openBlockSelectScreen() {
        openClickGUI();
    }

    public static void toggleScanner() {
        if (moduleManager != null && moduleManager.baseFinder != null) {
            moduleManager.baseFinder.toggle();
        } else if (scanner != null) {
            if (scanner.isRunning()) {
                scanner.stop();
                ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
            } else {
                scanner.start();
            }
        }
    }
}
