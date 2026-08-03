package com.lucca.simplylightsmart.network;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.item.SmartLinkerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the small server-authoritative interactions initiated from the client. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ClearLinkerSelectionPayload.TYPE, ClearLinkerSelectionPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> {
                            if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
                                return;
                            }
                            ItemStack stack = player.getMainHandItem();
                            if (stack.getItem() instanceof SmartLinkerItem && SmartLinkerItem.clearSelection(stack)) {
                                player.displayClientMessage(
                                        Component.translatable("message.simply_light_smart.linker.selection_cleared"), true);
                            }
                        }));
    }
}
