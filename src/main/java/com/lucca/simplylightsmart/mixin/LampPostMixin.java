package com.lucca.simplylightsmart.mixin;

import com.flanks255.simplylight.blocks.LampPost;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LampPost.class, remap = false)
abstract class LampPostMixin {
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

    @Inject(method = "lambda$new$3", at = @At("HEAD"), cancellable = true)
    private static void simplyLightSmart$readSmartPower(BlockState state, CallbackInfoReturnable<Integer> callback) {
        if (OriginalLightAdapter.hasSmartPower(state)) {
            callback.setReturnValue(state.getValue(LampPost.POSITION) == LampPost.Position.TOP
                    ? state.getValue(OriginalLightAdapter.SMART_POWER)
                    : 0);
        }
    }

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void simplyLightSmart$syncPlacedColumn(Level level, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack, CallbackInfo callback) {
        OriginalLightAdapter.refreshLocal(level, pos);
    }
}
