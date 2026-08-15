package com.perigrine3.createcybernetics.client.biomonitor;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.perigrine3.createcybernetics.network.payload.BiomonitorVitalsPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BiomonitorRenderer {
    private BiomonitorRenderer() {
    }

    private static final int FRAME_RED = 70;
    private static final int FRAME_GREEN = 235;
    private static final int FRAME_BLUE = 220;
    private static final int FRAME_ALPHA = 255;

    private static final int TITLE_COLOR = 0xFFE6FFFF;
    private static final int VALUE_COLOR = 0xFF74EBD8;
    private static final int EFFECT_COLOR = 0xFFB5FF74;
    private static final int WARNING_COLOR = 0xFFFFC85A;
    private static final int IMPLANT_COLOR = 0xFF74EBD8;

    private static final int HEALTH_GREEN_COLOR = 0xFF66FF8A;
    private static final int HEALTH_YELLOW_COLOR = 0xFFFFD65A;
    private static final int HEALTH_RED_COLOR = 0xFFFF5A5A;

    private static final int FULL_BRIGHT = 15728880;

    private static final double CORNER_LENGTH_PERCENT = 0.22D;
    private static final double BOX_PADDING = 0.08D;
    private static final double PANEL_HEIGHT_OFFSET = 0.85D;

    private static final float PANEL_SCALE = 0.015F;
    private static final float PANEL_FORWARD_OFFSET = 0.02F;

    private static final int LINE_SPACING = 10;

    private static final RenderType BIOMONITOR_LINES = RenderType.create(
            "createcybernetics_biomonitor_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256,
            false,
            false,
            RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER).setLineState(RenderStateShard.DEFAULT_LINE).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).setCullState(RenderStateShard.NO_CULL).setLightmapState(RenderStateShard.NO_LIGHTMAP).setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false)
    );

    public static void render(PoseStack poseStack, Camera camera, LivingEntity target, BiomonitorVitalsPayload snapshot, float partialTick) {
        if (target == null || !target.isAlive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        AABB targetBounds = BiomonitorTargeting.getInterpolatedBoundingBox(target, partialTick).inflate(BOX_PADDING);
        Vec3 cameraPosition = camera.getPosition();

        renderTargetFrame(poseStack, bufferSource, targetBounds, cameraPosition);
        renderVitalsPanel(poseStack, bufferSource, camera, target, snapshot, targetBounds, cameraPosition);

        bufferSource.endBatch();
    }

    private static void renderTargetFrame(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, AABB targetBounds, Vec3 cameraPosition) {
        double minX = targetBounds.minX - cameraPosition.x;
        double minY = targetBounds.minY - cameraPosition.y;
        double minZ = targetBounds.minZ - cameraPosition.z;

        double maxX = targetBounds.maxX - cameraPosition.x;
        double maxY = targetBounds.maxY - cameraPosition.y;
        double maxZ = targetBounds.maxZ - cameraPosition.z;

        double xLength = maxX - minX;
        double yLength = maxY - minY;
        double zLength = maxZ - minZ;

        double cornerX = xLength * CORNER_LENGTH_PERCENT;
        double cornerY = yLength * CORNER_LENGTH_PERCENT;
        double cornerZ = zLength * CORNER_LENGTH_PERCENT;

        VertexConsumer consumer = bufferSource.getBuffer(BIOMONITOR_LINES);
        PoseStack.Pose pose = poseStack.last();

        renderLine(consumer, pose, minX, minY, minZ, minX + cornerX, minY, minZ);
        renderLine(consumer, pose, minX, minY, minZ, minX, minY + cornerY, minZ);
        renderLine(consumer, pose, minX, minY, minZ, minX, minY, minZ + cornerZ);

        renderLine(consumer, pose, maxX, minY, minZ, maxX - cornerX, minY, minZ);
        renderLine(consumer, pose, maxX, minY, minZ, maxX, minY + cornerY, minZ);
        renderLine(consumer, pose, maxX, minY, minZ, maxX, minY, minZ + cornerZ);

        renderLine(consumer, pose, minX, minY, maxZ, minX + cornerX, minY, maxZ);
        renderLine(consumer, pose, minX, minY, maxZ, minX, minY + cornerY, maxZ);
        renderLine(consumer, pose, minX, minY, maxZ, minX, minY, maxZ - cornerZ);

        renderLine(consumer, pose, maxX, minY, maxZ, maxX - cornerX, minY, maxZ);
        renderLine(consumer, pose, maxX, minY, maxZ, maxX, minY + cornerY, maxZ);
        renderLine(consumer, pose, maxX, minY, maxZ, maxX, minY, maxZ - cornerZ);

        renderLine(consumer, pose, minX, maxY, minZ, minX + cornerX, maxY, minZ);
        renderLine(consumer, pose, minX, maxY, minZ, minX, maxY - cornerY, minZ);
        renderLine(consumer, pose, minX, maxY, minZ, minX, maxY, minZ + cornerZ);

        renderLine(consumer, pose, maxX, maxY, minZ, maxX - cornerX, maxY, minZ);
        renderLine(consumer, pose, maxX, maxY, minZ, maxX, maxY - cornerY, minZ);
        renderLine(consumer, pose, maxX, maxY, minZ, maxX, maxY, minZ + cornerZ);

        renderLine(consumer, pose, minX, maxY, maxZ, minX + cornerX, maxY, maxZ);
        renderLine(consumer, pose, minX, maxY, maxZ, minX, maxY - cornerY, maxZ);
        renderLine(consumer, pose, minX, maxY, maxZ, minX, maxY, maxZ - cornerZ);

        renderLine(consumer, pose, maxX, maxY, maxZ, maxX - cornerX, maxY, maxZ);
        renderLine(consumer, pose, maxX, maxY, maxZ, maxX, maxY - cornerY, maxZ);
        renderLine(consumer, pose, maxX, maxY, maxZ, maxX, maxY, maxZ - cornerZ);
    }

    private static void renderLine(VertexConsumer consumer, PoseStack.Pose pose, double startX, double startY, double startZ, double endX, double endY, double endZ) {
        float directionX = (float) (endX - startX);
        float directionY = (float) (endY - startY);
        float directionZ = (float) (endZ - startZ);
        float length = Mth.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);

        if (length <= 0.0001F) {
            return;
        }

        float normalX = directionX / length;
        float normalY = directionY / length;
        float normalZ = directionZ / length;

        consumer.addVertex(pose.pose(), (float) startX, (float) startY, (float) startZ).setColor(FRAME_RED, FRAME_GREEN, FRAME_BLUE, FRAME_ALPHA).setNormal(pose, normalX, normalY, normalZ);
        consumer.addVertex(pose.pose(), (float) endX, (float) endY, (float) endZ).setColor(FRAME_RED, FRAME_GREEN, FRAME_BLUE, FRAME_ALPHA).setNormal(pose, normalX, normalY, normalZ);
    }

    private static void renderVitalsPanel(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, LivingEntity target, BiomonitorVitalsPayload snapshot, AABB targetBounds, Vec3 cameraPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        double panelX = (targetBounds.minX + targetBounds.maxX) * 0.5D;
        double panelY = targetBounds.maxY + PANEL_HEIGHT_OFFSET;
        double panelZ = (targetBounds.minZ + targetBounds.maxZ) * 0.5D;

        boolean implantMode = minecraft.player != null && minecraft.player.isShiftKeyDown();
        boolean obfuscated = BiomonitorObfuscationController.shouldObfuscate(minecraft.player);
        List<BiomonitorLine> lines = implantMode ? createImplantLines(target, snapshot) : createVitalsLines(target, snapshot);

        poseStack.pushPose();
        poseStack.translate(panelX - cameraPosition.x, panelY - cameraPosition.y, panelZ - cameraPosition.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(PANEL_SCALE, -PANEL_SCALE, PANEL_SCALE);
        poseStack.translate(0.0D, 0.0D, PANEL_FORWARD_OFFSET);

        int totalHeight = Math.max(0, (lines.size() - 1) * LINE_SPACING);
        int startingY = -totalHeight / 2;

        for (int index = 0; index < lines.size(); index++) {
            BiomonitorLine line = lines.get(index);

            Component renderedText = obfuscated ? line.text().copy().withStyle(ChatFormatting.OBFUSCATED) : line.text();
            int textWidth = font.width(renderedText);
            float textX = -textWidth / 2.0F;
            float textY = startingY + index * LINE_SPACING;

            renderTextLine(font, renderedText, textX, textY, line.color(), poseStack, bufferSource);
        }

        poseStack.popPose();
    }

    private static void renderTextLine(Font font, Component line, float x, float y, int color, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        font.drawInBatch(line, x, y, color, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0, FULL_BRIGHT);
    }

    private static List<BiomonitorLine> createVitalsLines(LivingEntity target, BiomonitorVitalsPayload snapshot) {
        List<BiomonitorLine> lines = new ArrayList<>();

        Component entityName = target.getType().getDescription();

        float health = snapshot != null ? snapshot.health() : target.getHealth();
        float maxHealth = snapshot != null ? snapshot.maxHealth() : target.getMaxHealth();
        int armor = snapshot != null ? snapshot.armor() : target.getArmorValue();

        lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.subject", entityName), TITLE_COLOR));
        lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.health", formatDecimal(health), formatDecimal(maxHealth)), getHealthColor(health, maxHealth)));
        lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.armor", armor), VALUE_COLOR));

        if (snapshot == null) {
            lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.effects_scanning"), WARNING_COLOR));
            return lines;
        }

        if (snapshot.hasHungerData()) {
            lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.hunger", snapshot.foodLevel(), formatDecimal(snapshot.saturationLevel())), VALUE_COLOR));
        }

        List<BiomonitorVitalsPayload.EffectData> effects = snapshot.effects();

        lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.effects", effects.size()), effects.isEmpty() ? VALUE_COLOR : EFFECT_COLOR));

        if (effects.isEmpty()) {
            lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.none"), VALUE_COLOR));
            return lines;
        }

        for (BiomonitorVitalsPayload.EffectData effect : effects) {
            lines.add(new BiomonitorLine(formatEffect(effect), EFFECT_COLOR));
        }

        return lines;
    }

    private static List<BiomonitorLine> createImplantLines(LivingEntity target, BiomonitorVitalsPayload snapshot) {
        List<BiomonitorLine> lines = new ArrayList<>();
        Component entityName = target.getType().getDescription();

        lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.subject", entityName), TITLE_COLOR));
        lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.implants"), TITLE_COLOR));

        if (snapshot == null) {
            lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.scanning"), WARNING_COLOR));
            return lines;
        }

        if (snapshot.implants().isEmpty()) {
            lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.no_implants"), VALUE_COLOR));
            return lines;
        }

        for (BiomonitorVitalsPayload.ImplantData implant : snapshot.implants()) {
            Component implantName = Component.translatable(implant.translationKey());

            if (implant.count() > 1) {
                lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.implant_multiple", implantName, implant.count()), IMPLANT_COLOR));
            } else {
                lines.add(new BiomonitorLine(Component.translatable("hud.createcybernetics.biomonitor.implant", implantName), IMPLANT_COLOR));
            }
        }

        return lines;
    }

    private static int getHealthColor(float health, float maxHealth) {
        if (maxHealth <= 0.0F) {
            return HEALTH_RED_COLOR;
        }

        float healthPercent = health / maxHealth;

        if (healthPercent <= 0.25F) {
            return HEALTH_RED_COLOR;
        }

        if (healthPercent <= 0.50F) {
            return HEALTH_YELLOW_COLOR;
        }

        return HEALTH_GREEN_COLOR;
    }

    private static Component formatEffect(BiomonitorVitalsPayload.EffectData effect) {
        Component effectName = Component.translatable(effect.translationKey());
        Component duration = effect.infiniteDuration() ? Component.translatable("hud.createcybernetics.biomonitor.infinite") : Component.literal(formatDuration(effect.duration()));

        if (effect.amplifier() > 0) {
            return Component.translatable("hud.createcybernetics.biomonitor.effect_amplified", effectName, toRomanNumeral(effect.amplifier() + 1), duration);
        }

        return Component.translatable("hud.createcybernetics.biomonitor.effect", effectName, duration);
    }

    private static String formatDecimal(float value) {
        return String.format("%.1f", value);
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private static String toRomanNumeral(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(value);
        };
    }

    private record BiomonitorLine(Component text, int color) {
    }
}