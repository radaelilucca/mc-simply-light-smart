package com.lucca.simplylightsmart.original;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Persistent, chunk-local catalogue of original Simply Light control blocks.
 *
 * <p>The values deliberately contain only the local X/Z coordinates and the
 * height relative to the world's minimum build height. That makes the
 * attachment suitable for a chunk copied into another location by a
 * contraption implementation. Columns are represented solely by their base.
 * </p>
 */
public final class OriginalLightChunkIndex implements INBTSerializable<CompoundTag> {
    public static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_TAG = "schema";
    private static final String POSITIONS_TAG = "positions";

    private int schemaVersion = SCHEMA_VERSION;
    private final Set<Integer> localPositions = new LinkedHashSet<>();

    public boolean needsRebuild() {
        return schemaVersion != SCHEMA_VERSION;
    }

    public boolean add(BlockPos pos, int minBuildHeight) {
        return localPositions.add(pack(pos, minBuildHeight));
    }

    public boolean remove(BlockPos pos, int minBuildHeight) {
        return localPositions.remove(pack(pos, minBuildHeight));
    }

    public boolean replace(Collection<BlockPos> positions, int minBuildHeight) {
        Set<Integer> replacement = new LinkedHashSet<>();
        for (BlockPos pos : positions) {
            replacement.add(pack(pos, minBuildHeight));
        }
        boolean changed = schemaVersion != SCHEMA_VERSION || !localPositions.equals(replacement);
        schemaVersion = SCHEMA_VERSION;
        localPositions.clear();
        localPositions.addAll(replacement);
        return changed;
    }

    public List<BlockPos> positions(ChunkPos chunkPos, int minBuildHeight) {
        return localPositions.stream().map(packed -> unpack(chunkPos, packed, minBuildHeight)).toList();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_TAG, schemaVersion);
        tag.putIntArray(POSITIONS_TAG, localPositions.stream().mapToInt(Integer::intValue).toArray());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        schemaVersion = tag.getInt(SCHEMA_TAG);
        localPositions.clear();
        for (int packed : tag.getIntArray(POSITIONS_TAG)) {
            localPositions.add(packed);
        }
    }

    private static int pack(BlockPos pos, int minBuildHeight) {
        int localY = pos.getY() - minBuildHeight;
        if (localY < 0 || localY > 0xFFFFFF) {
            throw new IllegalArgumentException("Position is outside the supported build height: " + pos);
        }
        return localY << 8 | (pos.getZ() & 15) << 4 | (pos.getX() & 15);
    }

    private static BlockPos unpack(ChunkPos chunkPos, int packed, int minBuildHeight) {
        int localX = packed & 15;
        int localZ = packed >>> 4 & 15;
        int localY = packed >>> 8;
        return new BlockPos(chunkPos.getMinBlockX() + localX, minBuildHeight + localY, chunkPos.getMinBlockZ() + localZ);
    }
}
