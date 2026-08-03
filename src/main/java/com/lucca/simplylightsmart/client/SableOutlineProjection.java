package com.lucca.simplylightsmart.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Optional client bridge for Sable's render-space transform.
 *
 * <p>Sable is deliberately accessed through reflection: the addon remains
 * loadable without it and does not need a compile-time dependency on the
 * companion library. The reflected API is Sable Companion 1.6.0, bundled by
 * Sable 2.0.3 in the supported instance.</p>
 */
final class SableOutlineProjection {
    private static final String COMPANION_CLASS = "dev.ryanhcode.sable.companion.SableCompanion";
    private static final String CLIENT_ACCESS_CLASS = "dev.ryanhcode.sable.companion.ClientSubLevelAccess";
    private static final String POSE_CLASS = "dev.ryanhcode.sable.companion.math.Pose3dc";

    private static Bridge bridge;
    private static boolean lookupAttempted;

    private SableOutlineProjection() {
    }

    /** Returns an axis-aligned render-space bound for the selected block. */
    static AABB projectBounds(ClientLevel level, AABB bounds, float partialTick) {
        Bridge activeBridge = bridge();
        if (activeBridge == null) {
            return bounds;
        }

        try {
            Object subLevel = activeBridge.getContainingClient.invoke(activeBridge.companion, bounds.getCenter().x, bounds.getCenter().z);
            if (subLevel == null) {
                return bounds;
            }

            Object pose = activeBridge.renderPose.invoke(subLevel, partialTick);
            Vec3 first = project(activeBridge, pose, bounds.minX, bounds.minY, bounds.minZ);
            AABB projected = new AABB(first, first);
            for (int x = 0; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = 0; z <= 1; z++) {
                        Vec3 point = project(activeBridge, pose,
                                x == 0 ? bounds.minX : bounds.maxX,
                                y == 0 ? bounds.minY : bounds.maxY,
                                z == 0 ? bounds.minZ : bounds.maxZ);
                        projected = projected.minmax(new AABB(point, point));
                    }
                }
            }
            return projected;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // A different Sable version must never prevent normal outlines.
            return bounds;
        }
    }

    private static Vec3 project(Bridge activeBridge, Object pose, double x, double y, double z)
            throws ReflectiveOperationException {
        return (Vec3) activeBridge.transformPosition.invoke(pose, new Vec3(x, y, z));
    }

    private static Bridge bridge() {
        if (lookupAttempted) {
            return bridge;
        }
        lookupAttempted = true;

        try {
            ClassLoader loader = SableOutlineProjection.class.getClassLoader();
            Class<?> companionClass = Class.forName(COMPANION_CLASS, false, loader);
            Class<?> clientAccessClass = Class.forName(CLIENT_ACCESS_CLASS, false, loader);
            Class<?> poseClass = Class.forName(POSE_CLASS, false, loader);
            Field instance = companionClass.getField("INSTANCE");
            bridge = new Bridge(
                    instance.get(null),
                    companionClass.getMethod("getContainingClient", double.class, double.class),
                    clientAccessClass.getMethod("renderPose", float.class),
                    poseClass.getMethod("transformPosition", Vec3.class));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            bridge = null;
        }
        return bridge;
    }

    private record Bridge(Object companion, Method getContainingClient, Method renderPose, Method transformPosition) {
    }
}
