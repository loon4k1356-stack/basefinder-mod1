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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        // Инициализация менеджеров
        moduleManager = new ModuleManager();
        baseFinderModule = new BaseFinderModule();
        moduleManager.registerModule(baseFinderModule);
        
        // Инициализация сканера
        scanner = new BlockScanner();

        // Инициализация HUD
        hudRenderer = new HudRenderer();

        // Регистрация клавиши открытия GUI (Правый Shift)
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

        // Регистрация HUD
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (hudRenderer != null) {
                hudRenderer.render(drawContext, tickCounter);
            }
        });

        // Регистрация обработчика клавиш (если есть статический метод)
        // Если KeybindHandler требует экземпляр, закомментируй эту строку
        try {
            KeybindHandler.register(); 
        } catch (Exception e) {
            LOGGER.warn("KeybindHandler registration skipped or failed.");
        }

        LOGGER.info("[freezdlc] Initialization complete!");
    }

    // Методы для доступа из других классов
    public static void toggleScanner() {
        if (scanner == null) return;
        if (scanner.isRunning()) {
            scanner.stopScan();
        } else {
            scanner.startScan();
        }
    }

    public static void openBlockSelectScreen() {
        MinecraftClient.getInstance().setScreen(new ClickGUI()); // Или отдельный экран, если был
        // Переключаем вкладку на блоки, если нужно
    }
}
