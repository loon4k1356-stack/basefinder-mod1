package com.basefinder.keybind;

import com.basefinder.BaseFinderClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    // Метод регистрации больше не нужен, так как мы регистрируем ключи прямо в BaseFinderClient
    // Но оставляем класс, чтобы не ломать структуру проекта

    public static void handleToggle() {
        if (BaseFinderClient.scanner == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (BaseFinderClient.scanner.isRunning()) {
            BaseFinderClient.scanner.stopScan();
            mc.player.sendMessage(Text.literal("§c[freezdlc] Scanner stopped"), true);
        } else {
            BaseFinderClient.scanner.startScan();
            String mode = BaseFinderClient.scanner.isLiteMode() ? "LITE" : "FULL";
            mc.player.sendMessage(Text.literal("§a[freezdlc] Scanner started (" + mode + ") | Radius: " + BaseFinderClient.scanner.getScanRadius()), true);
        }
    }

    // Заглушка для выбора блоков, если экран еще не создан
    public static void handleBlockSelect() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§e[freezdlc] Block selector coming soon..."), true);
        }
    }
}
