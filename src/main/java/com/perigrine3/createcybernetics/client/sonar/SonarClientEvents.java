package com.perigrine3.createcybernetics.client.sonar;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.SculkedConversionHelper;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

@EventBusSubscriber(modid = CreateCybernetics.MODID, value = Dist.CLIENT)
public final class SonarClientEvents {
    private static final ResourceLocation SONAR_POST = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "shaders/post/sonar.json");

    private static final float SOUND_RADIUS_BLOCKS = 20.0F;
    private static final double SOUND_ENTITY_SEARCH_RADIUS = 1.5D;

    private static final float LIFE_SECONDS = 10.0F;
    private static final float WAVE_SPEED = 5.0F;
    private static final float WAVE_MAX_RADIUS = 50.0F;

    private static final float RING_BRIGHTNESS = 0.85F;
    private static final float RING_HALF_WIDTH = 0.08F;
    private static final float GROUND_HEIGHT_RANGE = 1.25F;

    private static final float SKY_EPS = 0.0005F;

    private static final double WAVE_ENTITY_VERTICAL_RANGE = 4.0D;
    private static final double WAVE_ENTITY_BASE_TOLERANCE = 0.30D;

    private static final int OUTLINE_RED = 0;
    private static final int OUTLINE_GREEN = 170;
    private static final int OUTLINE_BLUE = 170;

    private static final float PASS_WORLD = 0.0F;
    private static final float PASS_HAND = 1.0F;

    private static PostChain sonarPostChain;
    private static int sonarPostWidth = -1;
    private static int sonarPostHeight = -1;

    private SonarClientEvents() {
    }

    @SubscribeEvent
    public static void onPlaySoundEvent(PlaySoundEvent event) {
        if (event.getSound() == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!isSonarActive(mc)) {
            return;
        }

        Vec3 soundPos;

        if (event.getSound().isRelative()) {
            soundPos = mc.player.position();
        } else {
            soundPos = new Vec3(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
        }

        pushSound(mc, soundPos);

        if (!event.getSound().isRelative()) {
            markNearbySoundEntity(mc, soundPos);
        }
    }

    @SubscribeEvent
    public static void onLevelSoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (!event.getLevel().isClientSide) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!isSonarActive(mc)) {
            return;
        }

        Entity entity = event.getEntity();

        if (!(entity instanceof LivingEntity living) || living == mc.player || living.isRemoved() || !living.isAlive()) {
            return;
        }

        Vec3 soundPos = living.position();

        if (!isValidPosition(soundPos)) {
            return;
        }

        if (soundPos.distanceToSqr(mc.player.position()) > SOUND_RADIUS_BLOCKS * SOUND_RADIUS_BLOCKS) {
            return;
        }

        SonarPingManager.push(soundPos);
        SonarEntityTracker.mark(living);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            SonarPingManager.clear();
            SonarEntityTracker.clear();
            SculkOutlineRenderer.clear();
            closeSonarPostChain();
            return;
        }

        boolean sonarActive = isSonarActive(mc);
        boolean sculkConversionActive = isSculkConversionActive(mc);

        if (!sonarActive && !sculkConversionActive) {
            SonarPingManager.clear();
            SonarEntityTracker.clear();
            SculkOutlineRenderer.clear();
            closeSonarPostChain();
            return;
        }

        if (sonarActive) {
            SonarPingManager.prune(LIFE_SECONDS);
            detectEntitiesTouchedByWaves(mc);
            SculkOutlineRenderer.tick(mc);
        } else {
            SonarPingManager.clear();
            SculkOutlineRenderer.clear();
            closeSonarPostChain();
        }

        if (sculkConversionActive) {
            detectEntitiesTouchingSculk(mc);
        }

        SonarEntityTracker.tick();
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return;
        }

        boolean sonarActive = canRenderSonar(mc);
        boolean sculkConversionActive = isSculkConversionActive(mc);

        if (!sonarActive && !sculkConversionActive) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            renderDetectedEntityOutlines(mc, event);
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            if (sonarActive) {
                SculkOutlineRenderer.render(event);
            }

            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        if (!sonarActive) {
            return;
        }

        if (!ensureSonarPostChain(mc)) {
            return;
        }

        updateSonarUniforms(mc);

        if (sonarPostChain == null) {
            return;
        }

        sonarPostChain.setUniform("PassMode", PASS_WORLD);

        processSonarPass(mc);
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (!canRenderSonar(mc)) {
            return;
        }

        if (!ensureSonarPostChain(mc)) {
            return;
        }

        if (sonarPostChain == null) {
            return;
        }

        sonarPostChain.setUniform("PassMode", PASS_HAND);

        processSonarPass(mc);
    }

    private static void renderDetectedEntityOutlines(Minecraft mc, RenderLevelStageEvent event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        List<SonarEntityTracker.TrackedSnapshot> tracked = SonarEntityTracker.snapshot();

        if (tracked.isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        OutlineBufferSource outlineBuffer = mc.renderBuffers().outlineBufferSource();

        dispatcher.setRenderShadow(false);

        try {
            for (SonarEntityTracker.TrackedSnapshot snapshot : tracked) {
                LivingEntity entity = snapshot.entity();

                if (entity == mc.player || entity.isRemoved() || !entity.isAlive()) {
                    continue;
                }

                float strength = Mth.clamp(snapshot.strength(), 0.0F, 1.0F);
                float fade = strength * strength * (3.0F - 2.0F * strength);

                int alpha = Mth.clamp((int) (fade * 255.0F), 0, 255);

                if (alpha <= 0) {
                    continue;
                }

                outlineBuffer.setColor(OUTLINE_RED, OUTLINE_GREEN, OUTLINE_BLUE, alpha);

                double renderX = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cameraPos.x;
                double renderY = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cameraPos.y;
                double renderZ = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cameraPos.z;

                event.getPoseStack().pushPose();

                dispatcher.render(
                        entity,
                        renderX,
                        renderY,
                        renderZ,
                        Mth.lerp(partialTick, entity.yRotO, entity.getYRot()),
                        partialTick,
                        event.getPoseStack(),
                        outlineBuffer,
                        0x00F000F0
                );

                event.getPoseStack().popPose();
            }

            outlineBuffer.endOutlineBatch();
        } finally {
            dispatcher.setRenderShadow(true);
        }
    }

    private static void detectEntitiesTouchedByWaves(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        List<SonarPingManager.Ping> pings = SonarPingManager.snapshotNewestFirst();

        if (pings.isEmpty()) {
            return;
        }

        long now = Util.getNanos();

        for (SonarPingManager.Ping ping : pings) {
            if (ping == null || !isValidPosition(ping.worldPos)) {
                continue;
            }

            float ageSeconds = (now - ping.timeNanos) / 1_000_000_000.0F;

            if (ageSeconds < 0.0F || ageSeconds >= LIFE_SECONDS) {
                continue;
            }

            double radius = Math.min(ageSeconds * WAVE_SPEED, WAVE_MAX_RADIUS);

            AABB searchBox = new AABB(
                    ping.worldPos.x - radius - 2.0D,
                    ping.worldPos.y - WAVE_ENTITY_VERTICAL_RANGE,
                    ping.worldPos.z - radius - 2.0D,
                    ping.worldPos.x + radius + 2.0D,
                    ping.worldPos.y + WAVE_ENTITY_VERTICAL_RANGE,
                    ping.worldPos.z + radius + 2.0D
            );

            for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != mc.player && !entity.isRemoved() && entity.isAlive())) {
                if (entity.position().distanceToSqr(mc.player.position()) > SOUND_RADIUS_BLOCKS * SOUND_RADIUS_BLOCKS) {
                    continue;
                }

                double verticalDistance = Math.abs(entity.getY() - ping.worldPos.y);

                if (verticalDistance > WAVE_ENTITY_VERTICAL_RANGE) {
                    continue;
                }

                double dx = entity.getX() - ping.worldPos.x;
                double dz = entity.getZ() - ping.worldPos.z;

                double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                AABB entityBox = entity.getBoundingBox();

                double entityHorizontalRadius = Math.max(entityBox.getXsize(), entityBox.getZsize()) * 0.5D;
                double tolerance = WAVE_ENTITY_BASE_TOLERANCE + entityHorizontalRadius + WAVE_SPEED / 20.0D;

                if (Math.abs(horizontalDistance - radius) <= tolerance) {
                    SonarEntityTracker.mark(entity);
                }
            }
        }
    }

    private static void detectEntitiesTouchingSculk(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!SculkedConversionHelper.isTouchingSculk(mc.player)) {
            return;
        }

        AABB searchBox = mc.player.getBoundingBox().inflate(SOUND_RADIUS_BLOCKS);

        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != mc.player && !entity.isRemoved() && entity.isAlive())) {
            if (entity.position().distanceToSqr(mc.player.position()) > SOUND_RADIUS_BLOCKS * SOUND_RADIUS_BLOCKS) {
                continue;
            }

            if (!SculkedConversionHelper.isTouchingSculk(entity)) {
                continue;
            }

            SonarEntityTracker.mark(entity);
        }
    }

    private static void markNearbySoundEntity(Minecraft mc, Vec3 soundPos) {
        if (mc.level == null || mc.player == null || !isValidPosition(soundPos)) {
            return;
        }

        AABB searchBox = new AABB(
                soundPos.x - SOUND_ENTITY_SEARCH_RADIUS,
                soundPos.y - SOUND_ENTITY_SEARCH_RADIUS,
                soundPos.z - SOUND_ENTITY_SEARCH_RADIUS,
                soundPos.x + SOUND_ENTITY_SEARCH_RADIUS,
                soundPos.y + SOUND_ENTITY_SEARCH_RADIUS,
                soundPos.z + SOUND_ENTITY_SEARCH_RADIUS
        );

        LivingEntity nearest = null;
        double nearestDistance = SOUND_ENTITY_SEARCH_RADIUS * SOUND_ENTITY_SEARCH_RADIUS;

        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> entity != mc.player && !entity.isRemoved() && entity.isAlive())) {
            double distance = entity.position().distanceToSqr(soundPos);

            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = entity;
            }
        }

        if (nearest != null) {
            SonarEntityTracker.mark(nearest);
        }
    }

    private static void updateSonarUniforms(Minecraft mc) {
        PostChain chain = sonarPostChain;

        if (chain == null) {
            return;
        }

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        if (width != sonarPostWidth || height != sonarPostHeight) {
            chain.resize(width, height);
            sonarPostWidth = width;
            sonarPostHeight = height;
        }

        float aspect = (float) width / (float) height;

        double fovDegrees = mc.options.fov().get();
        float tanHalfFov = (float) Math.tan(Math.toRadians(fovDegrees * 0.5D));

        chain.setUniform("Aspect", aspect);
        chain.setUniform("TanHalfFov", tanHalfFov);

        chain.setUniform("EdgeThreshold", 0.10F);
        chain.setUniform("EdgeBoost", 1.25F);

        chain.setUniform("SoundLifeSeconds", LIFE_SECONDS);

        chain.setUniform("WaveSpeed", WAVE_SPEED);
        chain.setUniform("WaveMaxRadius", WAVE_MAX_RADIUS);

        chain.setUniform("RingBrightness", RING_BRIGHTNESS);
        chain.setUniform("RingHalfWidth", RING_HALF_WIDTH);
        chain.setUniform("GroundHeightRange", GROUND_HEIGHT_RANGE);

        chain.setUniform("SkyDepthEps", SKY_EPS);

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        Quaternionf inverseCameraRotation = new Quaternionf(camera.rotation()).conjugate();

        Vector3f worldUpView = new Vector3f(0.0F, 1.0F, 0.0F);
        inverseCameraRotation.transform(worldUpView);

        chain.setUniform("WorldUpX", worldUpView.x);
        chain.setUniform("WorldUpY", worldUpView.y);
        chain.setUniform("WorldUpZ", worldUpView.z);

        List<SonarPingManager.Ping> pings = SonarPingManager.snapshotNewestFirst();
        int count = Math.min(SonarPingManager.MAX_PINGS, pings.size());

        chain.setUniform("SoundCount", (float) count);

        long now = Util.getNanos();

        for (int i = 0; i < SonarPingManager.MAX_PINGS; i++) {
            float vx = 0.0F;
            float vy = 0.0F;
            float vz = 0.0F;
            float ageSeconds = 999.0F;

            if (i < count) {
                SonarPingManager.Ping ping = pings.get(i);

                if (ping != null && isValidPosition(ping.worldPos)) {
                    Vec3 delta = ping.worldPos.subtract(cameraPos);

                    Vector3f viewPos = new Vector3f((float) delta.x, (float) delta.y, (float) delta.z);
                    inverseCameraRotation.transform(viewPos);

                    if (viewPos.isFinite() && viewPos.length() <= 256.0F) {
                        vx = viewPos.x;
                        vy = viewPos.y;
                        vz = viewPos.z;
                        ageSeconds = (now - ping.timeNanos) / 1_000_000_000.0F;
                    }
                }
            }

            chain.setUniform("SoundPosX" + i, vx);
            chain.setUniform("SoundPosY" + i, vy);
            chain.setUniform("SoundPosZ" + i, vz);
            chain.setUniform("SoundAge" + i, ageSeconds);
        }
    }

    private static void processSonarPass(Minecraft mc) {
        PostChain chain = sonarPostChain;

        if (chain == null) {
            return;
        }

        try {
            chain.process(0.0F);
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Throwable ignored) {
            closeSonarPostChain();
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    private static boolean ensureSonarPostChain(Minecraft mc) {
        if (sonarPostChain != null) {
            return true;
        }

        try {
            sonarPostChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), SONAR_POST);

            sonarPostWidth = mc.getWindow().getWidth();
            sonarPostHeight = mc.getWindow().getHeight();

            sonarPostChain.resize(sonarPostWidth, sonarPostHeight);

            return true;
        } catch (Throwable ignored) {
            closeSonarPostChain();
            return false;
        }
    }

    private static void closeSonarPostChain() {
        if (sonarPostChain != null) {
            try {
                sonarPostChain.close();
            } catch (Throwable ignored) {
            }
        }

        sonarPostChain = null;
        sonarPostWidth = -1;
        sonarPostHeight = -1;
    }

    private static void pushSound(Minecraft mc, Vec3 pos) {
        if (!isValidPosition(pos)) {
            return;
        }

        if (pos.distanceToSqr(mc.player.position()) > SOUND_RADIUS_BLOCKS * SOUND_RADIUS_BLOCKS) {
            return;
        }

        SonarPingManager.push(pos);
    }

    private static boolean canRenderSonar(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        if (!isSonarActive(mc)) {
            return false;
        }

        return !isVanillaSpectatorShaderEntity(mc.getCameraEntity());
    }

    private static boolean isVanillaSpectatorShaderEntity(Entity entity) {
        return entity instanceof Spider
                || entity instanceof Creeper
                || entity instanceof EnderMan;
    }

    private static boolean isSonarActive(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return false;
        }

        if (mc.level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) {
            return false;
        }

        PlayerCyberwareData data = mc.player.getData(ModAttachments.CYBERWARE);

        return data.hasSpecificItem(ModItems.WETWARE_WARDENANTLERS.get(), CyberwareSlot.EYES);
    }

    private static boolean isSculkConversionActive(Minecraft mc) {
        if (mc.player == null || !mc.player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = mc.player.getData(ModAttachments.CYBERWARE);

        return SculkedConversionHelper.hasFullDefaultSculkConversion(data);
    }

    private static boolean isValidPosition(Vec3 pos) {
        return pos != null
                && Double.isFinite(pos.x)
                && Double.isFinite(pos.y)
                && Double.isFinite(pos.z);
    }
}