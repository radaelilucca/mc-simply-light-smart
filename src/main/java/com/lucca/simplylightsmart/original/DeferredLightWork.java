package com.lucca.simplylightsmart.original;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Defers light writes until a server tick after the event that discovered
 * them. This prevents callbacks during chunk restoration from re-entering a
 * mod's chunk or lighting implementation.
 */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID)
public final class DeferredLightWork {
    private static final Map<ChunkKey, PendingChunk> CHUNK_REFRESHES = new LinkedHashMap<>();
    private static final Map<PositionKey, PendingPosition> POSITION_REFRESHES = new LinkedHashMap<>();
    private static final Map<PositionKey, PendingSwitch> SWITCH_REFRESHES = new LinkedHashMap<>();

    private DeferredLightWork() {
    }

    /** Records a fully loaded chunk without inspecting or modifying it. */
    public static void enqueueLoadedChunk(ServerLevel level, LevelChunk chunk) {
        ChunkKey key = new ChunkKey(level, chunk.getPos().toLong());
        CHUNK_REFRESHES.put(key, new PendingChunk(chunk, nextTick(level.getServer())));
    }

    /** Records one known-light position for a later redstone refresh. */
    public static void enqueuePositionRefresh(ServerLevel level, BlockPos pos) {
        PositionKey key = new PositionKey(level, pos.asLong());
        POSITION_REFRESHES.put(key, new PendingPosition(nextTick(level.getServer())));
    }

    /**
     * Defers a linked target without loading its chunk. The position refresh is
     * applied only if that chunk is still loaded when its turn arrives.
     */
    public static void enqueueLinkedTargetRefresh(ServerLevel sourceLevel, GlobalPos target) {
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(target.dimension());
        if (targetLevel != null) {
            enqueuePositionRefresh(targetLevel, target.pos());
        }
    }

    /** Records a Switch whose persisted state has just become available. */
    public static void enqueueSwitchInitialization(ServerLevel level, BlockPos pos, SmartSwitchBlockEntity switchEntity) {
        PositionKey key = new PositionKey(level, pos.asLong());
        SWITCH_REFRESHES.merge(key, new PendingSwitch(switchEntity, true, nextTick(level.getServer())),
                (current, queued) -> new PendingSwitch(queued.switchEntity(), current.initialize() || queued.initialize(),
                        Math.min(current.eligibleTick(), queued.eligibleTick())));
    }

    /** Records a normal redstone-driven Switch refresh. */
    public static void enqueueSwitchRefresh(ServerLevel level, BlockPos pos, SmartSwitchBlockEntity switchEntity) {
        PositionKey key = new PositionKey(level, pos.asLong());
        SWITCH_REFRESHES.merge(key, new PendingSwitch(switchEntity, false, nextTick(level.getServer())),
                (current, queued) -> new PendingSwitch(queued.switchEntity(), current.initialize(),
                        Math.min(current.eligibleTick(), queued.eligibleTick())));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long currentTick = server.getTickCount();
        processChunks(currentTick);
        processPositions(currentTick);
        processSwitches(currentTick);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CHUNK_REFRESHES.clear();
        POSITION_REFRESHES.clear();
        SWITCH_REFRESHES.clear();
    }

    private static void processChunks(long currentTick) {
        List<Map.Entry<ChunkKey, PendingChunk>> ready = removeReady(CHUNK_REFRESHES, currentTick);
        for (Map.Entry<ChunkKey, PendingChunk> entry : ready) {
            PendingChunk pending = entry.getValue();
            ChunkKey key = entry.getKey();
            LevelChunk current = key.level().getChunkSource().getChunkNow(key.chunkX(), key.chunkZ());
            if (current == pending.chunk()) {
                OriginalLightEvents.refreshLoadedChunk(key.level(), current);
            }
        }
    }

    private static void processPositions(long currentTick) {
        List<Map.Entry<PositionKey, PendingPosition>> ready = removeReady(POSITION_REFRESHES, currentTick);
        for (Map.Entry<PositionKey, PendingPosition> entry : ready) {
            PositionKey key = entry.getKey();
            BlockPos pos = BlockPos.of(key.pos());
            LevelChunk chunk = key.level().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                OriginalLightAdapter.refreshLocal(key.level(), pos);
            }
        }
    }

    private static void processSwitches(long currentTick) {
        List<Map.Entry<PositionKey, PendingSwitch>> ready = removeReady(SWITCH_REFRESHES, currentTick);
        for (Map.Entry<PositionKey, PendingSwitch> entry : ready) {
            PendingSwitch pending = entry.getValue();
            PositionKey key = entry.getKey();
            BlockPos pos = BlockPos.of(key.pos());
            if (key.level().getBlockEntity(pos) != pending.switchEntity()) {
                continue;
            }
            if (pending.initialize()) {
                pending.switchEntity().initializeAfterLoad();
            } else {
                pending.switchEntity().refreshSignal();
            }
        }
    }

    private static <K, V extends PendingWork> List<Map.Entry<K, V>> removeReady(Map<K, V> queue, long currentTick) {
        List<Map.Entry<K, V>> ready = new ArrayList<>();
        Iterator<Map.Entry<K, V>> entries = queue.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<K, V> entry = entries.next();
            if (entry.getValue().eligibleTick() > currentTick) {
                continue;
            }
            entries.remove();
            ready.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return ready;
    }

    private static long nextTick(MinecraftServer server) {
        return (long) server.getTickCount() + 1L;
    }

    private record ChunkKey(ServerLevel level, long chunkPos) {
        int chunkX() {
            return (int) chunkPos;
        }

        int chunkZ() {
            return (int) (chunkPos >>> 32);
        }
    }

    private record PositionKey(ServerLevel level, long pos) {
    }

    private sealed interface PendingWork permits PendingChunk, PendingPosition, PendingSwitch {
        long eligibleTick();
    }

    private record PendingChunk(LevelChunk chunk, long eligibleTick) implements PendingWork {
    }

    private record PendingPosition(long eligibleTick) implements PendingWork {
    }

    private record PendingSwitch(SmartSwitchBlockEntity switchEntity, boolean initialize, long eligibleTick) implements PendingWork {
    }
}
