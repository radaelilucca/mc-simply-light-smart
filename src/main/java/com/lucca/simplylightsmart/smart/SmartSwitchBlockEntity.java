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

    private final Set<SmartTargetAddress> linkedTargets = new LinkedHashSet<>();
    private int signal;

    public SmartSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMART_SWITCH.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            DeferredLightWork.enqueueSwitchInitialization(serverLevel, worldPosition, this);
        }
    }

    /** Invoked by deferred work only after the chunk-restoration tick. */
    public void initializeAfterLoad() {
        refreshSignal();
        enqueueTargetRefreshes(linkedTargets);
    }

    public int signal() {
        return signal;
    }

    public Set<SmartTargetAddress> linkedTargets() {
        return Set.copyOf(linkedTargets);
    }

    /** Called by the linker after it has collected the player's targets. */
    public LinkApplicationResult applyLinks(ServerPlayer player, List<SmartTargetAddress> requestedTargets) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return LinkApplicationResult.failure("not_server");
        }
        if (requestedTargets.size() > MAX_LINKS) {
            return LinkApplicationResult.failure("too_many_targets");
        }

        SmartTargetAddress switchPos = address(serverLevel, worldPosition);
        LinkedHashSet<SmartTargetAddress> uniqueTargets = new LinkedHashSet<>(requestedTargets);
        if (uniqueTargets.size() != requestedTargets.size()) {
            return LinkApplicationResult.failure("duplicate_target");
        }
        for (SmartTargetAddress target : uniqueTargets) {
            GlobalPos resolvedTarget = target.resolve(serverLevel).orElse(null);
            if (resolvedTarget == null || !resolvedTarget.dimension().equals(serverLevel.dimension())) {
                return LinkApplicationResult.failure("different_dimension");
            }
            if (!isWithinLinkRange(serverLevel, worldPosition, resolvedTarget)) {
                return LinkApplicationResult.failure("out_of_range");
            }
            ServerLevel targetLevel = serverLevel.getServer().getLevel(resolvedTarget.dimension());
            LevelChunk targetChunk = targetLevel == null ? null : targetLevel.getChunkSource().getChunkNow(
                    resolvedTarget.pos().getX() >> 4, resolvedTarget.pos().getZ() >> 4);
            if (targetChunk == null || !isValidTarget(targetLevel, resolvedTarget.pos())) {
                return LinkApplicationResult.failure("invalid_target");
            }
        }

        SmartLinkSavedData links = SmartLinkSavedData.get(serverLevel);
        if (!links.replaceTargets(switchPos, linkedTargets, uniqueTargets)) {
            return LinkApplicationResult.failure("target_already_linked");
        }

        Set<SmartTargetAddress> previousTargets = Set.copyOf(linkedTargets);
        linkedTargets.clear();
        linkedTargets.addAll(uniqueTargets);
        setChanged();
        enqueueTargetRefreshes(previousTargets);
        enqueueTargetRefreshes(linkedTargets);
        return LinkApplicationResult.success(linkedTargets.size());
    }

    /** Clears ownership and restores local redstone for loaded former targets. */
    public void clearLinks() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Set<SmartTargetAddress> previousTargets = Set.copyOf(linkedTargets);
        SmartLinkSavedData.get(serverLevel).replaceTargets(address(serverLevel, worldPosition), previousTargets, List.of());
        linkedTargets.clear();
        setChanged();
        enqueueTargetRefreshes(previousTargets);
    }

    /** Called when a linked dimmer is destroyed before the switch is unloaded. */
    public void forgetTarget(SmartTargetAddress target) {
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
        SmartLinkSavedData.get(serverLevel).setSwitchSignal(address(serverLevel, worldPosition), nextSignal);
        if (signal != nextSignal) {
            signal = nextSignal;
            setChanged();
            enqueueTargetRefreshes(linkedTargets);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("signal", signal);
        ListTag targets = new ListTag();
        for (SmartTargetAddress target : linkedTargets) {
            CompoundTag targetTag = new CompoundTag();
            target.write(targetTag, "");
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
            SmartTargetAddress.read(target, "").ifPresent(linkedTargets::add);
        }
    }

    public static SmartTargetAddress address(ServerLevel level, BlockPos pos) {
        return SmartTargetAddress.capture(level, pos);
    }

    /** Measures the physical range through Sable Companion when it is present. */
    public static boolean isWithinLinkRange(ServerLevel switchLevel, BlockPos switchPos, GlobalPos target) {
        return SableLinkDistance.distanceSquared(switchLevel, switchPos, target.pos())
                <= MAX_LINK_DISTANCE * MAX_LINK_DISTANCE;
    }

    /** Queues linked targets and never forces their chunks to load. */
    private void enqueueTargetRefreshes(Iterable<SmartTargetAddress> targets) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (SmartTargetAddress target : targets) {
            target.resolve(serverLevel).ifPresent(resolved -> DeferredLightWork.enqueueLinkedTargetRefresh(serverLevel, resolved));
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
