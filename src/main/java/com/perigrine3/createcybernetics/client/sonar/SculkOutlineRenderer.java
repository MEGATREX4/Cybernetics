package com.perigrine3.createcybernetics.client.sonar;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashSet;
import java.util.Set;

public final class SculkOutlineRenderer {
    private static final int SEARCH_RADIUS = 50;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private static final float OUTLINE_RED = 0.0F;
    private static final float OUTLINE_GREEN = 0.6666667F;
    private static final float OUTLINE_BLUE = 0.6666667F;
    private static final float OUTLINE_ALPHA = 1.0F;

    private static final Set<BlockPos> SCULK_POSITIONS = new HashSet<>();

    private static long lastRefreshTick = Long.MIN_VALUE;
    private static BlockPos lastCenter;
    private static ResourceKey<Level> lastDimension;

    private SculkOutlineRenderer() {
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        ClientLevel level = mc.level;
        BlockPos center = mc.player.blockPosition();
        ResourceKey<Level> dimension = level.dimension();

        long gameTime = level.getGameTime();

        boolean dimensionChanged = lastDimension == null || lastDimension != dimension;
        boolean movedSignificantly = lastCenter == null || center.distManhattan(lastCenter) >= 3;
        boolean refreshDue = gameTime - lastRefreshTick >= REFRESH_INTERVAL_TICKS;

        if (!dimensionChanged && !movedSignificantly && !refreshDue) {
            return;
        }

        refresh(level, center);

        lastCenter = center.immutable();
        lastDimension = dimension;
        lastRefreshTick = gameTime;
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        if (SCULK_POSITIONS.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        RenderSystem.lineWidth(2.0F);

        poseStack.pushPose();

        for (BlockPos pos : SCULK_POSITIONS) {
            BlockState state = mc.level.getBlockState(pos);

            if (!isSculk(state)) {
                continue;
            }

            double x = pos.getX() - cameraPos.x;
            double y = pos.getY() - cameraPos.y;
            double z = pos.getZ() - cameraPos.z;

            AABB box = new AABB(
                    x,
                    y,
                    z,
                    x + 1.0D,
                    y + 1.0D,
                    z + 1.0D
            ).inflate(0.002D);

            LevelRenderer.renderLineBox(
                    poseStack,
                    consumer,
                    box,
                    OUTLINE_RED,
                    OUTLINE_GREEN,
                    OUTLINE_BLUE,
                    OUTLINE_ALPHA
            );
        }

        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());

        RenderSystem.lineWidth(1.0F);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void clear() {
        SCULK_POSITIONS.clear();

        lastRefreshTick = Long.MIN_VALUE;
        lastCenter = null;
        lastDimension = null;
    }

    private static void refresh(ClientLevel level, BlockPos center) {
        SCULK_POSITIONS.clear();

        int minX = center.getX() - SEARCH_RADIUS;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - SEARCH_RADIUS);
        int minZ = center.getZ() - SEARCH_RADIUS;

        int maxX = center.getX() + SEARCH_RADIUS;
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + SEARCH_RADIUS);
        int maxZ = center.getZ() + SEARCH_RADIUS;

        int radiusSquared = SEARCH_RADIUS * SEARCH_RADIUS;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            int dx = x - center.getX();

            for (int z = minZ; z <= maxZ; z++) {
                int dz = z - center.getZ();

                if (dx * dx + dz * dz > radiusSquared) {
                    continue;
                }

                for (int y = minY; y <= maxY; y++) {
                    int dy = y - center.getY();

                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }

                    cursor.set(x, y, z);

                    if (isSculk(level.getBlockState(cursor))) {
                        SCULK_POSITIONS.add(cursor.immutable());
                    }
                }
            }
        }
    }

    private static boolean isSculk(BlockState state) {
        return state.is(Blocks.SCULK)
                || state.is(Blocks.SCULK_VEIN)
                || state.is(Blocks.SCULK_CATALYST)
                || state.is(Blocks.SCULK_SENSOR)
                || state.is(Blocks.CALIBRATED_SCULK_SENSOR)
                || state.is(Blocks.SCULK_SHRIEKER);
    }
}