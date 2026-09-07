package com.basefinder.mixin;

import com.basefinder.BaseFinderClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {

    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void onBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        if (BaseFinderClient.scanner != null && BaseFinderClient.scanner.getAntiXRayBypass() != null) {
            BaseFinderClient.scanner.getAntiXRayBypass().recordBlockUpdate(pos, state);
        }
    }
}
