package com.lucca.simplylightsmart.mixin;

import com.flanks255.simplylight.blocks.RotatableLamp;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LightBulb inherits its state definition from RotatableLamp. The property is
 * therefore present on its inherited state; only LightBulb reads it in phase 0.
 */
@Mixin(value = RotatableLamp.class, remap = false)
abstract class RotatableLampStateMixin {
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
}
