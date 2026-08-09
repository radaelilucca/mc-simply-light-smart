package com.lucca.simplylightsmart.item;

import com.lucca.simplylightsmart.smart.SmartLinkDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Prints server-authoritative dimmer and Smart Switch state for a clicked block. */
public final class SmartDebuggerItem extends Item {
    public SmartDebuggerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clickedPos = context.getClickedPos();
        SmartLinkDiagnostics.sendReport(player, level, clickedPos);
        return InteractionResult.CONSUME;
    }
}
