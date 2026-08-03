package com.lucca.simplylightstoggle;

import com.mojang.logging.LogUtils;
import com.lucca.simplylightstoggle.registry.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

/** Main entry point for Simply Lights Toggle. */
@Mod(SimplyLightsToggle.MOD_ID)
public final class SimplyLightsToggle {
    public static final String MOD_ID = "simply_lights_toggle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimplyLightsToggle(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        modEventBus.addListener(this::addCreativeTabEntries);
        LOGGER.info("Simply Lights Toggle loaded");
    }

    private void addCreativeTabEntries(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            ModBlocks.addBulbsToCreativeTab(event);
        }
    }
}
