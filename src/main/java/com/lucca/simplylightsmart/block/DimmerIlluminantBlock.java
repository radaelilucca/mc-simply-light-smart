package com.lucca.simplylightsmart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Full-block Illuminant Block variant whose light level follows redstone. */
public final class DimmerIlluminantBlock extends Block implements DimmerLight {
    private final boolean inverted;
    private final DyeColor color;

    public DimmerIlluminantBlock(DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of().strength(1.0F)
                .lightLevel(state -> Math.round(state.getValue(POWER) * 15.0F / 15.0F)));
        this.color = color;
        this.inverted = inverted;
        registerDefaultState(defaultBlockState().setValue(POWER, inverted ? 0 : 15));
    }

    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withPower(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        refreshPower(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        refreshPower(level, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            DimmerLight.removeTarget(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void refreshPower(Level level, BlockPos pos, BlockState state) {
        BlockState updatedState = withPower(level, pos, state);
        if (updatedState != state) {
            level.setBlockAndUpdate(pos, updatedState);
        }
    }

    private BlockState withPower(Level level, BlockPos pos, BlockState state) {
        return state.setValue(POWER, DimmerLight.power(level, pos, inverted));
    }
}
