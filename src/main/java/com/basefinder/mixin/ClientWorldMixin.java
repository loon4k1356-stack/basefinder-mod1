package com.basefinder.mixin;

import com.basefinder.BaseFinderClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {

    @Inject(method = "updateBlock", at = @At("HEAD"))
    private void onUpdateBlock(BlockPos pos, BlockState state, CallbackInfo ci) {
        // Временно отключено до полной реализации AntiXRayBypass
        /* 
        if (BaseFinderClient.scanner != null && BaseFinderClient.scanner.getAntiXRayBypass() != null) {
            BaseFinderClient.scanner.getAntiXRayBypass().recordBlockUpdate(pos, state);
        }
        */
    }
}
