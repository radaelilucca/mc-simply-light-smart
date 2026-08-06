package com.lucca.simplylightsmart.smart;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.block.DimmerLight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Applies the latest remote signal after a target chunk returns to memory. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID)
public final class SmartLinkEvents {
    private SmartLinkEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunkPos = event.getChunk().getPos();
        // ChunkEvent.Load may fire before FULL promotion. Run on the next server
        // task rather than touching blocks during the loading callback itself.
        level.getServer().execute(() -> refreshLoadedTargets(level, chunkPos));
    }

    private static void refreshLoadedTargets(ServerLevel level, ChunkPos chunkPos) {
        for (var target : SmartLinkSavedData.get(level).targetsInChunk(level.dimension(), chunkPos)) {
            LevelChunk targetChunk = level.getChunkSource().getChunkNow(
                    target.pos().getX() >> 4, target.pos().getZ() >> 4);
            if (targetChunk == null) {
                continue;
            }
            BlockState targetState = targetChunk.getBlockState(target.pos());
            if (targetState.getBlock() instanceof DimmerLight dimmer) {
                dimmer.refreshPower(level, target.pos(), targetState);
            }
        }
    }
}
