package com.lucca.simplylightsmart.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Grants the state-definition mixins access to Block's protected setter. */
@Mixin(Block.class)
public interface BlockDefaultStateInvoker {
    @Invoker("registerDefaultState")
    void simplyLightSmart$registerDefaultState(BlockState state);
}
