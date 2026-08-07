package com.lucca.simplylightsmart.smart;

import com.lucca.simplylightsmart.SimplyLightSmart;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
    private static final String VERSION_TAG = "version";
    private static final int CURRENT_VERSION = 3;
    private static final String LINKS_TAG = "links";
    private static final String SWITCH_SIGNALS_TAG = "switch_signals";
    private static final String ADDRESS_REMAPS_TAG = "address_remaps";
    private static final String SIGNAL_TAG = "signal";

    private final Map<SmartTargetAddress, SmartTargetAddress> switchByTarget = new HashMap<>();
    private final Map<SmartTargetAddress, Integer> signalBySwitch = new HashMap<>();
    private final Map<SmartTargetAddress, SmartTargetAddress> addressRemaps = new HashMap<>();

    public static SmartLinkSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SmartLinkSavedData::new, SmartLinkSavedData::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE),
                DATA_NAME);
    }

    public static SmartLinkSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SmartLinkSavedData data = new SmartLinkSavedData();
        int version = tag.contains(VERSION_TAG) ? tag.getInt(VERSION_TAG) : 0;
        if (version > CURRENT_VERSION) {
            SimplyLightSmart.LOGGER.warn("Loading Smart Link data version {} with reader version {}", version, CURRENT_VERSION);
        }
        ListTag links = tag.getList(LINKS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < links.size(); index++) {
            CompoundTag entry = links.getCompound(index);
            SmartTargetAddress.read(entry, "target")
                    .flatMap(target -> SmartTargetAddress.read(entry, "switch")
                            .map(switchPos -> Map.entry(target, switchPos)))
                    .ifPresent(link -> data.switchByTarget.put(link.getKey(), link.getValue()));
        }
        ListTag signals = tag.getList(SWITCH_SIGNALS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < signals.size(); index++) {
            CompoundTag entry = signals.getCompound(index);
            SmartTargetAddress.read(entry, "switch")
                    .ifPresent(switchPos -> data.signalBySwitch.put(switchPos, Math.clamp(entry.getInt(SIGNAL_TAG), 0, 15)));
        }
        ListTag remaps = tag.getList(ADDRESS_REMAPS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < remaps.size(); index++) {
            CompoundTag entry = remaps.getCompound(index);
            SmartTargetAddress.read(entry, "from")
                    .flatMap(from -> SmartTargetAddress.read(entry, "to").map(to -> Map.entry(from, to)))
                    .ifPresent(remap -> data.addressRemaps.put(remap.getKey(), remap.getValue()));
        }
        return data;
    }

    public Optional<SmartTargetAddress> ownerOf(SmartTargetAddress target) {
        SmartTargetAddress canonicalTarget = canonicalAddress(target);
        SmartTargetAddress owner = switchByTarget.get(canonicalTarget);
        if (owner != null) {
            return Optional.of(owner);
        }
        return switchByTarget.entrySet().stream()
                .filter(entry -> entry.getKey().matchesFallback(canonicalTarget))
                .findFirst()
                .map(entry -> migrateTarget(entry.getKey(), canonicalTarget, entry.getValue()));
    }

    public boolean isOwnedBy(SmartTargetAddress target, SmartTargetAddress switchPos) {
        return ownerOf(target).map(switchPos::equals).orElse(false);
    }

    /** Returns the latest signal published by a Switch without loading its chunk. */
    public int signalOf(SmartTargetAddress switchPos) {
        SmartTargetAddress canonicalSwitch = canonicalAddress(switchPos);
        Integer signal = signalBySwitch.get(canonicalSwitch);
        if (signal != null) {
            return signal;
        }
        return signalBySwitch.entrySet().stream()
                .filter(entry -> entry.getKey().matchesFallback(canonicalSwitch))
                .findFirst()
                .map(entry -> migrateSignal(entry.getKey(), canonicalSwitch, entry.getValue()))
                .orElse(0);
    }

    /** Stores the latest signal published by a loaded Switch. */
    public void setSwitchSignal(SmartTargetAddress switchPos, int signal) {
        switchPos = canonicalAddress(switchPos);
        signal = Math.clamp(signal, 0, 15);
        if (!Integer.valueOf(signal).equals(signalBySwitch.put(switchPos, signal))) {
            setDirty();
        }
    }

    /** Finds links in a loaded chunk without requesting any additional chunks. */
    public Iterable<SmartTargetAddress> targetsInChunk(net.minecraft.resources.ResourceKey<Level> dimension, ChunkPos chunkPos) {
        return switchByTarget.keySet().stream()
                .filter(target -> target.fallback().dimension().equals(dimension) && new ChunkPos(target.fallback().pos()).equals(chunkPos))
                .toList();
    }

    /** Returns false without changing state when another switch owns a target. */
    public boolean replaceTargets(SmartTargetAddress switchPos, Iterable<SmartTargetAddress> currentTargets,
                                  Iterable<SmartTargetAddress> requestedTargets) {
        switchPos = canonicalAddress(switchPos);
        for (SmartTargetAddress target : requestedTargets) {
            target = canonicalAddress(target);
            SmartTargetAddress owner = ownerOf(target).orElse(null);
            if (owner != null && !owner.equals(switchPos)) {
                return false;
            }
        }

        for (SmartTargetAddress target : currentTargets) {
            target = canonicalAddress(target);
            if (switchPos.equals(ownerOf(target).orElse(null))) {
                switchByTarget.remove(target);
            }
        }
        for (SmartTargetAddress target : requestedTargets) {
            switchByTarget.put(canonicalAddress(target), switchPos);
        }
        setDirty();
        return true;
    }

    /** Removes every index entry owned by a switch, including stale entries. */
    public void removeSwitch(SmartTargetAddress switchPos) {
        SmartTargetAddress canonicalSwitch = canonicalAddress(switchPos);
        boolean linksChanged = switchByTarget.entrySet().removeIf(entry -> entry.getValue().equals(canonicalSwitch)
                || entry.getValue().matchesFallback(canonicalSwitch));
        boolean signalsChanged = signalBySwitch.entrySet().removeIf(entry -> entry.getKey().equals(canonicalSwitch)
                || entry.getKey().matchesFallback(canonicalSwitch));
        if (linksChanged || signalsChanged) {
            setDirty();
        }
    }

    /** Removes a target and returns the switch that owned it, if any. */
    public Optional<SmartTargetAddress> removeTarget(SmartTargetAddress target) {
        target = canonicalAddress(target);
        SmartTargetAddress owner = ownerOf(target).orElse(null);
        if (owner != null) {
            switchByTarget.remove(target);
        }
        if (owner != null) {
            setDirty();
        }
        return Optional.ofNullable(owner);
    }

    /** Addresses whose backing block currently belongs to the given sublevel. */
    public Set<SmartTargetAddress> addressesInSubLevel(java.util.UUID subLevelId) {
        Set<SmartTargetAddress> addresses = new LinkedHashSet<>();
        switchByTarget.forEach((target, owner) -> {
            addresses.add(target);
            addresses.add(owner);
        });
        addresses.addAll(signalBySwitch.keySet());
        addresses.removeIf(address -> address.subLevel()
                .map(reference -> !reference.id().equals(subLevelId))
                .orElse(true));
        return addresses;
    }

    /**
     * Replaces every reference to an endpoint Sable moved to another sublevel.
     * The remap remains saved so an unloaded Switch repairs its list on load.
     */
    public boolean remapAddress(SmartTargetAddress previous, SmartTargetAddress current) {
        SmartTargetAddress canonicalPrevious = canonicalAddress(previous);
        SmartTargetAddress canonicalCurrent = canonicalAddress(current);
        if (canonicalPrevious.equals(canonicalCurrent)) {
            return false;
        }
        SmartTargetAddress previousOwner = switchByTarget.get(canonicalPrevious);
        SmartTargetAddress currentOwner = switchByTarget.get(canonicalCurrent);
        if (previousOwner != null && currentOwner != null && !previousOwner.equals(currentOwner)) {
            SimplyLightSmart.LOGGER.warn("Refused ambiguous Smart link remap from {} to {}", canonicalPrevious, canonicalCurrent);
            return false;
        }

        remapReferences(switchByTarget, canonicalPrevious, canonicalCurrent);
        remapReferences(signalBySwitch, canonicalPrevious, canonicalCurrent);
        addressRemaps.replaceAll((from, to) -> to.equals(canonicalPrevious) ? canonicalCurrent : to);
        addressRemaps.put(canonicalPrevious, canonicalCurrent);
        setDirty();
        return true;
    }

    /** Returns the current address for a moved endpoint without loading chunks. */
    public SmartTargetAddress canonicalAddress(SmartTargetAddress address) {
        SmartTargetAddress current = address;
        for (int depth = 0; depth < 16; depth++) {
            SmartTargetAddress next = addressRemaps.get(current);
            if (next == null || next.equals(current)) {
                return current;
            }
            current = next;
        }
        return current;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(VERSION_TAG, CURRENT_VERSION);
        ListTag links = new ListTag();
        switchByTarget.forEach((target, switchPos) -> {
            CompoundTag entry = new CompoundTag();
            target.write(entry, "target");
            switchPos.write(entry, "switch");
            links.add(entry);
        });
        tag.put(LINKS_TAG, links);
        ListTag signals = new ListTag();
        signalBySwitch.forEach((switchPos, signal) -> {
            CompoundTag entry = new CompoundTag();
            switchPos.write(entry, "switch");
            entry.putInt(SIGNAL_TAG, signal);
            signals.add(entry);
        });
        tag.put(SWITCH_SIGNALS_TAG, signals);
        ListTag remaps = new ListTag();
        addressRemaps.forEach((from, to) -> {
            CompoundTag entry = new CompoundTag();
            from.write(entry, "from");
            to.write(entry, "to");
            remaps.add(entry);
        });
        tag.put(ADDRESS_REMAPS_TAG, remaps);
        return tag;
    }

    private static <V> void remapReferences(Map<SmartTargetAddress, V> references,
                                            SmartTargetAddress previous, SmartTargetAddress current) {
        Map<SmartTargetAddress, V> remapped = new HashMap<>();
        references.forEach((key, value) -> {
            SmartTargetAddress replacementKey = key.equals(previous) ? current : key;
            @SuppressWarnings("unchecked")
            V replacementValue = value instanceof SmartTargetAddress address && address.equals(previous)
                    ? (V) current : value;
            remapped.put(replacementKey, replacementValue);
        });
        references.clear();
        references.putAll(remapped);
    }

    private SmartTargetAddress migrateTarget(SmartTargetAddress legacy, SmartTargetAddress current, SmartTargetAddress owner) {
        switchByTarget.remove(legacy);
        switchByTarget.put(current, owner);
        setDirty();
        return owner;
    }

    private int migrateSignal(SmartTargetAddress legacy, SmartTargetAddress current, int signal) {
        signalBySwitch.remove(legacy);
        signalBySwitch.put(current, signal);
        setDirty();
        return signal;
    }
}
