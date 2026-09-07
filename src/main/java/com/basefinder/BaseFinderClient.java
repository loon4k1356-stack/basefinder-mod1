package com.basefinder;

import com.basefinder.command.BaseFinderCommand;
import com.basefinder.config.ConfigManager;
import com.basefinder.gui.BlockSelectScreen;
import com.basefinder.keybind.KeybindHandler;
import com.basefinder.render.BlockHighlightRenderer;
import com.basefinder.scanner.BlockScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFinderClient implements ClientModInitializer {
    public static final String MOD_ID = "basefinder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static BlockScanner scanner;
    public static BlockHighlightRenderer renderer;
    public static KeybindHandler keybindHandler;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[BaseFinder] Initializing...");

        scanner = new BlockScanner();
        renderer = new BlockHighlightRenderer();
        keybindHandler = new KeybindHandler();

        keybindHandler.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            keybindHandler.handleTick(client);
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            renderer.render(context, scanner);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            BaseFinderCommand.register(dispatcher);
        });

        ConfigManager.init();

        LOGGER.info("[BaseFinder] Initialized! [O] GUI, [H] Toggle Scanner");
    }

    public static void openBlockSelectScreen() {
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        client.setScreen(new BlockSelectScreen());
    }

    public static void toggleScanner() {
        if (scanner.isRunning()) {
            scanner.stop();
            ConfigManager.saveConfig(ConfigManager.getCurrentConfigName());
        } else {
            scanner.start();
        }
    }
}
