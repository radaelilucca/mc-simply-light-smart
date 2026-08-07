package com.lucca.simplylightsmart.smart;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
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
    /**
     * Reads Linkers saved before sublevel-aware addresses existed. Such items
     * stored a raw GlobalPos list; normal saves are encoded exclusively with
     * the current SmartTargetAddress format after the next item save.
     */
    private static final Codec<SmartTargetAddress> TARGET_CODEC = Codec.either(SmartTargetAddress.CODEC, GlobalPos.CODEC)
            .xmap(value -> value.map(address -> address,
                            legacy -> new SmartTargetAddress(legacy, Optional.empty())),
                    Either::left);

    public static final Codec<SmartLinkerSelection> CODEC = TARGET_CODEC.listOf()
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
