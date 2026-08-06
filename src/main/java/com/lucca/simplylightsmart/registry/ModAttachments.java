package com.lucca.simplylightsmart.registry;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.original.OriginalLightChunkIndex;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Persistent chunk attachments owned by the addon. */
public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES, SimplyLightSmart.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OriginalLightChunkIndex>> ORIGINAL_LIGHT_INDEX =
            ATTACHMENTS.register("original_light_index", () -> AttachmentType.serializable(OriginalLightChunkIndex::new).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
