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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dimmable Illuminant Edge that retains the original four-way connection shape. */
public final class DimmerEdgeLight extends Block implements SimpleWaterloggedBlock, DimmerLight {
    private final VoxelShape west;
    private final VoxelShape east;
    private final VoxelShape south;
    private final VoxelShape north;
    private final VoxelShape all;
    private final boolean inverted;
    private final DyeColor color;

    public DimmerEdgeLight(boolean top, DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of().noCollission().strength(1.0F)
                .lightLevel(state -> state.getValue(POWER)));
        this.inverted = inverted;
        this.color = color;
        if (top) {
            west = Shapes.box(0, 0.9375D, 0, 0.0625D, 1, 1);
            east = Shapes.box(0.9375D, 0.9375D, 0, 1, 1, 1);
            south = Shapes.box(0, 0.9375D, 0.9375D, 1, 1, 1);
            north = Shapes.box(0, 0.9375D, 0, 1, 1, 0.0625D);
        } else {
            west = Shapes.box(0, 0, 0, 0.0625D, 0.0625D, 1);
            east = Shapes.box(0.9375D, 0, 0, 1, 0.0625D, 1);
            south = Shapes.box(0, 0, 0.9375D, 1, 0.0625D, 1);
            north = Shapes.box(0, 0, 0, 1, 0.0625D, 0.0625D);
        }
        all = Shapes.or(west, east, south, north);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.WEST, false)
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(POWER, inverted ? 0 : 15));
    }

    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.SOUTH, BlockStateProperties.EAST,
                BlockStateProperties.WEST, BlockStateProperties.WATERLOGGED, POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(BlockStateProperties.NORTH)) shape = Shapes.or(shape, north);
        if (state.getValue(BlockStateProperties.SOUTH)) shape = Shapes.or(shape, south);
        if (state.getValue(BlockStateProperties.EAST)) shape = Shapes.or(shape, east);
        if (state.getValue(BlockStateProperties.WEST)) shape = Shapes.or(shape, west);
        return shape.isEmpty() ? all : shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        BlockState state = defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER)
                .setValue(BlockStateProperties.NORTH, supports(level, pos, Direction.NORTH))
                .setValue(BlockStateProperties.SOUTH, supports(level, pos, Direction.SOUTH))
                .setValue(BlockStateProperties.EAST, supports(level, pos, Direction.EAST))
                .setValue(BlockStateProperties.WEST, supports(level, pos, Direction.WEST));
        return withPower(level, pos, state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (direction.getAxis().isHorizontal()) {
            state = state.setValue(property(direction), supports(level, pos, direction));
        }
        return state;
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

    private static boolean supports(LevelAccessor level, BlockPos pos, Direction side) {
        return Block.canSupportCenter(level, pos.relative(side), side.getOpposite());
    }

    private static net.minecraft.world.level.block.state.properties.BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockStateProperties.NORTH;
            case SOUTH -> BlockStateProperties.SOUTH;
            case EAST -> BlockStateProperties.EAST;
            case WEST -> BlockStateProperties.WEST;
            default -> throw new IllegalArgumentException("Edge lights only connect horizontally");
        };
    }

    private BlockState withPower(Level level, BlockPos pos, BlockState state) {
        return state.setValue(POWER, DimmerLight.power(level, pos, inverted));
    }
}
