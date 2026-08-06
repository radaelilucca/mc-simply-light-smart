package com.lucca.simplylightsmart.item;

import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import com.lucca.simplylightsmart.registry.ModDataComponents;
import com.lucca.simplylightsmart.smart.SmartLinkerSelection;
import com.lucca.simplylightsmart.smart.SmartSwitchBlock;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Selects original Simply Light fixtures and applies the complete selection to a Smart
 * Switch. The server owns all mutation and validation; the item retains the
 * proposed target list until the player clears it manually.
 */
public final class SmartLinkerItem extends Item {
    public SmartLinkerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * Client-friendly selected locations for the outline renderer. Positions
     * are only meaningful in {@link #selectedDimension(ItemStack)}.
     */
    public static List<BlockPos> selectedTargets(ItemStack stack) {
        return selection(stack).targets().stream().map(GlobalPos::pos).toList();
    }

    /** The dimension stored for the current selection, or {@code null} when empty. */
    public static net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> selectedDimension(ItemStack stack) {
        return selection(stack).targets().isEmpty() ? null : selection(stack).targets().getFirst().dimension();
    }

    /** Clears a proposed selection and returns whether it contained targets. */
    public static boolean clearSelection(ItemStack stack) {
        if (selection(stack).targets().isEmpty()) {
            return false;
        }
        stack.set(ModDataComponents.LINKER_SELECTION.get(), SmartLinkerSelection.EMPTY);
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = context.getLevel().getBlockState(clickedPos);
        if (OriginalLightAdapter.isLocallyEnabled(clickedState)) {
            if (!(context.getPlayer() instanceof ServerPlayer) || !(context.getLevel() instanceof ServerLevel level)) {
                return InteractionResult.SUCCESS;
            }
            toggleTarget(stack, GlobalPos.of(level.dimension(), OriginalLightAdapter.controlPosition(clickedPos, clickedState)));
            return InteractionResult.CONSUME;
        }

        // Use the interaction's secondary-use flag rather than crouching state.
        // In particular, creative-flight players can hold Shift to request the
        // secondary action without entering the crouching pose.
        if (context.getPlayer() != null && context.isSecondaryUseActive()
                && context.getLevel().getBlockState(clickedPos).getBlock() instanceof SmartSwitchBlock) {
            if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level)) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(clickedPos);
            if (blockEntity instanceof SmartSwitchBlockEntity switchEntity) {
                SmartSwitchBlockEntity.LinkApplicationResult result = switchEntity.applyLinks(player, selection(stack).targets());
                if (result.success()) {
                    player.displayClientMessage(result.linkedCount() == 0
                            ? Component.translatable("message.simply_light_smart.linker.cleared")
                            : Component.translatable("message.simply_light_smart.linker.applied", result.linkedCount()), true);
                } else {
                    player.displayClientMessage(Component.translatable(
                            "message.simply_light_smart.linker.failed",
                            Component.translatable("message.simply_light_smart.linker.reason." + result.reason())), true);
                }
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "tooltip.simply_light_smart.smart_linker.selected", selection(stack).targets().size())
                .withStyle(ChatFormatting.GRAY));
    }

    private static SmartLinkerSelection selection(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LINKER_SELECTION.get(), SmartLinkerSelection.EMPTY);
    }

    private static void toggleTarget(ItemStack stack, GlobalPos target) {
        stack.set(ModDataComponents.LINKER_SELECTION.get(), selection(stack).toggle(target));
    }
}
