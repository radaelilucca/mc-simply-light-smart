package com.lucca.simplylightsmart.smart;

import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Server-side diagnostics for tracing a lamp's effective dimmer controller. */
public final class SmartLinkDiagnostics {
    private SmartLinkDiagnostics() {
    }

    public static void sendReport(ServerPlayer player, ServerLevel level, BlockPos clickedPos) {
        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.getBlock() instanceof SmartSwitchBlock) {
            sendSwitchReport(player, level, clickedPos);
            return;
        }
        if (!OriginalLightAdapter.isLocallyEnabled(clickedState)) {
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.not_supported",
                    blockName(clickedState)).withStyle(ChatFormatting.RED));
            return;
        }

        BlockPos controlPos = OriginalLightAdapter.controlPosition(clickedPos, clickedState);
        BlockState controlState = level.getBlockState(controlPos);
        SmartTargetAddress target = SmartSwitchBlockEntity.address(level, controlPos);
        SmartLinkSavedData links = SmartLinkSavedData.get(level);
        Optional<SmartTargetAddress> owner = links.ownerOf(target);
        int localSignal = level.getBestNeighborSignal(controlPos);
        int effectiveSignal = owner.map(links::signalOf).orElse(localSignal);
        int expectedEmission = OriginalLightAdapter.emissionForSignal(controlState, effectiveSignal);

        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.header",
                blockName(controlState), controlPos.toShortString()).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.light_state",
                controlState.getValue(OriginalLightAdapter.SMART_MODE).getSerializedName(),
                controlState.getValue(OriginalLightAdapter.SMART_POWER), localSignal));
        if (owner.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.link.none")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            SmartTargetAddress switchAddress = owner.get();
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.link.owner",
                    describeAddress(switchAddress), links.signalOf(switchAddress)).withStyle(ChatFormatting.YELLOW));
            player.sendSystemMessage(ownerStatus(level, switchAddress));
        }
        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.effective_signal",
                effectiveSignal, expectedEmission).withStyle(ChatFormatting.AQUA));
    }

    private static void sendSwitchReport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SmartSwitchBlockEntity switchEntity) {
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.header",
                    pos.toShortString()).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.state",
                    switchEntity.signal(), switchEntity.linkedTargets().size()).withStyle(ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.missing",
                    pos.toShortString()).withStyle(ChatFormatting.RED));
        }
    }

    private static Component ownerStatus(ServerLevel sourceLevel, SmartTargetAddress owner) {
        Optional<GlobalPos> resolved = owner.resolve(sourceLevel);
        if (resolved.isEmpty()) {
            return Component.translatable("message.simply_light_smart.debugger.owner.unresolved").withStyle(ChatFormatting.RED);
        }
        GlobalPos ownerPos = resolved.get();
        ServerLevel ownerLevel = sourceLevel.getServer().getLevel(ownerPos.dimension());
        if (ownerLevel == null) {
            return Component.translatable("message.simply_light_smart.debugger.owner.unresolved").withStyle(ChatFormatting.RED);
        }
        LevelChunk ownerChunk = ownerLevel.getChunkSource().getChunkNow(ownerPos.pos().getX() >> 4, ownerPos.pos().getZ() >> 4);
        if (ownerChunk == null) {
            return Component.translatable("message.simply_light_smart.debugger.owner.unloaded").withStyle(ChatFormatting.GRAY);
        }
        BlockEntity ownerEntity = ownerChunk.getBlockEntity(ownerPos.pos());
        if (ownerEntity instanceof SmartSwitchBlockEntity switchEntity) {
            return Component.translatable("message.simply_light_smart.debugger.owner.loaded",
                    switchEntity.signal(), switchEntity.linkedTargets().size()).withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("message.simply_light_smart.debugger.owner.stale").withStyle(ChatFormatting.RED);
    }

    private static String blockName(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String describeAddress(SmartTargetAddress address) {
        String fallback = address.fallback().dimension().location() + " @ " + address.fallback().pos().toShortString();
        return address.subLevel().map(reference -> fallback + " [Sable " + reference.id() + " / "
                + reference.localPos().toShortString() + "]").orElse(fallback);
    }
}
