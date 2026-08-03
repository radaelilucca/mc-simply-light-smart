package com.lucca.simplylightsmart.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * A Simply Light-compatible bulb whose brightness follows the surrounding
 * redstone signal. The inverted variant brightens as the signal increases.
 */
public final class DimmerLightBulb extends AbstractDimmerRotatableLamp {
    private final DyeColor color;

    public DimmerLightBulb(DyeColor color, boolean inverted) {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .strength(1.0F), 14, inverted);
        this.color = color;

        UP = Block.box(6, 0, 6, 10, 5, 10);
        DOWN = Block.box(6, 11, 6, 10, 16, 10);
        EAST = Block.box(0, 6, 6, 5, 10, 10);
        WEST = Block.box(11, 6, 6, 16, 10, 10);
        NORTH = Block.box(6, 6, 11, 10, 10, 16);
        SOUTH = Block.box(6, 6, 0, 10, 10, 5);

    }

    public DyeColor color() {
        return color;
    }

}
