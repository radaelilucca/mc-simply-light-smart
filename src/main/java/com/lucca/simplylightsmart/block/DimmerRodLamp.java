package com.lucca.simplylightsmart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dimmable Illuminant Rod with the original three axis-aligned shapes. */
public final class DimmerRodLamp extends Block implements SimpleWaterloggedBlock, DimmerLight {
    private static final VoxelShape UP_DOWN = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape EAST_WEST = Block.box(0, 7, 7, 16, 9, 9);
    private static final VoxelShape NORTH_SOUTH = Block.box(7, 7, 0, 9, 9, 16);

    private final boolean inverted;
    private final DyeColor color;

    public DimmerRodLamp(DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of().strength(1.0F)
                .lightLevel(state -> state.getValue(POWER)));
        this.color = color;
        this.inverted = inverted;
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y)
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(POWER, inverted ? 0 : 15));
    }

    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.AXIS, BlockStateProperties.WATERLOGGED, POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.AXIS)) {
            case X -> EAST_WEST;
            case Y -> UP_DOWN;
            case Z -> NORTH_SOUTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withPower(context.getLevel(), context.getClickedPos(), defaultBlockState()
                .setValue(BlockStateProperties.AXIS, context.getClickedFace().getAxis())
                .setValue(BlockStateProperties.WATERLOGGED,
                        context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER));
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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
        BlockState updated = withPower(level, pos, state);
        if (updated != state) {
            level.setBlockAndUpdate(pos, updated);
        }
    }

    private BlockState withPower(Level level, BlockPos pos, BlockState state) {
        return state.setValue(POWER, DimmerLight.power(level, pos, inverted));
    }
}
