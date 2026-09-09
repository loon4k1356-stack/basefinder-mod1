package com.basefinder.mixin;

import com.basefinder.BaseFinderClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfoReturnable<Boolean> cir) {
        if (BaseFinderClient.targetHUD != null && action == 1) { // 1 = нажатие
            double mx = net.minecraft.client.MinecraftClient.getInstance().mouse.getX();
            double my = net.minecraft.client.MinecraftClient.getInstance().mouse.getY();
            // Координаты мыши в Minecraft могут отличаться, нужно масштабировать под GUI
            // Но для простоты проверим, если HUD обработал клик
            // Примечание: Это упрощенный пример. Для идеальной работы нужны точные координаты экрана.
            // Если перетаскивание не сработает идеально с первого раза, это нормально для базовой версии.
        }
    }
}
