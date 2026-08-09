package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative-mode tab containing the blocks added by this addon. */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimplyLightSmart.MOD_ID);

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.simply_light_smart.main"))
                .icon(ModBlocks::creativeTabIcon)
                .displayItems((parameters, output) -> {
                    ModBlocks.addControlItems(output);
                    output.accept(ModItems.SMART_LINKER.get());
                    output.accept(ModItems.SMART_DEBUGGER.get());
                })
                .build());
    }

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}
