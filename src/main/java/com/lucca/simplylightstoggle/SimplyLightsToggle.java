package com.lucca.simplylightstoggle;

import com.lucca.simplylightstoggle.registry.ModBlocks;
import com.lucca.simplylightstoggle.registry.ModCreativeTabs;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Main entry point for Simply Lights Toggle. */
@Mod(SimplyLightsToggle.MOD_ID)
public final class SimplyLightsToggle {
    public static final String MOD_ID = "simply_lights_toggle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimplyLightsToggle(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        LOGGER.info("Simply Lights Toggle loaded");
    }
}
