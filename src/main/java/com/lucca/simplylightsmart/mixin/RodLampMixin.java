package com.lucca.simplylightsmart.mixin;

import com.flanks255.simplylight.blocks.RodLamp;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RodLamp.class, remap = false)
abstract class RodLampMixin {
    @Inject(method = "createBlockStateDefinition", at = @At("TAIL"))
    private void simplyLightSmart$addSmartPower(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo callback) {
        builder.add(OriginalLightAdapter.SMART_POWER, OriginalLightAdapter.SMART_MODE);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void simplyLightSmart$setNativeSmartDefaults(CallbackInfo callback) {
        Block block = (Block) (Object) this;
        ((BlockDefaultStateInvoker) block).simplyLightSmart$registerDefaultState(
                OriginalLightAdapter.applyNativeDefaults(block.defaultBlockState()));
    }

    @Inject(method = "lambda$new$0", at = @At("HEAD"), cancellable = true)
    private static void simplyLightSmart$readSmartPower(BlockState state, CallbackInfoReturnable<Integer> callback) {
        if (OriginalLightAdapter.hasSmartPower(state)) {
            callback.setReturnValue(state.getValue(OriginalLightAdapter.SMART_POWER));
        }
    }
}
