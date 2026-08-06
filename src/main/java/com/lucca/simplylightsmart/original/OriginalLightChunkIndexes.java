package com.lucca.simplylightsmart.original;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Maintains the chunk attachment used to avoid repeat light scans on load. */
public final class OriginalLightChunkIndexes {
    private OriginalLightChunkIndexes() {
    }

    /** Records a supported lamp placed in an already loaded chunk. */
    public static void indexPlacedLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!OriginalLightAdapter.isLocallyEnabled(state)) {
            return;
        }
        BlockPos controlPos = OriginalLightAdapter.controlPosition(pos, state);
        LevelChunk chunk = loadedChunk(level, controlPos);
        if (chunk == null) {
            return;
        }
        OriginalLightChunkIndex index = chunk.getData(ModAttachments.ORIGINAL_LIGHT_INDEX);
        if (index.add(controlPos, level.getMinBuildHeight())) {
            chunk.setUnsaved(true);
        }
    }

    /** Removes a control position before its source state leaves the world. */
    public static void removeBrokenLight(ServerLevel level, BlockPos pos, BlockState state) {
        if (!OriginalLightAdapter.isLocallyEnabled(state)) {
            return;
        }
        BlockPos controlPos = OriginalLightAdapter.controlPosition(pos, state);
        LevelChunk chunk = loadedChunk(level, controlPos);
        if (chunk == null) {
            return;
        }
        chunk.getExistingData(ModAttachments.ORIGINAL_LIGHT_INDEX).ifPresent(index -> {
            if (index.remove(controlPos, level.getMinBuildHeight())) {
                chunk.setUnsaved(true);
            }
        });
    }

    /**
     * Refreshes indexed controls. A missing or old attachment is rebuilt by
     * scanning once, which covers contraption operations that omit attachments.
     */
    public static void refreshLoadedChunk(ServerLevel level, LevelChunk chunk) {
        OriginalLightChunkIndex index = chunk.getExistingData(ModAttachments.ORIGINAL_LIGHT_INDEX).orElse(null);
        if (index == null || index.needsRebuild()) {
            index = chunk.getData(ModAttachments.ORIGINAL_LIGHT_INDEX);
            rebuild(level, chunk, index);
            SimplyLightSmart.LOGGER.debug("Rebuilt original-light index for chunk {} in {}", chunk.getPos(), level.dimension().location());
        } else {
            SimplyLightSmart.LOGGER.debug("Reused original-light index for chunk {} in {}", chunk.getPos(), level.dimension().location());
        }

        boolean changed = false;
        for (BlockPos pos : index.positions(chunk.getPos(), level.getMinBuildHeight())) {
            BlockState state = chunk.getBlockState(pos);
            if (!OriginalLightAdapter.isLocallyEnabled(state)
                    || !OriginalLightAdapter.controlPosition(pos, state).equals(pos)) {
                changed |= index.remove(pos, level.getMinBuildHeight());
                continue;
            }
            OriginalLightAdapter.refreshLocal(level, pos);
        }
        if (changed) {
            chunk.setUnsaved(true);
        }
    }

    private static void rebuild(ServerLevel level, LevelChunk chunk, OriginalLightChunkIndex index) {
        List<BlockPos> controls = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            for (int z = chunk.getPos().getMinBlockZ(); z <= chunk.getPos().getMaxBlockZ(); z++) {
                for (int x = chunk.getPos().getMinBlockX(); x <= chunk.getPos().getMaxBlockX(); x++) {
                    pos.set(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (OriginalLightAdapter.isLocallyEnabled(state)
                            && OriginalLightAdapter.controlPosition(pos, state).equals(pos)) {
                        controls.add(pos.immutable());
                    }
                }
            }
        }
        if (index.replace(controls, level.getMinBuildHeight())) {
            chunk.setUnsaved(true);
        }
    }

    private static LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
