package com.lucca.simplylightsmart.smart;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Rehomes persisted addresses when Sable separates blocks into a sublevel. */
public final class SableSplitLinkMigration {
    private static final String COMPANION_CLASS = "dev.ryanhcode.sable.companion.SableCompanion";

    private static Bridge bridge;
    private static boolean lookupAttempted;

    private SableSplitLinkMigration() {
    }

    /**
     * Runs from Sable's {@code ServerSubLevel#setSplitFrom}. The blocks have
     * already moved, but the source transform has not been discarded yet.
     * This updates metadata only; it never requests chunks or writes blocks.
     */
    public static void onSplit(Object splitSubLevel, Object containingSubLevel, Object originalPose) {
        Bridge activeBridge = bridge();
        if (activeBridge == null) {
            return;
        }
        try {
            if (!(activeBridge.getLevel.invoke(splitSubLevel) instanceof ServerLevel level)) {
                return;
            }
            UUID sourceId = (UUID) activeBridge.getUniqueId.invoke(containingSubLevel);
            UUID splitId = (UUID) activeBridge.getUniqueId.invoke(splitSubLevel);
            SmartLinkSavedData links = SmartLinkSavedData.get(level);

            for (SmartTargetAddress previous : links.addressesInSubLevel(sourceId)) {
                BlockPos sourcePosition = absolutePosition(previous, containingSubLevel, activeBridge);
                if (sourcePosition == null) {
                    continue;
                }
                Vec3 splitPosition = (Vec3) activeBridge.transformPositionInverse.invoke(
                        originalPose, Vec3.atCenterOf(sourcePosition));
                BlockPos candidate = BlockPos.containing(splitPosition);
                Object candidateSubLevel = activeBridge.getContaining.invoke(activeBridge.companion, level, candidate);
                if (candidateSubLevel != splitSubLevel || !isLinkEndpoint(level, candidate)) {
                    continue;
                }

                SmartTargetAddress current = SmartTargetAddress.capture(level, candidate);
                if (current.subLevel().filter(reference -> reference.id().equals(splitId)).isEmpty()) {
                    continue;
                }
                links.remapAddress(previous, current);
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError exception) {
            SimplyLightSmart.LOGGER.debug("Could not migrate Smart links after a Sable split", exception);
        }
    }

    private static BlockPos absolutePosition(SmartTargetAddress address, Object subLevel, Bridge bridge)
            throws ReflectiveOperationException {
        if (address.subLevel().isEmpty()) {
            return null;
        }
        Object plot = bridge.getPlot.invoke(subLevel);
        net.minecraft.world.level.ChunkPos chunkMin = (net.minecraft.world.level.ChunkPos) bridge.getChunkMin.invoke(plot);
        BlockPos local = address.subLevel().get().localPos();
        return new BlockPos(chunkMin.getMinBlockX() + local.getX(), local.getY(), chunkMin.getMinBlockZ() + local.getZ());
    }

    private static boolean isLinkEndpoint(ServerLevel level, BlockPos position) {
        return OriginalLightAdapter.isValidTarget(level, position)
                || level.getBlockEntity(position) instanceof SmartSwitchBlockEntity;
    }

    private static Bridge bridge() {
        if (lookupAttempted) {
            return bridge;
        }
        lookupAttempted = true;
        try {
            ClassLoader loader = SableSplitLinkMigration.class.getClassLoader();
            Class<?> companionClass = Class.forName(COMPANION_CLASS, false, loader);
            Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel", false, loader);
            Class<?> plotClass = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot", false, loader);
            Class<?> poseClass = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc", false, loader);
            Field instance = companionClass.getField("INSTANCE");
            bridge = new Bridge(
                    instance.get(null),
                    companionClass.getMethod("getContaining", Level.class, net.minecraft.core.Position.class),
                    subLevelClass.getMethod("getLevel"),
                    subLevelClass.getMethod("getUniqueId"),
                    subLevelClass.getMethod("getPlot"),
                    plotClass.getMethod("getChunkMin"),
                    poseClass.getMethod("transformPositionInverse", Vec3.class));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            bridge = null;
        }
        return bridge;
    }

    private record Bridge(Object companion, Method getContaining, Method getLevel, Method getUniqueId,
                          Method getPlot, Method getChunkMin, Method transformPositionInverse) {
    }
}
