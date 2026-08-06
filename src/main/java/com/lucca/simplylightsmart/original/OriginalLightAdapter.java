package com.lucca.simplylightsmart.original;

import com.flanks255.simplylight.blocks.EdgeLight;
import com.flanks255.simplylight.blocks.Fixture;
import com.flanks255.simplylight.blocks.LampBlock;
import com.flanks255.simplylight.blocks.LampPost;
import com.flanks255.simplylight.blocks.LightBulb;
import com.flanks255.simplylight.blocks.RodLamp;
import com.flanks255.simplylight.blocks.ThinLamp;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.StringRepresentable;

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

    /** Phase 2 enables original simple families; connected geometry stays for phase 3. */
    public static boolean isLocallyEnabled(BlockState state) {
        return hasSmartPower(state) && (state.getBlock() instanceof LampBlock
                || state.getBlock() instanceof LightBulb
                || state.getBlock() instanceof ThinLamp
                || state.getBlock() instanceof Fixture);
    }

    public static void refreshLocal(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!isLocallyEnabled(state)) {
            return;
        }
        applyEmission(level, pos, state, emissionForSignal(state, level.getBestNeighborSignal(pos)));
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
