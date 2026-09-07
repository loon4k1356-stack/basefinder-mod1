package com.basefinder;

import com.basefinder.command.BaseFinderCommand;
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

        // Initialize components
        scanner = new BlockScanner();
        renderer = new BlockHighlightRenderer();
        keybindHandler = new KeybindHandler();

        // Register keybinds
        keybindHandler.register();

        // Register commands
        BaseFinderCommand.register();

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                // Handle keybinds
                keybindHandler.handleTick(client);

                // Process scanner
                scanner.tick();
            }
        });

        // Register world render events for block highlighting
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (scanner.isRunning() || !scanner.getFoundBlocks().isEmpty()) {
                renderer.render(context, scanner);
            }
        });

        LOGGER.info("[BaseFinder] BaseFinder initialized successfully!");
        LOGGER.info("[BaseFinder] Controls: [O] Open GUI, [H] Toggle Scanner");
    }

    /**
     * Open the block selection GUI
     */
    public static void openBlockSelectScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.setScreen(new BlockSelectScreen());
        }
    }

    /**
     * Toggle scanner on/off
     */
    public static void toggleScanner() {
        if (scanner.isRunning()) {
            scanner.stop();
        } else {
            scanner.start();
        }
    }
}
