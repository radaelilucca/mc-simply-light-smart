package com.lucca.simplylightsmart.mixin;

import com.flanks255.simplylight.blocks.LampBlock;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import com.lucca.simplylightsmart.original.DeferredLightWork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LampBlock.class, remap = false)
abstract class LampBlockMixin {
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

    @Inject(method = "lambda$new$1", at = @At("HEAD"), cancellable = true)
    private static void simplyLightSmart$readSmartPower(BlockState state, CallbackInfoReturnable<Integer> callback) {
        if (OriginalLightAdapter.hasSmartPower(state)) {
            callback.setReturnValue(state.getValue(OriginalLightAdapter.SMART_POWER));
        }
    }

    /**
     * LampBlock normally replaces its entire state with a binary on/off state
     * on every neighbour update. That discards smart_power, so smart-enabled
     * Illuminant Blocks must take over this one transition.
     */
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void simplyLightSmart$preserveGraduatedPower(BlockState state, Level level, BlockPos pos,
            Block neighborBlock, BlockPos neighborPos, boolean movedByPiston, CallbackInfo callback) {
        if (OriginalLightAdapter.isLocallyEnabled(state)) {
            if (level instanceof ServerLevel serverLevel) {
                DeferredLightWork.enqueuePositionRefresh(serverLevel, pos);
            }
            callback.cancel();
        }
    }
}
