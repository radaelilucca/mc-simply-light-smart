package com.lucca.simplylightsmart.block;

import com.lucca.simplylightsmart.smart.SmartLinkSavedData;
import com.lucca.simplylightsmart.smart.SmartSwitchBlockEntity;
import com.flanks255.simplylight.blocks.RotatableLamp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Shared redstone dimmer behavior for the addon’s rotatable light blocks. */
public abstract class AbstractDimmerRotatableLamp extends RotatableLamp {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    private final int maximumLightLevel;
    private final boolean inverted;

    protected AbstractDimmerRotatableLamp(BlockBehaviour.Properties properties, int maximumLightLevel, boolean inverted) {
        super(properties.lightLevel(state -> lightLevel(state, maximumLightLevel)));
        this.maximumLightLevel = maximumLightLevel;
        this.inverted = inverted;
        registerDefaultState(defaultBlockState().setValue(POWER, inverted ? 0 : 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withPower(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        refreshPower(level, pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        refreshPower(level, pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
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
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected final BlockState withPower(BlockState state, Level level, BlockPos pos) {
        return state.setValue(POWER, redstonePower(level, pos));
    }

    public final void refreshPower(Level level, BlockPos pos, BlockState state) {
        int power = redstonePower(level, pos);
        if (state.getValue(POWER) != power) {
            level.setBlockAndUpdate(pos, state.setValue(POWER, power));
        }
    }

    private int redstonePower(Level level, BlockPos pos) {
        int signal = remoteSignal(level, pos);
        return inverted ? signal : 15 - signal;
    }

    private int remoteSignal(Level level, BlockPos pos) {
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
                    // A linked lamp never falls back to local redstone. If its
                    // switch is currently unloaded, it remains off until that
                    // switch is available to provide its signal again.
                    .orElseGet(() -> SmartLinkSavedData.get(serverLevel)
                            .ownerOf(SmartSwitchBlockEntity.globalPos(serverLevel, pos))
                            .isPresent() ? 0 : level.getBestNeighborSignal(pos));
        }
        return level.getBestNeighborSignal(pos);
    }

    private static int lightLevel(BlockState state, int maximumLightLevel) {
        return Math.round(state.getValue(POWER) * maximumLightLevel / 15.0F);
    }
}
