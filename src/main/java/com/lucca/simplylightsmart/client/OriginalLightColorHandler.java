package com.lucca.simplylightsmart.client;

import com.flanks255.simplylight.SLBlockReg;
import com.flanks255.simplylight.SLBlocks;
import com.flanks255.simplylight.blocks.LightBulb;
import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Applies a brightness tint to the Lightbulb glow layer without replacing its texture. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OriginalLightColorHandler {
    private OriginalLightColorHandler() {
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? glowTint(state) : 0xFFFFFF,
                originalBulbs());
    }

    private static Block[] originalBulbs() {
        return SLBlocks.BULBS.stream().map(SLBlockReg::get).toArray(Block[]::new);
    }

    private static int glowTint(BlockState state) {
        if (!OriginalLightAdapter.hasSmartPower(state)) {
            return 0xFFFFFF;
        }
        int power = state.getValue(OriginalLightAdapter.SMART_POWER);
        int channel = 0x18 + Math.round(power * (0xFF - 0x18) / 15.0F);
        return channel << 16 | channel << 8 | channel;
    }
}
