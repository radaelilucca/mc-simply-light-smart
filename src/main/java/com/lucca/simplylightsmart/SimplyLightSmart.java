package com.lucca.simplylightsmart;

import com.lucca.simplylightsmart.registry.ModBlocks;
import com.lucca.simplylightsmart.registry.ModBlockEntities;
import com.lucca.simplylightsmart.registry.ModAttachments;
import com.lucca.simplylightsmart.registry.ModCreativeTabs;
import com.lucca.simplylightsmart.registry.ModDataComponents;
import com.lucca.simplylightsmart.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Main entry point for Simply Light - Smart!. */
@Mod(SimplyLightSmart.MOD_ID)
public final class SimplyLightSmart {
    public static final String MOD_ID = "simply_light_smart";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static String version() {
        return ModList.get().getModContainerById(MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    public SimplyLightSmart(IEventBus modEventBus) {
        ModAttachments.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        LOGGER.info("Simply Light - Smart! loaded");
    }
}
