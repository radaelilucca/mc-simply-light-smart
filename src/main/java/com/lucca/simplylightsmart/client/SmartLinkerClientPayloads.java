package com.lucca.simplylightsmart.client;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.network.LinkerHighlightsPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers client-only handling for Smart Linker highlight updates. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SmartLinkerClientPayloads {
    private SmartLinkerClientPayloads() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(LinkerHighlightsPayload.TYPE, LinkerHighlightsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SmartLinkerSelectionOutline.setLinkedTargets(payload.links())));
    }
}
