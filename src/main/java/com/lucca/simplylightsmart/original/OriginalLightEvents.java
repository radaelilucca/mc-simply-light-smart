package com.lucca.simplylightsmart.original;

import com.lucca.simplylightsmart.SimplyLightSmart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Server-authoritative lifecycle hooks for enabled original-light families. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID)
public final class OriginalLightEvents {
    private OriginalLightEvents() {
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            OriginalLightAdapter.refreshLocal(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (Direction direction : event.getNotifiedSides()) {
            BlockPos target = event.getPos().relative(direction);
            // A neighbour notification can cross into a chunk still being
            // promoted to FULL. Never make the server thread wait for it:
            // that chunk's own load hook will refresh the lamp when ready.
            LevelChunk targetChunk = level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4);
            if (targetChunk != null && OriginalLightAdapter.isLocallyEnabled(targetChunk.getBlockState(target))) {
                // Let native neighbour handlers finish before reading their state.
                level.getServer().execute(() -> OriginalLightAdapter.refreshLocal(level, target));
            }
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            OriginalLightAdapter.removeTarget(level, event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ChunkPos chunkPos = event.getChunk().getPos();
        level.getServer().execute(() -> refreshChunk(level, chunkPos));
    }

    private static void refreshChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
                    pos.set(x, y, z);
                    if (OriginalLightAdapter.isLocallyEnabled(level.getBlockState(pos))) {
                        OriginalLightAdapter.refreshLocal(level, pos);
                    }
                }
            }
        }
    }
}
