package com.lucca.simplylightsmart.block;

import com.flanks255.simplylight.blocks.RotatableLamp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Shared redstone dimmer behavior for the addon’s rotatable light blocks. */
public abstract class AbstractDimmerRotatableLamp extends RotatableLamp implements DimmerLight {
    public static final IntegerProperty POWER = DimmerLight.POWER;

    private final int maximumLightLevel;
    private final boolean inverted;

    protected AbstractDimmerRotatableLamp(BlockBehaviour.Properties properties, int maximumLightLevel, boolean inverted) {
        super(properties.lightLevel(state -> lightLevel(state, maximumLightLevel)));
        this.maximumLightLevel = maximumLightLevel;
        this.inverted = inverted;
        registerDefaultState(defaultBlockState().setValue(POWER, inverted ? 0 : 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withPower(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshPower(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        refreshPower(level, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            DimmerLight.removeTarget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected final BlockState withPower(BlockState state, Level level, BlockPos pos) {
        return state.setValue(POWER, DimmerLight.power(level, pos, inverted));
    }

    public final void refreshPower(Level level, BlockPos pos, BlockState state) {
        int power = DimmerLight.power(level, pos, inverted);
        if (state.getValue(POWER) != power) {
            level.setBlockAndUpdate(pos, state.setValue(POWER, power));
        }
    }

    private static int lightLevel(BlockState state, int maximumLightLevel) {
        return Math.round(state.getValue(POWER) * maximumLightLevel / 15.0F);
    }
}
