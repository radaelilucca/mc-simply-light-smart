package com.lucca.simplylightsmart.client;

import com.lucca.simplylightsmart.SimplyLightSmart;
import com.lucca.simplylightsmart.item.SmartLinkerItem;
import com.lucca.simplylightsmart.network.LinkerHighlightsPayload;
import com.lucca.simplylightsmart.network.RequestLinkerHighlightsPayload;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draws a small green outline around each target currently held by the Linker. */
@EventBusSubscriber(modid = SimplyLightSmart.MOD_ID, value = Dist.CLIENT)
public final class SmartLinkerSelectionOutline {
    private static final long HIGHLIGHT_REFRESH_MILLIS = 250L;
    private static final float RED = 0.25F;
    private static final float GREEN = 1.0F;
    private static final float BLUE = 0.25F;
    private static final float ALPHA = 1.0F;
    private static List<LinkerHighlightsPayload.LinkPair> linkedTargets = List.of();
    private static long lastHighlightRequest;

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
            linkedTargets = List.of();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastHighlightRequest >= HIGHLIGHT_REFRESH_MILLIS) {
            PacketDistributor.sendToServer(new RequestLinkerHighlightsPayload());
            lastHighlightRequest = now;
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
            VoxelShape shape = OriginalLightAdapter.isLocallyEnabled(state)
                    ? OriginalLightAdapter.selectionShape(level, target, state)
                    : state.getShape(level, target);
            if (shape.isEmpty()) {
                continue;
            }

            AABB bounds = shape.bounds().move(target).inflate(0.002D);
            bounds = SableOutlineProjection.projectBounds(level, bounds, event.getPartialTick().getGameTimeDeltaPartialTick(false));
            LevelRenderer.renderLineBox(poseStack, consumer, bounds, RED, GREEN, BLUE, ALPHA);
        }
        for (LinkerHighlightsPayload.LinkPair link : linkedTargets) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Vec3 lamp = visualCenter(level, link.lamp(), partialTick);
            Vec3 smartSwitch = visualCenter(level, link.smartSwitch(), partialTick);
            drawLine(consumer, poseStack, lamp, smartSwitch);
        }
        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }

    /** Receives the server-filtered owner pairs for the current selection. */
    public static void setLinkedTargets(List<LinkerHighlightsPayload.LinkPair> links) {
        linkedTargets = List.copyOf(links);
    }

    private static void drawLine(VertexConsumer consumer, PoseStack poseStack, Vec3 from, Vec3 to) {
        var pose = poseStack.last();
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(0.35F, 1.0F, 0.35F, 0.9F)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(0.35F, 1.0F, 0.35F, 0.9F)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** Anchors a line to the visible shape bounds, which may occupy only part of its block cell. */
    private static Vec3 visualCenter(ClientLevel level, BlockPos pos, float partialTick) {
        if (!level.hasChunkAt(pos)) {
            return SableOutlineProjection.projectBounds(level, new AABB(Vec3.atCenterOf(pos), Vec3.atCenterOf(pos)), partialTick)
                    .getCenter();
        }
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = OriginalLightAdapter.isLocallyEnabled(state)
                ? OriginalLightAdapter.selectionShape(level, pos, state)
                : state.getShape(level, pos);
        Vec3 center = shape.isEmpty() ? Vec3.atCenterOf(pos) : shape.bounds().move(pos).getCenter();
        AABB projected = SableOutlineProjection.projectBounds(level, new AABB(center, center), partialTick);
        return projected.getCenter();
    }
}
