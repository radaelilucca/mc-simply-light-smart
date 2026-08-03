package com.lucca.simplylightsmart.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/** Redstone-dimmable equivalent of Simply Light's rotatable slabs and panels. */
public final class DimmerThinLamp extends AbstractDimmerRotatableLamp {
    private final DyeColor color;

    public DimmerThinLamp(double thickness, DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of().strength(1.0F), 15, inverted);
        this.color = color;

        UP = Block.box(0, 0, 0, 16, thickness, 16);
        DOWN = Block.box(0, 16 - thickness, 0, 16, 16, 16);
        EAST = Block.box(0, 0, 0, thickness, 16, 16);
        WEST = Block.box(16 - thickness, 0, 0, 16, 16, 16);
        NORTH = Block.box(0, 0, 16 - thickness, 16, 16, 16);
        SOUTH = Block.box(0, 0, 0, 16, 16, thickness);
    }

    public DyeColor color() {
        return color;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos adjacentPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state;

        if (context.getPlayer() != null
                && !context.getPlayer().isCrouching()
                && context.getLevel().getBlockState(adjacentPos).getBlock() instanceof DimmerThinLamp) {
            state = defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, waterlogged)
                    .setValue(BlockStateProperties.FACING, context.getLevel().getBlockState(adjacentPos).getValue(BlockStateProperties.FACING));
        } else {
            state = defaultBlockState()
                    .setValue(BlockStateProperties.FACING, context.getClickedFace())
                    .setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        }

        return withPower(state, context.getLevel(), context.getClickedPos());
    }
}
