package com.lucca.simplylightsmart.smart;

import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import com.lucca.simplylightsmart.original.DeferredLightWork;
import com.lucca.simplylightsmart.registry.ModBlockEntities;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Persists and updates the dimmers controlled by a single Smart Switch. */
public final class SmartSwitchBlockEntity extends BlockEntity {
    public static final int MAX_LINKS = 64;
    public static final double MAX_LINK_DISTANCE = 128.0D;
    private static final String TARGETS_TAG = "targets";
    private static final String DIMENSION_TAG = "dimension";
    private static final String POSITION_TAG = "position";

    private final Set<GlobalPos> linkedTargets = new LinkedHashSet<>();
    private int signal;

    public SmartSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_SWITCH.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            SmartLinkSavedData.get(serverLevel).setSwitchSignal(globalPos(serverLevel, worldPosition), signal);
            DeferredLightWork.enqueueSwitchInitialization(serverLevel, worldPosition, this);
        }
    }

    /** Invoked by deferred work only after the chunk-restoration tick. */
    public void initializeAfterLoad() {
        refreshSignal();
        refreshTargets(linkedTargets);
    }

    public int signal() {
        return signal;
    }

    public Set<GlobalPos> linkedTargets() {
        return Set.copyOf(linkedTargets);
    }

    /** Called by the linker after it has collected the player's targets. */
    public LinkApplicationResult applyLinks(ServerPlayer player, List<GlobalPos> requestedTargets) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return LinkApplicationResult.failure("not_server");
        }
        if (requestedTargets.size() > MAX_LINKS) {
            return LinkApplicationResult.failure("too_many_targets");
        }

        GlobalPos switchPos = globalPos(serverLevel, worldPosition);
        LinkedHashSet<GlobalPos> uniqueTargets = new LinkedHashSet<>(requestedTargets);
        if (uniqueTargets.size() != requestedTargets.size()) {
            return LinkApplicationResult.failure("duplicate_target");
        }
        for (GlobalPos target : uniqueTargets) {
            if (!target.dimension().equals(serverLevel.dimension())) {
                return LinkApplicationResult.failure("different_dimension");
            }
            if (!isWithinLinkRange(serverLevel, worldPosition, target)) {
                return LinkApplicationResult.failure("out_of_range");
            }
            ServerLevel targetLevel = serverLevel.getServer().getLevel(target.dimension());
            LevelChunk targetChunk = targetLevel == null ? null : targetLevel.getChunkSource().getChunkNow(
                    target.pos().getX() >> 4, target.pos().getZ() >> 4);
            if (targetChunk == null || !isValidTarget(targetLevel, target.pos())) {
                return LinkApplicationResult.failure("invalid_target");
            }
        }

        SmartLinkSavedData links = SmartLinkSavedData.get(serverLevel);
        if (!links.replaceTargets(switchPos, linkedTargets, uniqueTargets)) {
            return LinkApplicationResult.failure("target_already_linked");
        }

        Set<GlobalPos> previousTargets = Set.copyOf(linkedTargets);
        linkedTargets.clear();
        linkedTargets.addAll(uniqueTargets);
        setChanged();
        refreshTargets(previousTargets);
        refreshTargets(linkedTargets);
        return LinkApplicationResult.success(linkedTargets.size());
    }

    /** Clears ownership and restores local redstone for loaded former targets. */
    public void clearLinks() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Set<GlobalPos> previousTargets = Set.copyOf(linkedTargets);
        SmartLinkSavedData.get(serverLevel).replaceTargets(globalPos(serverLevel, worldPosition), previousTargets, List.of());
        linkedTargets.clear();
        setChanged();
        refreshTargets(previousTargets);
    }

    /** Called when a linked dimmer is destroyed before the switch is unloaded. */
    public void forgetTarget(GlobalPos target) {
        if (linkedTargets.remove(target)) {
            setChanged();
        }
    }

    /** Re-reads local redstone and updates all loaded linked dimmers when it changed. */
    public void refreshSignal() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int nextSignal = serverLevel.getBestNeighborSignal(worldPosition);
        SmartLinkSavedData.get(serverLevel).setSwitchSignal(globalPos(serverLevel, worldPosition), nextSignal);
        if (signal != nextSignal) {
            signal = nextSignal;
            setChanged();
            refreshTargets(linkedTargets);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("signal", signal);
        ListTag targets = new ListTag();
        for (GlobalPos target : linkedTargets) {
            CompoundTag targetTag = new CompoundTag();
            targetTag.putString(DIMENSION_TAG, target.dimension().location().toString());
            targetTag.putLong(POSITION_TAG, target.pos().asLong());
            targets.add(targetTag);
        }
        tag.put(TARGETS_TAG, targets);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signal = tag.getInt("signal");
        linkedTargets.clear();
        ListTag targets = tag.getList(TARGETS_TAG, CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < targets.size(); index++) {
            CompoundTag target = targets.getCompound(index);
            ResourceLocation dimensionId = ResourceLocation.tryParse(target.getString(DIMENSION_TAG));
            if (dimensionId != null && target.contains(POSITION_TAG)) {
                linkedTargets.add(GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId),
                        BlockPos.of(target.getLong(POSITION_TAG))));
            }
        }
    }

    public static GlobalPos globalPos(ServerLevel level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos);
    }

    /** Measures the physical range through Sable Companion when it is present. */
    public static boolean isWithinLinkRange(ServerLevel switchLevel, BlockPos switchPos, GlobalPos target) {
        return SableLinkDistance.distanceSquared(switchLevel, switchPos, target.pos())
                <= MAX_LINK_DISTANCE * MAX_LINK_DISTANCE;
    }

    private void refreshTargets(Iterable<GlobalPos> targets) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (GlobalPos target : targets) {
            ServerLevel targetLevel = serverLevel.getServer().getLevel(target.dimension());
            LevelChunk targetChunk = targetLevel == null ? null : targetLevel.getChunkSource().getChunkNow(
                    target.pos().getX() >> 4, target.pos().getZ() >> 4);
            if (targetChunk == null) {
                continue;
            }
            if (OriginalLightAdapter.isValidTarget(targetLevel, target.pos())) {
                OriginalLightAdapter.refreshLocal(targetLevel, target.pos());
            }
        }
    }

    private static boolean isValidTarget(ServerLevel level, BlockPos pos) {
        return OriginalLightAdapter.isValidTarget(level, pos);
    }

    public record LinkApplicationResult(boolean success, String reason, int linkedCount) {
        public static LinkApplicationResult success(int linkedCount) {
            return new LinkApplicationResult(true, "ok", linkedCount);
        }

        public static LinkApplicationResult failure(String reason) {
            return new LinkApplicationResult(false, reason, 0);
        }
    }
}
