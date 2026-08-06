package com.lucca.simplylightsmart.block;

import com.lucca.simplylightsmart.smart.SmartLinkSavedData;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Common redstone and Smart Switch behavior shared by every dimmable light. */
public interface DimmerLight {
    IntegerProperty POWER = BlockStateProperties.POWER;

    void refreshPower(Level level, BlockPos pos, BlockState state);

    /** Returns the position stored by the Linker for this light. */
    default BlockPos linkPosition(Level level, BlockPos pos, BlockState state) {
        return pos;
    }

    /** Returns the exact geometry drawn by the Linker's selection outline. */
    default VoxelShape linkOutlineShape(BlockGetter level, BlockPos pos, BlockState state) {
        return state.getShape(level, pos);
    }

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
                    LevelChunk switchChunk = switchLevel == null ? null : switchLevel.getChunkSource().getChunkNow(
                            owner.pos().getX() >> 4, owner.pos().getZ() >> 4);
                    if (switchChunk != null
                            && switchChunk.getBlockEntity(owner.pos()) instanceof SmartSwitchBlockEntity switchEntity) {
                        switchEntity.forgetTarget(SmartSwitchBlockEntity.globalPos(serverLevel, pos));
                    }
                });
    }

    private static int remoteSignal(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            SmartLinkSavedData links = SmartLinkSavedData.get(serverLevel);
            return links.ownerOf(SmartSwitchBlockEntity.globalPos(serverLevel, pos))
                    .map(links::signalOf)
                    .orElseGet(() -> level.getBestNeighborSignal(pos));
        }
        return level.getBestNeighborSignal(pos);
    }
}
