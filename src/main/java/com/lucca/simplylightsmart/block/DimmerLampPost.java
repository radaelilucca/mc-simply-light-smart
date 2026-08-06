package com.lucca.simplylightsmart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A three-block dimmable Illuminant Column. Its bottom segment owns redstone and Linker state. */
public final class DimmerLampPost extends Block implements SimpleWaterloggedBlock, DimmerLight {
    public static final EnumProperty<Position> POSITION = EnumProperty.create("position", Position.class);
    private static final VoxelShape TOP_SHAPE = Shapes.or(
            Block.box(6, 0, 6, 10, 5, 10), Block.box(5, 5, 5, 11, 6, 11), Block.box(3, 6, 3, 13, 16, 13));
    private static final VoxelShape MIDDLE_SHAPE = Shapes.or(
            Block.box(6, 0, 6, 10, 10, 10), Block.box(5, 10, 5, 11, 16, 11));
    private static final VoxelShape BOTTOM_SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 2, 13), Block.box(4, 2, 4, 12, 4, 12), Block.box(6, 4, 6, 10, 16, 10));

    private final boolean inverted;
    private final DyeColor color;

    public DimmerLampPost(DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of().strength(1.0F)
                .lightLevel(state -> state.getValue(POSITION) == Position.TOP ? state.getValue(POWER) : 0));
        this.inverted = inverted;
        this.color = color;
        registerDefaultState(defaultBlockState()
                .setValue(POSITION, Position.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, false)
                .setValue(POWER, inverted ? 0 : 15));
    }

    public DyeColor color() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POSITION, BlockStateProperties.WATERLOGGED, POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(POSITION));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 2
                || !level.getBlockState(pos.above()).canBeReplaced(context)
                || !level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return null;
        }
        return withPower(level, pos, defaultBlockState()
                .setValue(POSITION, Position.BOTTOM)
                .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockState middle = state.setValue(POSITION, Position.MIDDLE)
                .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos.above()).getType() == Fluids.WATER);
        BlockState top = state.setValue(POSITION, Position.TOP)
                .setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos.above(2)).getType() == Fluids.WATER);
        level.setBlock(pos.above(), middle, Block.UPDATE_ALL);
        level.setBlock(pos.above(2), top, Block.UPDATE_ALL);
        refreshPower(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (state.getValue(POSITION) == Position.BOTTOM) {
            refreshPower(level, pos, state);
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockPos linkPosition(Level level, BlockPos pos, BlockState state) {
        return basePosition(pos, state.getValue(POSITION));
    }

    @Override
    public VoxelShape linkOutlineShape(BlockGetter level, BlockPos pos, BlockState state) {
        return Shapes.or(BOTTOM_SHAPE, MIDDLE_SHAPE.move(0, 1, 0), TOP_SHAPE.move(0, 2, 0));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            return;
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        BlockPos base = basePosition(pos, state.getValue(POSITION));
        DimmerLight.removeTarget(level, base);
        switch (state.getValue(POSITION)) {
            case BOTTOM -> removePart(level, pos.above(), this, pos.above(2));
            case MIDDLE -> removePart(level, pos.below(), this, pos.above());
            case TOP -> removePart(level, pos.below(), this, pos.below(2));
        }
    }

    @Override
    public void refreshPower(Level level, BlockPos pos, BlockState state) {
        BlockPos base = basePosition(pos, state.getValue(POSITION));
        BlockState baseState = level.getBlockState(base);
        if (!(baseState.getBlock() instanceof DimmerLampPost)) {
            return;
        }
        int power = DimmerLight.power(level, base, inverted);
        updatePart(level, base, power);
        updatePart(level, base.above(), power);
        updatePart(level, base.above(2), power);
    }

    private static VoxelShape shapeFor(Position position) {
        return switch (position) {
            case BOTTOM -> BOTTOM_SHAPE;
            case MIDDLE -> MIDDLE_SHAPE;
            case TOP -> TOP_SHAPE;
        };
    }

    private static BlockPos basePosition(BlockPos pos, Position position) {
        return switch (position) {
            case BOTTOM -> pos;
            case MIDDLE -> pos.below();
            case TOP -> pos.below(2);
        };
    }

    private static void removePart(Level level, BlockPos first, Block expected, BlockPos second) {
        if (level.getBlockState(first).is(expected)) level.setBlockAndUpdate(first, Blocks.AIR.defaultBlockState());
        if (level.getBlockState(second).is(expected)) level.setBlockAndUpdate(second, Blocks.AIR.defaultBlockState());
    }

    private static void updatePart(Level level, BlockPos pos, int power) {
        BlockState part = level.getBlockState(pos);
        if (part.getBlock() instanceof DimmerLampPost && part.getValue(POWER) != power) {
            level.setBlockAndUpdate(pos, part.setValue(POWER, power));
        }
    }

    private BlockState withPower(Level level, BlockPos pos, BlockState state) {
        return state.setValue(POWER, DimmerLight.power(level, pos, inverted));
    }

    public enum Position implements StringRepresentable {
        BOTTOM, MIDDLE, TOP;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
