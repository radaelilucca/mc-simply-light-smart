package com.lucca.simplylightsmart.client;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.block.DimmerLight;
import com.lucca.simplylightsmart.registry.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only tinting for the seven visible dimmer stages. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DimmerColorHandler {
    private static final int[] STAGE_TINTS = {
            0x242424, // 0
            0x484848, // 1-2
            0x686868, // 3-5
            0x909090, // 6-8
            0xB0B0B0, // 9-11
            0xD0D0D0, // 12-14
            0xFFFFFF  // 15
    };
    private static final int INVERTED_ITEM_TINT = 0x606060;

    private DimmerColorHandler() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? stageTint(state) : 0xFFFFFF, ModBlocks.dimmerBlocks());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? INVERTED_ITEM_TINT : 0xFFFFFF, ModBlocks.invertedDimmerItems());
    }

    private static int stageTint(BlockState state) {
        int power = state.getValue(DimmerLight.POWER);
        if (power <= 2) {
            return STAGE_TINTS[power == 0 ? 0 : 1];
        }
        return STAGE_TINTS[(power - 3) / 3 + 2];
    }
}
