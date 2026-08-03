package com.lucca.simplylightsmart.client;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.item.SmartLinkerItem;
import com.lucca.simplylightsmart.network.ClearLinkerSelectionPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Handles the client-only empty-air click that requests Linker selection clearing. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, value = Dist.CLIENT)
public final class SmartLinkerClientEvents {
    private SmartLinkerClientEvents() {
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity().isShiftKeyDown()
                && event.getEntity().getMainHandItem().getItem() instanceof SmartLinkerItem) {
            PacketDistributor.sendToServer(new ClearLinkerSelectionPayload());
        }
    }
}
