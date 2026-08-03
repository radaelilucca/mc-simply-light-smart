package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity registrations for the addon. */
public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimplyLightSmart.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmartSwitchBlockEntity>> SMART_SWITCH =
            BLOCK_ENTITY_TYPES.register("smart_switch", () ->
                    BlockEntityType.Builder.of(SmartSwitchBlockEntity::new, ModBlocks.SMART_SWITCH.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
