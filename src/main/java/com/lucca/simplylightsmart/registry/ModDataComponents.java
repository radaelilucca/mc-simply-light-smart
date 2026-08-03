package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.smart.SmartLinkerSelection;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data components used by items added by this addon. */
public final class ModDataComponents {
    private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE, SimplyLightSmart.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SmartLinkerSelection>> LINKER_SELECTION =
            COMPONENTS.registerComponentType("linker_selection", builder -> builder
                    .persistent(SmartLinkerSelection.CODEC)
                    .networkSynchronized(SmartLinkerSelection.STREAM_CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
