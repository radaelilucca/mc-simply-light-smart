package com.lucca.simplylightsmart.smart;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Optional server-side bridge to Sable Companion's physical-distance API.
 *
 * <p>The companion is loaded by Sable 2.0.3 itself. Reflection keeps the
 * addon entirely functional when Sable is absent, while allowing positions in
 * separate sublevels to be compared in world space when it is present.</p>
 */
final class SableLinkDistance {
    private static final String COMPANION_CLASS = "dev.ryanhcode.sable.companion.SableCompanion";
    private static Bridge bridge;
    private static boolean lookupAttempted;

    private SableLinkDistance() {
    }

    static double distanceSquared(ServerLevel level, BlockPos first, BlockPos second) {
        Bridge active = bridge();
        if (active == null) {
            return first.distSqr(second);
        }

        try {
            return (double) active.distanceSquared.invoke(
                    active.companion, level, Vec3.atCenterOf(first), Vec3.atCenterOf(second));
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
            return first.distSqr(second);
        }
    }

    private static Bridge bridge() {
        if (lookupAttempted) {
            return bridge;
        }
        lookupAttempted = true;

        try {
            Class<?> companionClass = Class.forName(COMPANION_CLASS, false, SableLinkDistance.class.getClassLoader());
            Field instance = companionClass.getField("INSTANCE");
            Method distanceSquared = companionClass.getMethod(
                    "distanceSquaredWithSubLevels", net.minecraft.world.level.Level.class, Position.class, Position.class);
            bridge = new Bridge(instance.get(null), distanceSquared);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            bridge = null;
        }
        return bridge;
    }

    private record Bridge(Object companion, Method distanceSquared) {
    }
}
