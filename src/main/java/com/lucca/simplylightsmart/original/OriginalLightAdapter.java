package com.lucca.simplylightsmart.original;

import com.flanks255.simplylight.blocks.EdgeLight;
import com.flanks255.simplylight.blocks.Fixture;
import com.flanks255.simplylight.blocks.LampBlock;
import com.flanks255.simplylight.blocks.LampPost;
import com.flanks255.simplylight.blocks.LightBulb;
import com.flanks255.simplylight.blocks.RodLamp;
import com.flanks255.simplylight.blocks.ThinLamp;
import com.lucca.simplylightsmart.smart.SmartLinkSavedData;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared contract for applying dimmer state to original Simply Light blocks.
 * The phase activation list is intentionally separate from the full catalogue:
 * it lets each phase enable families without changing their IDs or assets.
 */
public final class OriginalLightAdapter {
    public static final IntegerProperty SMART_POWER = IntegerProperty.create("smart_power", 0, 15);
    /**
     * Native keeps the source block's conventional default. Explicit modes are
     * stored in BlockState so a future Linker action can configure one lamp
     * without a block entity or a second block ID.
     */
    public static final EnumProperty<DimmerMode> SMART_MODE = EnumProperty.create("smart_mode", DimmerMode.class);

    private static final List<FamilyDefinition> FAMILIES = List.of(
            new FamilyDefinition("illuminant_block", LampBlock.class, BrightnessCurve.NATIVE_PAIR),
            new FamilyDefinition("lightbulb", LightBulb.class, BrightnessCurve.INVERTED),
            new FamilyDefinition("thin_lamp", ThinLamp.class, BrightnessCurve.INVERTED),
            new FamilyDefinition("fixture", Fixture.class, BrightnessCurve.INVERTED),
            new FamilyDefinition("illuminant_rod", RodLamp.class, BrightnessCurve.INVERTED),
            new FamilyDefinition("illuminant_edge", EdgeLight.class, BrightnessCurve.INVERTED),
            new FamilyDefinition("illuminant_column", LampPost.class, BrightnessCurve.INVERTED));

    private OriginalLightAdapter() {
    }

    public static Optional<FamilyDefinition> family(BlockState state) {
        return FAMILIES.stream().filter(family -> family.blockType().isInstance(state.getBlock())).findFirst();
    }

    public static boolean hasSmartPower(BlockState state) {
        return state.hasProperty(SMART_POWER);
    }

    /** Phase 3 enables all original families for local redstone control. */
    public static boolean isLocallyEnabled(BlockState state) {
        return hasSmartPower(state) && (state.getBlock() instanceof LampBlock
                || state.getBlock() instanceof LightBulb
                || state.getBlock() instanceof ThinLamp
                || state.getBlock() instanceof Fixture
                || state.getBlock() instanceof RodLamp
                || state.getBlock() instanceof EdgeLight
                || state.getBlock() instanceof LampPost);
    }

    /** Returns the single position that owns control of a supported light. */
    public static BlockPos controlPosition(BlockPos pos, BlockState state) {
        return state.getBlock() instanceof LampPost ? lampPostBase(pos, state) : pos;
    }

    /** Validates a canonical, loaded target before the Smart Switch claims it. */
    public static boolean isValidTarget(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isLocallyEnabled(state) || !controlPosition(pos, state).equals(pos)) {
            return false;
        }
        if (state.getBlock() instanceof LampPost lampPost) {
            return isLampPostPart(state, lampPost, LampPost.Position.BOTTOM)
                    && isLampPostPart(level.getBlockState(pos.above()), lampPost, LampPost.Position.MIDDLE)
                    && isLampPostPart(level.getBlockState(pos.above(2)), lampPost, LampPost.Position.TOP);
        }
        return true;
    }

    /** Supplies the full Column volume to the Linker's selection renderer. */
    public static VoxelShape selectionShape(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos controlPos = controlPosition(pos, state);
        BlockState controlState = level.getBlockState(controlPos);
        if (!(controlState.getBlock() instanceof LampPost lampPost)
                || !isLampPostPart(controlState, lampPost, LampPost.Position.BOTTOM)
                || !isLampPostPart(level.getBlockState(controlPos.above()), lampPost, LampPost.Position.MIDDLE)
                || !isLampPostPart(level.getBlockState(controlPos.above(2)), lampPost, LampPost.Position.TOP)) {
            return state.getShape(level, pos);
        }
        return Shapes.or(
                controlState.getShape(level, controlPos),
                level.getBlockState(controlPos.above()).getShape(level, controlPos.above()).move(0, 1, 0),
                level.getBlockState(controlPos.above(2)).getShape(level, controlPos.above(2)).move(0, 2, 0));
    }

    public static void refreshLocal(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!isLocallyEnabled(state)) {
            return;
        }
        if (state.getBlock() instanceof LampPost) {
            refreshLampPost(level, pos, state);
            return;
        }
        applyEmission(level, pos, state, emissionForSignal(state, controlSignal(level, pos)));
    }

    /**
     * A Lamp Post is one lamp composed of bottom, middle and top BlockStates.
     * The bottom receives redstone; every matching segment carries the same
     * persisted value, while the native top segment remains the sole emitter.
     */
    private static void refreshLampPost(Level level, BlockPos pos, BlockState state) {
        BlockPos basePos = lampPostBase(pos, state);
        BlockState baseState = level.getBlockState(basePos);
        if (!(baseState.getBlock() instanceof LampPost lampPost)
                || !isLampPostPart(baseState, lampPost, LampPost.Position.BOTTOM)
                || !isLampPostPart(level.getBlockState(basePos.above()), lampPost, LampPost.Position.MIDDLE)
                || !isLampPostPart(level.getBlockState(basePos.above(2)), lampPost, LampPost.Position.TOP)) {
            return;
        }

        int emission = emissionForSignal(baseState, controlSignal(level, basePos));
        applyEmission(level, basePos, baseState, emission);
        applyEmission(level, basePos.above(), level.getBlockState(basePos.above()), emission);
        applyEmission(level, basePos.above(2), level.getBlockState(basePos.above(2)), emission);
    }

    private static boolean isLampPostPart(BlockState state, LampPost lampPost, LampPost.Position position) {
        return state.is(lampPost)
                && hasSmartPower(state)
                && state.getValue(LampPost.POSITION) == position;
    }

    private static BlockPos lampPostBase(BlockPos pos, BlockState state) {
        return switch (state.getValue(LampPost.POSITION)) {
            case BOTTOM -> pos;
            case MIDDLE -> pos.below();
            case TOP -> pos.below(2);
        };
    }

    /** Reads a linked Switch when present, otherwise the block's local redstone. */
    private static int controlSignal(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return level.getBestNeighborSignal(pos);
        }
        GlobalPos target = SmartSwitchBlockEntity.globalPos(serverLevel, pos);
        Optional<GlobalPos> owner = SmartLinkSavedData.get(serverLevel).ownerOf(target);
        return owner.map(switchPos -> SmartLinkSavedData.get(serverLevel).signalOf(switchPos))
                .orElseGet(() -> level.getBestNeighborSignal(pos));
    }

    /** Removes a canonical original-light target from its Switch on break. */
    public static void removeTarget(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !isLocallyEnabled(state)) {
            return;
        }
        BlockPos controlPos = controlPosition(pos, state);
        SmartLinkSavedData.get(serverLevel)
                .removeTarget(SmartSwitchBlockEntity.globalPos(serverLevel, controlPos))
                .ifPresent(owner -> {
                    ServerLevel switchLevel = serverLevel.getServer().getLevel(owner.dimension());
                    if (switchLevel != null && switchLevel.hasChunkAt(owner.pos())
                            && switchLevel.getBlockEntity(owner.pos()) instanceof SmartSwitchBlockEntity switchEntity) {
                        switchEntity.forgetTarget(SmartSwitchBlockEntity.globalPos(serverLevel, controlPos));
                    }
                });
    }

    /** Writes only smart_power and the native Illuminant Block visual state. */
    public static void applyEmission(Level level, BlockPos pos, BlockState state, int emission) {
        if (level.isClientSide || !hasSmartPower(state)) {
            return;
        }
        BlockState updated = state.setValue(SMART_POWER, Math.clamp(emission, 0, 15));
        if (updated.getBlock() instanceof LampBlock) {
            updated = updated.setValue(LampBlock.ON, updated.getValue(SMART_POWER) > 0);
        }
        if (updated != state) {
            level.setBlockAndUpdate(pos, updated);
        }
    }

    public static int emissionForSignal(BlockState state, int signal) {
        int clampedSignal = Math.clamp(signal, 0, 15);
        return switch (state.getValue(SMART_MODE)) {
            case DEFAULT_ON -> 15 - clampedSignal;
            case DEFAULT_OFF -> clampedSignal;
            case NATIVE -> nativeEmissionForSignal(state, clampedSignal);
        };
    }

    private static int nativeEmissionForSignal(BlockState state, int signal) {
        FamilyDefinition family = family(state).orElseThrow();
        if (family.curve() == BrightnessCurve.NATIVE_PAIR
                && state.getBlock() instanceof LampBlock lampBlock
                && !lampBlock.Default) {
            return signal;
        }
        return 15 - signal;
    }

    public enum BrightnessCurve {
        INVERTED,
        NATIVE_PAIR
    }

    public enum DimmerMode implements StringRepresentable {
        NATIVE("native"),
        DEFAULT_ON("default_on"),
        DEFAULT_OFF("default_off");

        private final String serializedName;

        DimmerMode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public record FamilyDefinition(String id, Class<? extends Block> blockType, BrightnessCurve curve) {
    }
}
