package com.lucca.simplylightstoggle.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Redstone-dimmable equivalent of Simply Light's wall lamp fixture. */
public final class DimmerFixture extends AbstractDimmerRotatableLamp {
    private final DyeColor color;

    public DimmerFixture(DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of().noCollission().strength(1.0F), 15, inverted);
        this.color = color;

        UP = Block.box(6, 0, 6, 10, 2, 10);
        DOWN = Block.box(6, 14, 6, 10, 16, 10);
        EAST = Block.box(0, 6, 4, 3, 10, 12);
        WEST = Block.box(13, 6, 4, 16, 10, 12);
        NORTH = Block.box(4, 6, 13, 12, 10, 16);
        SOUTH = Block.box(4, 6, 0, 12, 10, 3);
    }

    public DyeColor color() {
        return color;
    }
}
