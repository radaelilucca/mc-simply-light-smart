package com.lucca.simplylightsmart.client;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.block.DimmerLight;
import com.lucca.simplylightsmart.item.SmartLinkerItem;
import com.lucca.simplylightsmart.original.OriginalLightAdapter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws a small green outline around each target currently held by the Linker. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, value = Dist.CLIENT)
public final class SmartLinkerSelectionOutline {
    private static final float RED = 0.25F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.25F;
    private static final float ALPHA = 1.0F;

    private SmartLinkerSelectionOutline() {
    }

    @SubscribeEvent
    public static void renderSelectedTargets(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        ItemStack linker = player.getMainHandItem();
        List<BlockPos> targets = SmartLinkerItem.selectedTargets(linker);
        if (targets.isEmpty()) {
            linker = player.getOffhandItem();
            targets = SmartLinkerItem.selectedTargets(linker);
        }
        ResourceKey<Level> selectionDimension = SmartLinkerItem.selectedDimension(linker);
        if (targets.isEmpty() || !level.dimension().equals(selectionDimension)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        for (BlockPos target : targets) {
            if (!level.hasChunkAt(target)) {
                continue;
            }

            BlockState state = level.getBlockState(target);
            VoxelShape shape = state.getBlock() instanceof DimmerLight dimmer
                    ? dimmer.linkOutlineShape(level, target, state)
                    : OriginalLightAdapter.isLocallyEnabled(state)
                            ? OriginalLightAdapter.selectionShape(level, target, state)
                            : state.getShape(level, target);
            if (shape.isEmpty()) {
                continue;
            }

            AABB bounds = shape.bounds().move(target).inflate(0.002D);
            bounds = SableOutlineProjection.projectBounds(level, bounds, event.getPartialTick().getGameTimeDeltaPartialTick(false));
            LevelRenderer.renderLineBox(poseStack, consumer, bounds, RED, GREEN, BLUE, ALPHA);
        }
        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }
}
