package com.lucca.simplylightsmart.network;

import com.lucca.simplylightsmart.SimplyLightSmart;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server response containing selected lamp and owning switch positions. */
public record LinkerHighlightsPayload(List<LinkPair> links) implements CustomPacketPayload {
    private static final StreamCodec<RegistryFriendlyByteBuf, LinkPair> LINK_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LinkPair::lamp,
            BlockPos.STREAM_CODEC, LinkPair::smartSwitch,
            LinkPair::new);
    public static final Type<LinkerHighlightsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimplyLightSmart.MOD_ID, "linker_highlights"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkerHighlightsPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.links.size());
                payload.links.forEach(link -> LINK_CODEC.encode(buffer, link));
            }, buffer -> {
                int size = Math.min(buffer.readVarInt(), 4096);
                List<LinkPair> links = new ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    links.add(LINK_CODEC.decode(buffer));
                }
                return new LinkerHighlightsPayload(links);
            });

    public LinkerHighlightsPayload {
        links = List.copyOf(links);
    }

    @Override
    public Type<LinkerHighlightsPayload> type() {
        return TYPE;
    }

    public record LinkPair(BlockPos lamp, BlockPos smartSwitch) {
    }
}
