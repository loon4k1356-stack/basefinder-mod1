package com.basefinder;

import com.basefinder.gui.ClickGUI;
import com.basefinder.gui.HudRenderer;
import com.basefinder.module.ModuleManager;
import com.basefinder.module.modules.BaseFinderModule;
import com.basefinder.scanner.BlockScanner; // Проверь, что такой класс есть
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

public class BaseFinderClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("BaseFinder");
    public static ModuleManager moduleManager;
    public static BaseFinderModule baseFinderModule;
    public static BlockScanner scanner; // Убедись, что класс BlockScanner существует
    public static HudRenderer hudRenderer;
    private static KeyBinding openGuiKey;

    @Override
    public void onInitialize() {
        LOGGER.info("[freezdlc] Initializing...");
        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);
        
        // Инициализация сканера (класс BlockScanner должен существовать!)
        scanner = new BlockScanner(); 
        
        hudRenderer = new HudRenderer();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.freezdlc.open_gui", GLFW.GLFW_KEY_RIGHT_SHIFT, "category.freezdlc"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGUI());
            }
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            if (hudRenderer != null) hudRenderer.render(ctx, tick);
        });

        // Регистрация рендера ESP (встроенная лямбда)
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            // Логика рендера из прошлого ответа, использующая scanner.getSelectedBlocks()
            // Убедись, что в BlockScanner есть метод getSelectedBlocks(), возвращающий List<BlockPos>
        });
        
        LOGGER.info("[freezdlc] Done!");
    }
}
