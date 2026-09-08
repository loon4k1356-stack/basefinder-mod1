package com.basefinder;

import com.basefinder.gui.ClickGUI;
import com.basefinder.gui.HudRenderer;
import com.basefinder.module.ModuleManager;
import com.basefinder.module.modules.BaseFinderModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFinderClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("BaseFinder");
    public static ModuleManager moduleManager;
    public static BaseFinderModule baseFinderModule;
    public static HudRenderer hudRenderer;

    private static KeyBinding openGuiKey;

    @Override
    public void onInitialize() {
        LOGGER.info("[freezdlc] Initializing...");

        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        // Если в ModuleManager нет метода registerModule, закомментируй строку ниже
        // moduleManager.registerModule(baseFinderModule); 
        
        hudRenderer = new HudRenderer();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.freezdlc.open_gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.freezdlc"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGUI());
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) {
                hudRenderer.render(drawContext, tickCounter);
            }
        });

        LOGGER.info("[freezdlc] Initialized successfully!");
    }
}
