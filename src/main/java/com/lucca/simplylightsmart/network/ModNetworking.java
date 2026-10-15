package com.lucca.simplylightsmart.network;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.smart.SmartLinkSavedData;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import com.lucca.simplylightsmart.smart.SmartTargetAddress;
import com.lucca.simplylightsmart.item.SmartLinkerItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the small server-authoritative interactions initiated from the client. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ClearLinkerSelectionPayload.TYPE, ClearLinkerSelectionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || !player.isShiftKeyDown()) {
                        return;
                    }
                    ItemStack stack = player.getMainHandItem();
                    if (stack.getItem() instanceof SmartLinkerItem && SmartLinkerItem.clearSelection(stack)) {
                        player.displayClientMessage(
                                Component.translatable("message.simply_light_smart.linker.selection_cleared"), true);
                    }
                }));
        registrar.playToServer(RequestLinkerHighlightsPayload.TYPE, RequestLinkerHighlightsPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        sendLinkerHighlights(player);
                    }
                }));

        // The dedicated server must advertise this client-bound payload during the
        // NeoForge handshake. The real handler is registered by the client-only class.
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            registrar.playToClient(LinkerHighlightsPayload.TYPE, LinkerHighlightsPayload.STREAM_CODEC,
                    (payload, context) -> {});
        }
    }

    private static void sendLinkerHighlights(net.minecraft.server.level.ServerPlayer player) {
        ItemStack held = player.getMainHandItem().getItem() instanceof SmartLinkerItem
                ? player.getMainHandItem()
                : player.getOffhandItem().getItem() instanceof SmartLinkerItem ? player.getOffhandItem() : ItemStack.EMPTY;
        if (held.isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();

        var selection = SmartLinkerItem.selectedTargetsWithAddresses(held);
        if (selection.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new LinkerHighlightsPayload(java.util.List.of()));
            return;
        }
        var links = SmartLinkSavedData.get(level);
        java.util.List<LinkerHighlightsPayload.LinkPair> highlights = new java.util.ArrayList<>();
        for (SmartTargetAddress target : selection) {
            var resolvedLamp = target.resolve(level).orElse(null);
            if (resolvedLamp == null || !resolvedLamp.dimension().equals(level.dimension())) {
                continue;
            }
            SmartTargetAddress owner = links.ownerOf(target).orElse(null);
            var resolvedSwitch = owner == null ? null : owner.resolve(level).orElse(null);
            if (resolvedSwitch == null || !resolvedSwitch.dimension().equals(resolvedLamp.dimension())) {
                continue;
            }
            double distanceSquared = SmartSwitchBlockEntity.distanceSquared(level, resolvedLamp.pos(), resolvedSwitch.pos());
            if (distanceSquared < 120.0D * 120.0D) {
                highlights.add(new LinkerHighlightsPayload.LinkPair(resolvedLamp.pos(), resolvedSwitch.pos()));
            }
        }
        PacketDistributor.sendToPlayer(player, new LinkerHighlightsPayload(highlights));
    }
}
