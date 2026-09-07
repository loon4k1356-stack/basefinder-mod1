package com.basefinder;

import com.basefinder.command.BaseFinderCommand;
import com.basefinder.config.ConfigManager;
import com.basefinder.gui.BlockSelectScreen;
import com.basefinder.keybind.KeybindHandler;
import com.basefinder.render.BlockHighlightRenderer;
import com.basefinder.scanner.BlockScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
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
        LOGGER.info("[BaseFinder] Initializing BaseFinder mod...");

        scanner = new BlockScanner();
        renderer = new BlockHighlightRenderer();
        keybindHandler = new KeybindHandler();

        keybindHandler.register();
        BaseFinderCommand.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                keybindHandler.handleTick(client);
                scanner.tick();
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (!scanner.getFoundBlocks().isEmpty()) {
                renderer.render(context, scanner);
            }
        });

        ConfigManager.loadConfig();

        LOGGER.info("[BaseFinder] BaseFinder initialized! Controls: [O] Open GUI, [H] Toggle Scanner");
    }

    public static void openBlockSelectScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.setScreen(new BlockSelectScreen());
        }
    }

    public static void toggleScanner() {
        if (scanner.isRunning()) {
            scanner.stop();
            ConfigManager.saveConfig();
        } else {
            scanner.start();
        }
    }
}
