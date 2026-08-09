package com.lucca.simplylightsmart.smart;

import com.mojang.serialization.MapCodec;
import com.lucca.simplylightsmart.original.DeferredLightWork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Redstone receiver which relays its exact signal to linked dimmers. */
public final class SmartSwitchBlock extends BaseEntityBlock {
    public static final MapCodec<SmartSwitchBlock> CODEC = simpleCodec(SmartSwitchBlock::new);

    public SmartSwitchBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmartSwitchBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SmartSwitchBlockEntity switchEntity) {
            DeferredLightWork.enqueueSwitchRefresh((ServerLevel) level, pos, switchEntity);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SmartSwitchBlockEntity switchEntity) {
            DeferredLightWork.enqueueSwitchRefresh((ServerLevel) level, pos, switchEntity);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SmartSwitchBlockEntity switchEntity) {
            switchEntity.clearLinks();
        } else if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            SmartSwitchBlockEntity.clearPersistedLinks(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
