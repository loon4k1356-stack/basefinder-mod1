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
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class BaseFinderClient implements ClientModInitializer {

    public static ModuleManager moduleManager;
    public static BaseFinderModule baseFinderModule;
    public static BlockScanner scanner;
    public static HudRenderer hudRenderer;
    
    private static KeyBinding openGuiKey;

    @Override
    public void onInitialize() {
        System.out.println("[freezdlc] Initializing...");

        // 1. Инициализация менеджеров
        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);

        // 2. Инициализация сканера
        scanner = new BlockScanner();

        // 3. Инициализация HUD
        hudRenderer = new HudRenderer();

        // 4. Регистрация клавиши (Правый Shift)
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.freezdlc.open_gui",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.freezdlc"
        ));

        // 5. Обработка нажатия (Открытие GUI)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClickGUI());
                }
            }
        });

        // 6. Рендеринг HUD (Интерфейс поверх игры)
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) {
                hudRenderer.render(drawContext, tickCounter);
            }
        });

        // 7. Простой рендеринг ESP (Блоки в мире)
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (scanner != null && !scanner.getSelectedBlocks().isEmpty()) {
                scanner.renderESP(context); // Метод внутри сканера
            }
        });

        System.out.println("[freezdlc] Initialization complete! Press Right Shift for GUI.");
    }
}
