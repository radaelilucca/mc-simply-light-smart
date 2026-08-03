package com.lucca.simplylightstoggle.client;

import com.lucca.simplylightstoggle.SimplyLightsToggle;
import com.lucca.simplylightstoggle.block.AbstractDimmerRotatableLamp;
import com.lucca.simplylightstoggle.registry.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only tinting for the four visible dimmer stages. */
@EventBusSubscriber(modid = SimplyLightsToggle.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DimmerColorHandler {
    private DimmerColorHandler() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? stageTint(state) : 0xFFFFFF, ModBlocks.dimmerBlocks());
    }

    private static int stageTint(BlockState state) {
        int power = state.getValue(AbstractDimmerRotatableLamp.POWER);
        if (power == 0) {
            return 0x242424;
        }
        if (power <= 5) {
            return 0x686868;
        }
        if (power <= 10) {
            return 0xB0B0B0;
        }
        return 0xFFFFFF;
    }
}
