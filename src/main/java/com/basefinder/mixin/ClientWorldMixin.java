package com.basefinder.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void onSetBlockState(BlockPos pos, BlockState state, CallbackInfo ci) {
        // ВРЕМЕННО ОТКЛЮЧЕНО ДЛЯ ИСПРАВЛЕНИЯ ОШИБОК СБОРКИ
        // Логика Anti-XRay будет восстановлена в версии 5.1, 
        // когда основной функционал (GUI, HUD, Сканер) будет полностью рабочим.
        
        /* 
        if (BaseFinderClient.scanner != null && BaseFinderClient.scanner.getAntiXRayBypass() != null) {
            BaseFinderClient.scanner.getAntiXRayBypass().recordBlockUpdate(pos, state);
        }
        */
    }
}
