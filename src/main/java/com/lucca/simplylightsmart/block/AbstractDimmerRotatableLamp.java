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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Shared redstone dimmer behavior for the addon’s rotatable light blocks. */
public abstract class AbstractDimmerRotatableLamp extends RotatableLamp {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

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
        updatePower(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        updatePower(level, pos, state);
    }

    protected final BlockState withPower(BlockState state, Level level, BlockPos pos) {
        return state.setValue(POWER, redstonePower(level, pos));
    }

    private void updatePower(Level level, BlockPos pos, BlockState state) {
        int power = redstonePower(level, pos);
        if (state.getValue(POWER) != power) {
            level.setBlockAndUpdate(pos, state.setValue(POWER, power));
        }
    }

    private int redstonePower(Level level, BlockPos pos) {
        int signal = level.getBestNeighborSignal(pos);
        return inverted ? signal : 15 - signal;
    }

    private static int lightLevel(BlockState state, int maximumLightLevel) {
        return Math.round(state.getValue(POWER) * maximumLightLevel / 15.0F);
    }
}
