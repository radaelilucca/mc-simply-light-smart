package com.lucca.simplylightsmart.client.shader;

import com.lucca.simplylightsmart.SimplyLightSmart;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Delivers background patcher results only once a local player exists to receive chat messages. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, value = Dist.CLIENT)
final class SmartShaderPatchNotifications {
    private static final Queue<Component> PENDING_MESSAGES = new ConcurrentLinkedQueue<>();
    private static final Component MOD_PREFIX = Component.literal("[Simply Light - Smart!]")
            .withStyle(ChatFormatting.LIGHT_PURPLE);

    private SmartShaderPatchNotifications() {
    }

    static void created(String name) {
        PENDING_MESSAGES.add(Component.translatable("message.simply_light_smart.shader.created", MOD_PREFIX, name));
    }

    static void failed(String name, String reason) {
        PENDING_MESSAGES.add(Component.translatable("message.simply_light_smart.shader.failed", MOD_PREFIX, name, reason));
    }

    @SubscribeEvent
    public static void deliverMessages(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }

        Component message;
        while ((message = PENDING_MESSAGES.poll()) != null) {
            Minecraft.getInstance().player.displayClientMessage(message, false);
        }
    }
}
