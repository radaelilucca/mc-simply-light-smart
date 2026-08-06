package com.lucca.simplylightsmart.mixin;

import com.flanks255.simplylight.blocks.LightBulb;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only the Lightbulb emission lambda with its smart_power value. */
@Mixin(value = LightBulb.class, remap = false)
abstract class LightBulbMixin {
    @Inject(method = "lambda$new$0", at = @At("HEAD"), cancellable = true)
    private static void simplyLightSmart$readSmartPower(BlockState state, CallbackInfoReturnable<Integer> callback) {
        if (OriginalLightAdapter.hasSmartPower(state)) {
            callback.setReturnValue(state.getValue(OriginalLightAdapter.SMART_POWER));
        }
    }
}
