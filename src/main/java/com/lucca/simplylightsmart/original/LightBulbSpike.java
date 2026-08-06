package com.lucca.simplylightsmart.original;

import com.flanks255.simplylight.blocks.LightBulb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Phase 0 viability spike for one original Simply Light Lightbulb family.
 * Other original families intentionally remain outside this implementation.
 */
public final class LightBulbSpike {
    public static final IntegerProperty SMART_POWER = IntegerProperty.create("smart_power", 0, 15);

    private LightBulbSpike() {
    }

    public static boolean supports(BlockState state) {
        return state.getBlock() instanceof LightBulb && state.hasProperty(SMART_POWER);
    }

    public static void refreshFromRedstone(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!supports(state)) {
            return;
        }

        int emission = 15 - Math.clamp(level.getBestNeighborSignal(pos), 0, 15);
        if (state.getValue(SMART_POWER) != emission) {
            level.setBlockAndUpdate(pos, state.setValue(SMART_POWER, emission));
        }
    }
}
