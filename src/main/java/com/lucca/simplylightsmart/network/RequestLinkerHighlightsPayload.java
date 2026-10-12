package com.lucca.simplylightsmart.network;

import com.lucca.simplylightsmart.SimplyLightSmart;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client request to refresh the owner-switch lines for the held Linker. */
public record RequestLinkerHighlightsPayload() implements CustomPacketPayload {
    public static final Type<RequestLinkerHighlightsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplyLightSmart.MOD_ID, "request_linker_highlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestLinkerHighlightsPayload> STREAM_CODEC =
            StreamCodec.unit(new RequestLinkerHighlightsPayload());

    @Override
    public Type<RequestLinkerHighlightsPayload> type() {
        return TYPE;
    }
}
