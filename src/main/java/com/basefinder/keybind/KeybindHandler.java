package com.basefinder.keybind;

import com.basefinder.BaseFinderClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    private KeyBinding openGuiKey;
    private KeyBinding toggleScannerKey;

    private boolean guiKeyWasPressed = false;
    private boolean scannerKeyWasPressed = false;

    public void register() {
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.basefinder.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.basefinder"
        ));

        toggleScannerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.basefinder.toggle_scanner",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.basefinder"
        ));

        BaseFinderClient.LOGGER.info("[BaseFinder] Keybinds registered: [O] GUI, [H] Toggle Scanner");
    }

    public void handleTick(MinecraftClient client) {
        if (client.player == null) return;

        boolean guiKeyPressed = openGuiKey.isPressed();
        if (guiKeyPressed && !guiKeyWasPressed) {
            if (client.currentScreen == null) {
                BaseFinderClient.openBlockSelectScreen();
            }
        }
        guiKeyWasPressed = guiKeyPressed;

        boolean scannerKeyPressed = toggleScannerKey.isPressed();
        if (scannerKeyPressed && !scannerKeyWasPressed) {
            if (client.currentScreen == null) {
                BaseFinderClient.toggleScanner();
                if (BaseFinderClient.scanner.isRunning()) {
                    String mode = BaseFinderClient.scanner.isLiteMode() ? "LITE" : "FULL";
                    client.player.sendMessage(
                            Text.literal("§a[BaseFinder] Сканер запущен (" + mode + ")")
                                    .append(Text.literal(" | Радиус: " + BaseFinderClient.scanner.getScanRadius()))
                                    .append(Text.literal(" | Блоков: " + BaseFinderClient.scanner.getSelectedBlocks().size())),
                            true
                    );
                } else {
                    client.player.sendMessage(
                            Text.literal("§c[BaseFinder] Сканер остановлен. Найдено: " +
                                    BaseFinderClient.scanner.getFoundBlocks().size() + " блоков"),
                            true
                    );
                }
            }
        }
        scannerKeyWasPressed = scannerKeyPressed;
    }
}
