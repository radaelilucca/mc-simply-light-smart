package com.lucca.simplylightsmart;

import com.lucca.simplylightsmart.registry.ModBlocks;
import com.lucca.simplylightsmart.registry.ModCreativeTabs;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Main entry point for Simply Light - Smart!. */
@Mod(SimplyLightSmart.MOD_ID)
public final class SimplyLightSmart {
    public static final String MOD_ID = "simply_light_smart";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimplyLightSmart(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        LOGGER.info("Simply Light - Smart! loaded");
    }
}
