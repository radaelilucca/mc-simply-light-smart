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
        Optional<SmartTargetAddress> owner = links.ownerOfReadOnly(target);
        int localSignal = level.getBestNeighborSignal(controlPos);
        int effectiveSignal = owner.map(links::signalOfReadOnly).orElse(localSignal);
        int expectedEmission = OriginalLightAdapter.emissionForSignal(controlState, effectiveSignal);

        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.header",
                blockName(controlState), controlPos.toShortString()).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.light_state",
                controlState.getValue(OriginalLightAdapter.SMART_MODE).getSerializedName(),
                controlState.getValue(OriginalLightAdapter.SMART_POWER), localSignal));
        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.address",
                describeAddress(target)).withStyle(ChatFormatting.DARK_AQUA));
        if (owner.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.link.none")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            SmartTargetAddress switchAddress = owner.get();
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.link.owner",
                    describeAddress(switchAddress), links.signalOfReadOnly(switchAddress)).withStyle(ChatFormatting.YELLOW));
            OwnerStatus ownerStatus = ownerStatus(level, switchAddress, target);
            player.sendSystemMessage(ownerStatus.status());
            ownerStatus.targetListStatus().ifPresent(player::sendSystemMessage);
        }
        player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.effective_signal",
                effectiveSignal, expectedEmission).withStyle(ChatFormatting.AQUA));
    }

    private static void sendSwitchReport(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SmartSwitchBlockEntity switchEntity) {
            SmartTargetAddress switchAddress = SmartSwitchBlockEntity.address(level, pos);
            SmartLinkSavedData links = SmartLinkSavedData.get(level);
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.header",
                    pos.toShortString()).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.state",
                    switchEntity.signal(), switchEntity.linkedTargets().size()).withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.address",
                    describeAddress(switchAddress)).withStyle(ChatFormatting.DARK_AQUA));

            int targetNumber = 1;
            for (SmartTargetAddress target : switchEntity.linkedTargets()) {
                player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.target",
                        targetNumber++, describeAddress(target)).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.target_status",
                        targetStatus(level, target), indexStatus(links, target, switchAddress)).withStyle(ChatFormatting.GRAY));
            }
        } else {
            player.sendSystemMessage(Component.translatable("message.simply_light_smart.debugger.switch.missing",
                    pos.toShortString()).withStyle(ChatFormatting.RED));
        }
    }

    private static OwnerStatus ownerStatus(ServerLevel sourceLevel, SmartTargetAddress owner, SmartTargetAddress target) {
        Optional<GlobalPos> resolved = owner.resolve(sourceLevel);
        if (resolved.isEmpty()) {
            return OwnerStatus.of(Component.translatable("message.simply_light_smart.debugger.owner.unresolved")
                    .withStyle(ChatFormatting.RED));
        }
        GlobalPos ownerPos = resolved.get();
        ServerLevel ownerLevel = sourceLevel.getServer().getLevel(ownerPos.dimension());
        if (ownerLevel == null) {
            return OwnerStatus.of(Component.translatable("message.simply_light_smart.debugger.owner.unresolved")
                    .withStyle(ChatFormatting.RED));
        }
        LevelChunk ownerChunk = ownerLevel.getChunkSource().getChunkNow(ownerPos.pos().getX() >> 4, ownerPos.pos().getZ() >> 4);
        if (ownerChunk == null) {
            return OwnerStatus.of(Component.translatable("message.simply_light_smart.debugger.owner.unloaded")
                    .withStyle(ChatFormatting.GRAY));
        }
        BlockEntity ownerEntity = ownerChunk.getBlockEntity(ownerPos.pos());
        if (ownerEntity instanceof SmartSwitchBlockEntity switchEntity) {
            Component targetListStatus = Component.translatable(switchEntity.linkedTargets().contains(target)
                    ? "message.simply_light_smart.debugger.owner.target_listed"
                    : "message.simply_light_smart.debugger.owner.target_missing")
                    .withStyle(switchEntity.linkedTargets().contains(target) ? ChatFormatting.GREEN : ChatFormatting.RED);
            return new OwnerStatus(Component.translatable("message.simply_light_smart.debugger.owner.loaded",
                    switchEntity.signal(), switchEntity.linkedTargets().size()).withStyle(ChatFormatting.GREEN),
                    Optional.of(targetListStatus));
        }
        return OwnerStatus.of(Component.translatable("message.simply_light_smart.debugger.owner.stale")
                .withStyle(ChatFormatting.RED));
    }

    private static Component targetStatus(ServerLevel sourceLevel, SmartTargetAddress target) {
        Optional<GlobalPos> resolved = target.resolve(sourceLevel);
        if (resolved.isEmpty()) {
            return Component.translatable("message.simply_light_smart.debugger.target.unresolved").withStyle(ChatFormatting.RED);
        }
        GlobalPos targetPos = resolved.get();
        ServerLevel targetLevel = sourceLevel.getServer().getLevel(targetPos.dimension());
        if (targetLevel == null) {
            return Component.translatable("message.simply_light_smart.debugger.target.unresolved").withStyle(ChatFormatting.RED);
        }
        LevelChunk targetChunk = targetLevel.getChunkSource().getChunkNow(targetPos.pos().getX() >> 4, targetPos.pos().getZ() >> 4);
        if (targetChunk == null) {
            return Component.translatable("message.simply_light_smart.debugger.target.unloaded",
                    describeGlobalPos(targetPos)).withStyle(ChatFormatting.GRAY);
        }
        SmartTargetAddress current = SmartSwitchBlockEntity.address(targetLevel, targetPos.pos());
        if (!OriginalLightAdapter.isValidTarget(targetLevel, targetPos.pos())) {
            return Component.translatable("message.simply_light_smart.debugger.target.invalid",
                    describeGlobalPos(targetPos), describeAddress(current)).withStyle(ChatFormatting.RED);
        }
        return Component.translatable(target.equals(current)
                        ? "message.simply_light_smart.debugger.target.resolved_match"
                        : "message.simply_light_smart.debugger.target.resolved_mismatch",
                describeGlobalPos(targetPos), describeAddress(current))
                .withStyle(target.equals(current) ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component indexStatus(SmartLinkSavedData links, SmartTargetAddress target, SmartTargetAddress switchAddress) {
        Optional<SmartTargetAddress> owner = links.ownerOfReadOnly(target);
        if (owner.isEmpty()) {
            return Component.translatable("message.simply_light_smart.debugger.index.missing").withStyle(ChatFormatting.RED);
        }
        if (owner.get().equals(switchAddress)) {
            return Component.translatable("message.simply_light_smart.debugger.index.owned").withStyle(ChatFormatting.GREEN);
        }
        return Component.translatable("message.simply_light_smart.debugger.index.other", describeAddress(owner.get()))
                .withStyle(ChatFormatting.RED);
    }

    private static String blockName(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String describeAddress(SmartTargetAddress address) {
        String fallback = address.fallback().dimension().location() + " @ " + address.fallback().pos().toShortString();
        return address.subLevel().map(reference -> fallback + " [Sable " + reference.id() + " / "
                + reference.localPos().toShortString() + "]").orElse(fallback);
    }

    private static String describeGlobalPos(GlobalPos position) {
        return position.dimension().location() + " @ " + position.pos().toShortString();
    }

    private record OwnerStatus(Component status, Optional<Component> targetListStatus) {
        private static OwnerStatus of(Component status) {
            return new OwnerStatus(status, Optional.empty());
        }
    }
}
