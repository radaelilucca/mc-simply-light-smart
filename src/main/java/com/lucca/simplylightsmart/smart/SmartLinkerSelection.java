package com.lucca.simplylightsmart.smart;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Persistent selection carried by a Smart Linker.
 *
 * <p>Targets retain a normal-world fallback and, when relevant, a stable
 * Sable sublevel address. The Switch performs authoritative validation when
 * this selection is applied.</p>
 */
public record SmartLinkerSelection(List<SmartTargetAddress> targets) {
    public static final SmartLinkerSelection EMPTY = new SmartLinkerSelection(List.of());
    public static final Codec<SmartLinkerSelection> CODEC = SmartTargetAddress.CODEC.listOf()
            .xmap(SmartLinkerSelection::new, SmartLinkerSelection::targets);
    public static final StreamCodec<RegistryFriendlyByteBuf, SmartLinkerSelection> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public SmartLinkerSelection {
        targets = List.copyOf(targets);
    }

    public SmartLinkerSelection toggle(SmartTargetAddress target) {
        if (targets.contains(target)) {
            return new SmartLinkerSelection(targets.stream().filter(existing -> !existing.equals(target)).toList());
        }

        return new SmartLinkerSelection(java.util.stream.Stream.concat(targets.stream(), java.util.stream.Stream.of(target))
                .toList());
    }
}
