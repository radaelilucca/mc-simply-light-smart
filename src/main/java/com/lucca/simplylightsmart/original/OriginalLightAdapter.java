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
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Shared contract for applying dimmer state to original Simply Light blocks.
 * The phase activation list is intentionally separate from the full catalogue:
 * it lets each phase enable families without changing their IDs or assets.
 */
public final class OriginalLightAdapter {
    public static final IntegerProperty SMART_POWER = IntegerProperty.create("smart_power", 0, 15);

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

    /** Only the confirmed Lightbulb spike is active until phase 2 enables simple families. */
    public static boolean isLocallyEnabled(BlockState state) {
        return state.getBlock() instanceof LightBulb && hasSmartPower(state);
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
        FamilyDefinition family = family(state).orElseThrow();
        if (family.curve() == BrightnessCurve.NATIVE_PAIR
                && state.getBlock() instanceof LampBlock lampBlock
                && !lampBlock.Default) {
            return clampedSignal;
        }
        return 15 - clampedSignal;
    }

    public enum BrightnessCurve {
        INVERTED,
        NATIVE_PAIR
    }

    public record FamilyDefinition(String id, Class<? extends Block> blockType, BrightnessCurve curve) {
    }
}
