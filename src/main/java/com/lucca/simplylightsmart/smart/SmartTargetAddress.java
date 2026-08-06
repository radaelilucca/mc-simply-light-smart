package com.lucca.simplylightsmart.smart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Persistent address of a dimmer or Switch.
 *
 * <p>Normal worlds use only {@link #fallback}. When Sable owns the position,
 * the address also carries the sublevel UUID and a plot-local position. The
 * fallback keeps legacy worlds and installations without Sable compatible.
 * </p>
 */
public record SmartTargetAddress(GlobalPos fallback, Optional<SubLevelReference> subLevel) {
    private static final String COMPANION_CLASS = "dev.ryanhcode.sable.companion.SableCompanion";
    private static final String CONTAINER_CLASS = "dev.ryanhcode.sable.api.sublevel.SubLevelContainer";
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Invalid sublevel UUID: " + value);
        }
    }, UUID::toString);
    private static final Codec<SubLevelReference> SUBLEVEL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("id").forGetter(SubLevelReference::id),
            Codec.LONG.fieldOf("local_position").xmap(BlockPos::of, BlockPos::asLong).forGetter(SubLevelReference::localPos))
            .apply(instance, SubLevelReference::new));
    public static final Codec<SmartTargetAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.fieldOf("fallback").forGetter(SmartTargetAddress::fallback),
            SUBLEVEL_CODEC.optionalFieldOf("sublevel").forGetter(SmartTargetAddress::subLevel))
            .apply(instance, SmartTargetAddress::new));

    private static Bridge bridge;
    private static boolean lookupAttempted;

    public SmartTargetAddress {
        subLevel = subLevel == null ? Optional.empty() : subLevel;
    }

    /** Captures a stable Sable address when the given position belongs to a sublevel. */
    public static SmartTargetAddress capture(ServerLevel level, BlockPos pos) {
        GlobalPos fallback = GlobalPos.of(level.dimension(), pos);
        Bridge activeBridge = bridge();
        if (activeBridge == null) {
            return new SmartTargetAddress(fallback, Optional.empty());
        }
        try {
            Object subLevel = activeBridge.getContaining.invoke(activeBridge.companion, level, pos);
            if (subLevel == null) {
                return new SmartTargetAddress(fallback, Optional.empty());
            }
            UUID id = (UUID) activeBridge.getUniqueId.invoke(subLevel);
            Object plot = activeBridge.getPlot.invoke(subLevel);
            ChunkPos chunkMin = (ChunkPos) activeBridge.getChunkMin.invoke(plot);
            BlockPos local = new BlockPos(pos.getX() - chunkMin.getMinBlockX(), pos.getY(), pos.getZ() - chunkMin.getMinBlockZ());
            return new SmartTargetAddress(fallback, Optional.of(new SubLevelReference(id, local)));
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
            return new SmartTargetAddress(fallback, Optional.empty());
        }
    }

    /** Resolves a persisted address without loading a chunk. */
    public Optional<GlobalPos> resolve(ServerLevel sourceLevel) {
        if (subLevel.isEmpty()) {
            return Optional.of(fallback);
        }
        Bridge activeBridge = bridge();
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(fallback.dimension());
        if (activeBridge == null || targetLevel == null) {
            return Optional.empty();
        }
        try {
            Object container = activeBridge.getContainer.invoke(null, targetLevel);
            Object resolvedSubLevel = activeBridge.getSubLevel.invoke(container, subLevel.get().id());
            if (resolvedSubLevel == null) {
                return Optional.empty();
            }
            Object plot = activeBridge.getPlot.invoke(resolvedSubLevel);
            ChunkPos chunkMin = (ChunkPos) activeBridge.getChunkMin.invoke(plot);
            BlockPos local = subLevel.get().localPos();
            return Optional.of(GlobalPos.of(fallback.dimension(), new BlockPos(
                    chunkMin.getMinBlockX() + local.getX(), local.getY(), chunkMin.getMinBlockZ() + local.getZ())));
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    public boolean matchesFallback(SmartTargetAddress other) {
        return fallback.equals(other.fallback);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SmartTargetAddress other)) {
            return false;
        }
        if (subLevel.isPresent() && other.subLevel.isPresent()) {
            return fallback.dimension().equals(other.fallback.dimension()) && subLevel.equals(other.subLevel);
        }
        return fallback.equals(other.fallback);
    }

    @Override
    public int hashCode() {
        return subLevel.map(reference -> 31 * fallback.dimension().hashCode() + reference.hashCode())
                .orElse(fallback.hashCode());
    }

    public void write(CompoundTag tag, String prefix) {
        tag.putString(key(prefix, "dimension"), fallback.dimension().location().toString());
        tag.putLong(key(prefix, "position"), fallback.pos().asLong());
        subLevel.ifPresent(reference -> {
            tag.putString(key(prefix, "sublevel"), reference.id().toString());
            tag.putLong(key(prefix, "local_position"), reference.localPos().asLong());
        });
    }

    public static Optional<SmartTargetAddress> read(CompoundTag tag, String prefix) {
        String dimensionKey = key(prefix, "dimension");
        String positionKey = key(prefix, "position");
        if (!tag.contains(dimensionKey) || !tag.contains(positionKey)) {
            return Optional.empty();
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(dimensionKey));
        if (dimensionId == null) {
            return Optional.empty();
        }
        GlobalPos fallback = GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId),
                BlockPos.of(tag.getLong(positionKey)));
        String subLevelKey = key(prefix, "sublevel");
        String localPositionKey = key(prefix, "local_position");
        if (!tag.contains(subLevelKey) || !tag.contains(localPositionKey)) {
            return Optional.of(new SmartTargetAddress(fallback, Optional.empty()));
        }
        try {
            return Optional.of(new SmartTargetAddress(fallback,
                    Optional.of(new SubLevelReference(UUID.fromString(tag.getString(subLevelKey)), BlockPos.of(tag.getLong(localPositionKey))))));
        } catch (IllegalArgumentException exception) {
            return Optional.of(new SmartTargetAddress(fallback, Optional.empty()));
        }
    }

    private static String key(String prefix, String suffix) {
        return prefix.isEmpty() ? suffix : prefix + "_" + suffix;
    }

    private static Bridge bridge() {
        if (lookupAttempted) {
            return bridge;
        }
        lookupAttempted = true;
        try {
            ClassLoader loader = SmartTargetAddress.class.getClassLoader();
            Class<?> companionClass = Class.forName(COMPANION_CLASS, false, loader);
            Class<?> containerClass = Class.forName(CONTAINER_CLASS, false, loader);
            Field instance = companionClass.getField("INSTANCE");
            Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel", false, loader);
            Class<?> plotClass = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot", false, loader);
            bridge = new Bridge(
                    instance.get(null),
                    companionClass.getMethod("getContaining", Level.class, net.minecraft.core.Position.class),
                    subLevelClass.getMethod("getUniqueId"),
                    subLevelClass.getMethod("getPlot"),
                    plotClass.getMethod("getChunkMin"),
                    containerClass.getMethod("getContainer", ServerLevel.class),
                    containerClass.getMethod("getSubLevel", UUID.class));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            bridge = null;
        }
        return bridge;
    }

    public record SubLevelReference(UUID id, BlockPos localPos) {
    }

    private record Bridge(Object companion, Method getContaining, Method getUniqueId, Method getPlot, Method getChunkMin,
                          Method getContainer, Method getSubLevel) {
    }
}
