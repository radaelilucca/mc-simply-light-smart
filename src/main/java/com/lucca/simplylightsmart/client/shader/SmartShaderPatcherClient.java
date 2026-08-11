package com.lucca.simplylightsmart.client.shader;

import com.lucca.simplylightsmart.SimplyLightSmart;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Starts the derived shader-pack generator after the client has initialized its game directory. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SmartShaderPatcherClient {
    private SmartShaderPatcherClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> SmartShaderPackPatcher.schedule(Minecraft.getInstance().gameDirectory.toPath()));
    }
}
