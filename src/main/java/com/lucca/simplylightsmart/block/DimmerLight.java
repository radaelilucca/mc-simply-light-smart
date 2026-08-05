package com.lucca.simplylightsmart.block;

import com.lucca.simplylightsmart.smart.SmartLinkSavedData;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Common redstone and Smart Switch behavior shared by every dimmable light. */
public interface DimmerLight {
    IntegerProperty POWER = BlockStateProperties.POWER;

    void refreshPower(Level level, BlockPos pos, BlockState state);

    static int power(Level level, BlockPos pos, boolean inverted) {
        int signal = remoteSignal(level, pos);
        return inverted ? signal : 15 - signal;
    }

    static void removeTarget(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SmartLinkSavedData.get(serverLevel)
                .removeTarget(SmartSwitchBlockEntity.globalPos(serverLevel, pos))
                .ifPresent(owner -> {
                    ServerLevel switchLevel = serverLevel.getServer().getLevel(owner.dimension());
                    if (switchLevel != null && switchLevel.hasChunkAt(owner.pos())
                            && switchLevel.getBlockEntity(owner.pos()) instanceof SmartSwitchBlockEntity switchEntity) {
                        switchEntity.forgetTarget(SmartSwitchBlockEntity.globalPos(serverLevel, pos));
                    }
                });
    }

    private static int remoteSignal(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            return SmartLinkSavedData.get(serverLevel)
                    .ownerOf(SmartSwitchBlockEntity.globalPos(serverLevel, pos))
                    .flatMap(owner -> {
                        ServerLevel switchLevel = serverLevel.getServer().getLevel(owner.dimension());
                        if (switchLevel != null && switchLevel.hasChunkAt(owner.pos())
                                && switchLevel.getBlockEntity(owner.pos()) instanceof SmartSwitchBlockEntity switchEntity) {
                            return java.util.Optional.of(switchEntity.signal());
                        }
                        return java.util.Optional.empty();
                    })
                    .orElseGet(() -> SmartLinkSavedData.get(serverLevel)
                            .ownerOf(SmartSwitchBlockEntity.globalPos(serverLevel, pos))
                            .isPresent() ? 0 : level.getBestNeighborSignal(pos));
        }
        return level.getBestNeighborSignal(pos);
    }
}
