package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.item.SmartLinkerItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Standalone item registrations that do not belong to a block. */
public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimplyLightSmart.MOD_ID);

    public static final DeferredItem<SmartLinkerItem> SMART_LINKER = ITEMS.registerItem(
            "smart_linker", SmartLinkerItem::new);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
