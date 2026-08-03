package com.lucca.simplylightsmart.network;

import com.lucca.simplylightsmart.SimplyLightSmart;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client request to clear the Smart Linker held in the main hand. */
public record ClearLinkerSelectionPayload() implements CustomPacketPayload {
    public static final Type<ClearLinkerSelectionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplyLightSmart.MOD_ID, "clear_linker_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearLinkerSelectionPayload> STREAM_CODEC =
            StreamCodec.unit(new ClearLinkerSelectionPayload());

    @Override
    public Type<ClearLinkerSelectionPayload> type() {
        return TYPE;
    }
}
