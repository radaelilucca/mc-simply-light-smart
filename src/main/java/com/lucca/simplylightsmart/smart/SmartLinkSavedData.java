package com.lucca.simplylightsmart.smart;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-persistent ownership index for smart links.
 *
 * <p>The block entity stores the ordered target list for its own UI and
 * updates. This index is deliberately target-first so a dimmer can determine
 * in constant time whether local redstone must be ignored.</p>
 */
public final class SmartLinkSavedData extends SavedData {
    private static final String DATA_NAME = "simply_light_smart_links";
    private static final String LINKS_TAG = "links";
    private static final String TARGET_DIMENSION_TAG = "target_dimension";
    private static final String TARGET_POSITION_TAG = "target_position";
    private static final String SWITCH_DIMENSION_TAG = "switch_dimension";
    private static final String SWITCH_POSITION_TAG = "switch_position";

    private final Map<GlobalPos, GlobalPos> switchByTarget = new HashMap<>();

    public static SmartLinkSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SmartLinkSavedData::new, SmartLinkSavedData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
                DATA_NAME);
    }

    public static SmartLinkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SmartLinkSavedData data = new SmartLinkSavedData();
        ListTag links = tag.getList(LINKS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < links.size(); index++) {
            CompoundTag entry = links.getCompound(index);
            readGlobalPos(entry, TARGET_DIMENSION_TAG, TARGET_POSITION_TAG)
                    .flatMap(target -> readGlobalPos(entry, SWITCH_DIMENSION_TAG, SWITCH_POSITION_TAG)
                            .map(switchPos -> Map.entry(target, switchPos)))
                    .ifPresent(link -> data.switchByTarget.put(link.getKey(), link.getValue()));
        }
        return data;
    }

    public Optional<GlobalPos> ownerOf(GlobalPos target) {
        return Optional.ofNullable(switchByTarget.get(target));
    }

    public boolean isOwnedBy(GlobalPos target, GlobalPos switchPos) {
        return switchPos.equals(switchByTarget.get(target));
    }

    /** Finds links in a loaded chunk without requesting any additional chunks. */
    public Iterable<GlobalPos> targetsInChunk(ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return switchByTarget.keySet().stream()
                .filter(target -> target.dimension().equals(dimension) && new ChunkPos(target.pos()).equals(chunkPos))
                .toList();
    }

    /** Returns false without changing state when another switch owns a target. */
    public boolean replaceTargets(GlobalPos switchPos, Iterable<GlobalPos> currentTargets, Iterable<GlobalPos> requestedTargets) {
        for (GlobalPos target : requestedTargets) {
            GlobalPos owner = switchByTarget.get(target);
            if (owner != null && !owner.equals(switchPos)) {
                return false;
            }
        }

        for (GlobalPos target : currentTargets) {
            if (switchPos.equals(switchByTarget.get(target))) {
                switchByTarget.remove(target);
            }
        }
        for (GlobalPos target : requestedTargets) {
            switchByTarget.put(target, switchPos);
        }
        setDirty();
        return true;
    }

    /** Removes every index entry owned by a switch, including stale entries. */
    public void removeSwitch(GlobalPos switchPos) {
        if (switchByTarget.entrySet().removeIf(entry -> entry.getValue().equals(switchPos))) {
            setDirty();
        }
    }

    /** Removes a target and returns the switch that owned it, if any. */
    public Optional<GlobalPos> removeTarget(GlobalPos target) {
        GlobalPos owner = switchByTarget.remove(target);
        if (owner != null) {
            setDirty();
        }
        return Optional.ofNullable(owner);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag links = new ListTag();
        switchByTarget.forEach((target, switchPos) -> {
            CompoundTag entry = new CompoundTag();
            writeGlobalPos(entry, TARGET_DIMENSION_TAG, TARGET_POSITION_TAG, target);
            writeGlobalPos(entry, SWITCH_DIMENSION_TAG, SWITCH_POSITION_TAG, switchPos);
            links.add(entry);
        });
        tag.put(LINKS_TAG, links);
        return tag;
    }

    private static void writeGlobalPos(CompoundTag tag, String dimensionKey, String positionKey, GlobalPos value) {
        tag.putString(dimensionKey, value.dimension().location().toString());
        tag.putLong(positionKey, value.pos().asLong());
    }

    private static Optional<GlobalPos> readGlobalPos(CompoundTag tag, String dimensionKey, String positionKey) {
        if (!tag.contains(dimensionKey) || !tag.contains(positionKey)) {
            return Optional.empty();
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(dimensionKey));
        if (dimensionId == null) {
            return Optional.empty();
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        return Optional.of(GlobalPos.of(dimension, BlockPos.of(tag.getLong(positionKey))));
    }
}
