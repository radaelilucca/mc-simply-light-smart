package com.lucca.simplylightsmart.original;

import com.lucca.simplylightsmart.SimplyLightSmart;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-authoritative placement update for the phase 0 Lightbulb spike. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID)
public final class LightBulbSpikeEvents {
    private LightBulbSpikeEvents() {
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LightBulbSpike.refreshFromRedstone(level, event.getPos());
        }
    }

    /**
     * NeoForge exposes the neighbours being notified after a block changes.
     * Checking only those positions avoids a Mixin into the remapped vanilla
     * Block class and keeps the spike scoped to adjacent Lightbulbs.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Direction direction : event.getNotifiedSides()) {
            LightBulbSpike.refreshFromRedstone(level, event.getPos().relative(direction));
        }
    }
}
