package com.perigrine3.createcybernetics.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.item.cyberware.arm.ArmCannonItem;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TrajectoryPreviewClient {

    private static final int MAX_STEPS = 120;

    private static final float ARROW_GRAVITY = 0.05F;
    private static final float ARROW_DRAG = 0.99F;

    private static final float TRIDENT_GRAVITY = 0.05F;
    private static final float TRIDENT_DRAG = 0.99F;

    private static final float THROW_GRAVITY = 0.03F;
    private static final float THROW_DRAG = 0.99F;

    private static final float TNT_GRAVITY = 0.04F;
    private static final float TNT_DRAG = 0.98F;

    private static final float STRAIGHT_GRAVITY = 0.0F;
    private static final float STRAIGHT_DRAG = 1.0F;

    private static boolean externalEnable = true;

    private TrajectoryPreviewClient() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null) {
            return;
        }

        if (!shouldRender(player)) {
            return;
        }

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);

        List<Vec3> points = new ArrayList<>(MAX_STEPS + 2);
        HitTarget firstHit = new HitTarget();

        boolean built = tryBuildBow(player, level, partial, points, firstHit)
                        || tryBuildCrossbow(player, level, partial, points, firstHit)
                        || tryBuildTrident(player, level, partial, points, firstHit)
                        || tryBuildHeldThrowable(player, level, partial, points, firstHit)
                        || tryBuildArmCannon(player, level, partial, points, firstHit);

        if (!built || points.size() < 2) {
            return;
        }

        renderLine(event, mc, points);
        renderFirstHitOverlay(event, mc, level, firstHit, partial);
    }

    private static boolean tryBuildBow(
            LocalPlayer player,
            ClientLevel level,
            float partial,
            List<Vec3> points,
            HitTarget firstHit
    ) {
        if (!player.isUsingItem()) {
            return false;
        }

        ItemStack using = player.getUseItem();
        if (using.isEmpty() || !(using.getItem() instanceof BowItem)) {
            return false;
        }

        int usedTicks = using.getUseDuration(player) - player.getUseItemRemainingTicks();
        float power = BowItem.getPowerForTime(usedTicks);

        if (power <= 0.05F) {
            return false;
        }

        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial).normalize();

        double speed = power * 3.0D;

        Vec3 start = eye.add(0.0D, -0.10D, 0.0D);
        Vec3 velocity = look.scale(speed);

        boolean built = buildTrajectory(
                level,
                player,
                start,
                velocity,
                ARROW_GRAVITY,
                ARROW_DRAG,
                points,
                firstHit
        );

        if (!built) {
            return false;
        }

        applyRenderOffset(player, look, points, 0.45D, 0.16D, 0.02D);
        return true;
    }

    private static boolean tryBuildCrossbow(
            LocalPlayer player,
            ClientLevel level,
            float partial,
            List<Vec3> points,
            HitTarget firstHit
    ) {
        ItemStack held = getHeldCrossbow(player);

        if (held.isEmpty() || !(held.getItem() instanceof CrossbowItem)) {
            return false;
        }

        if (!CrossbowItem.isCharged(held)) {
            return false;
        }

        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial).normalize();

        Vec3 start = eye
                .add(look.scale(0.35D))
                .add(0.0D, -0.10D, 0.0D);

        if (isCrossbowLoadedWithRocket(held)) {
            boolean built = buildTrajectory(
                    level,
                    player,
                    start,
                    look.scale(3.15D),
                    STRAIGHT_GRAVITY,
                    STRAIGHT_DRAG,
                    points,
                    firstHit
            );

            if (!built) {
                return false;
            }

            applyRenderOffset(player, look, points, 0.38D, 0.14D, 0.02D);
            return true;
        }

        boolean built = buildTrajectory(
                level,
                player,
                start,
                look.scale(3.15D),
                ARROW_GRAVITY,
                ARROW_DRAG,
                points,
                firstHit
        );

        if (!built) {
            return false;
        }

        applyRenderOffset(player, look, points, 0.38D, 0.14D, 0.02D);
        return true;
    }

    private static boolean tryBuildTrident(
            LocalPlayer player,
            ClientLevel level,
            float partial,
            List<Vec3> points,
            HitTarget firstHit
    ) {
        if (!player.isUsingItem()) {
            return false;
        }

        ItemStack using = player.getUseItem();

        if (using.isEmpty() || !(using.getItem() instanceof TridentItem)) {
            return false;
        }

        int usedTicks = using.getUseDuration(player) - player.getUseItemRemainingTicks();

        if (usedTicks < 10) {
            return false;
        }

        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial).normalize();

        Vec3 start = eye.add(0.0D, -0.10D, 0.0D);
        Vec3 velocity = look.scale(2.5D);

        boolean built = buildTrajectory(
                level,
                player,
                start,
                velocity,
                TRIDENT_GRAVITY,
                TRIDENT_DRAG,
                points,
                firstHit
        );

        if (!built) {
            return false;
        }

        applyRenderOffset(player, look, points, 0.30D, 0.12D, 0.02D);
        return true;
    }

    private static boolean tryBuildHeldThrowable(
            LocalPlayer player,
            ClientLevel level,
            float partial,
            List<Vec3> points,
            HitTarget firstHit
    ) {
        ItemStack stack = getHeldOrUsing(player);

        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial).normalize();

        Vec3 start = eye
                .add(look.scale(0.25D))
                .add(0.0D, -0.10D, 0.0D);

        double speed;
        float gravity;
        float drag;

        if (item instanceof EnderpearlItem
                || item instanceof SnowballItem
                || item instanceof EggItem) {
            speed = 1.5D;
            gravity = THROW_GRAVITY;
            drag = THROW_DRAG;
        } else if (item instanceof ThrowablePotionItem) {
            speed = 0.5D;
            gravity = 0.05F;
            drag = THROW_DRAG;
        } else if (item instanceof ExperienceBottleItem) {
            speed = 0.7D;
            gravity = 0.07F;
            drag = THROW_DRAG;
        } else if (item instanceof FireworkRocketItem) {
            speed = 1.6D;
            gravity = STRAIGHT_GRAVITY;
            drag = STRAIGHT_DRAG;
        } else if (stack.is(Items.WIND_CHARGE)) {
            speed = 1.5D;
            gravity = STRAIGHT_GRAVITY;
            drag = STRAIGHT_DRAG;
        } else {
            return false;
        }

        boolean built = buildTrajectory(
                level,
                player,
                start,
                look.scale(speed),
                gravity,
                drag,
                points,
                firstHit
        );

        if (!built) {
            return false;
        }

        applyRenderOffset(player, look, points, 0.25D, 0.10D, 0.02D);
        return true;
    }

    private static boolean tryBuildArmCannon(
            LocalPlayer player,
            ClientLevel level,
            float partial,
            List<Vec3> points,
            HitTarget firstHit
    ) {
        if (!player.getMainHandItem().isEmpty()) {
            return false;
        }

        if (!player.getOffhandItem().isEmpty()) {
            return false;
        }

        ItemStack ammo = ArmCannonItem.getSelectedAmmoForTrajectoryPreview(player);

        if (ammo.isEmpty()) {
            return false;
        }

        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial).normalize();

        Vec3 start = eye.add(look.scale(1.0D));

        double speed;
        float gravity;
        float drag;

        if (ammo.is(Tags.Items.NUGGETS)) {
            speed = 5.0D;
            gravity = ARROW_GRAVITY;
            drag = ARROW_DRAG;
        } else if (ammo.is(Items.TNT)) {
            speed = 1.8D;
            gravity = TNT_GRAVITY;
            drag = TNT_DRAG;
        } else if (ammo.is(Items.FIRE_CHARGE) || ammo.is(Items.WIND_CHARGE)) {
            speed = 4.0D;
            gravity = STRAIGHT_GRAVITY;
            drag = STRAIGHT_DRAG;
        } else if (ammo.getItem() instanceof FireworkRocketItem) {
            speed = 2.2D;
            gravity = STRAIGHT_GRAVITY;
            drag = STRAIGHT_DRAG;
        } else if (ammo.getItem() instanceof ArrowItem) {
            speed = 4.0D;
            gravity = ARROW_GRAVITY;
            drag = ARROW_DRAG;
        } else if (ammo.getItem() instanceof ProjectileItem) {
            speed = 4.0D;
            gravity = THROW_GRAVITY;
            drag = THROW_DRAG;
        } else {
            return false;
        }

        return buildTrajectory(
                level,
                player,
                start,
                look.scale(speed),
                gravity,
                drag,
                points,
                firstHit
        );
    }

    private static ItemStack getHeldCrossbow(LocalPlayer player) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof CrossbowItem) {
            return player.getUseItem();
        }

        if (player.getMainHandItem().getItem() instanceof CrossbowItem) {
            return player.getMainHandItem();
        }

        if (player.getOffhandItem().getItem() instanceof CrossbowItem) {
            return player.getOffhandItem();
        }

        return ItemStack.EMPTY;
    }

    private static ItemStack getHeldOrUsing(LocalPlayer player) {
        if (player.isUsingItem()) {
            return player.getUseItem();
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            return mainHand;
        }

        return player.getOffhandItem();
    }

    private static boolean isCrossbowLoadedWithRocket(ItemStack crossbow) {
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);

        if (charged == null) {
            return false;
        }

        List<ItemStack> loaded = charged.getItems();

        if (loaded == null || loaded.isEmpty()) {
            return false;
        }

        for (ItemStack stack : loaded) {
            if (!stack.isEmpty() && stack.is(Items.FIREWORK_ROCKET)) {
                return true;
            }
        }

        return false;
    }

    private static boolean buildTrajectory(
            ClientLevel level,
            LocalPlayer player,
            Vec3 startPos,
            Vec3 startVelocity,
            float gravity,
            float drag,
            List<Vec3> points,
            HitTarget firstHit
    ) {
        points.clear();
        firstHit.clear();

        Vec3 position = startPos;
        Vec3 velocity = startVelocity;

        points.add(position);

        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3 nextPosition = position.add(velocity);

            HitResult hit = clipFirst(level, player, position, nextPosition);

            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getLocation());
                firstHit.setFrom(hit);
                break;
            }

            points.add(nextPosition);

            velocity = velocity
                    .scale(drag)
                    .add(0.0D, -gravity, 0.0D);

            position = nextPosition;
        }

        return points.size() >= 2;
    }

    private static void applyRenderOffset(
            LocalPlayer player,
            Vec3 lookUnit,
            List<Vec3> points,
            double forward,
            double side,
            double yDelta
    ) {
        if (points.isEmpty()) {
            return;
        }

        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 right = lookUnit.cross(up);

        if (right.lengthSqr() < 1.0E-6D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }

        int armSign = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;

        Vec3 initialOffset = lookUnit
                .scale(forward)
                .add(right.scale(side * armSign))
                .add(0.0D, yDelta, 0.0D);

        int pointCount = points.size();

        for (int i = 0; i < pointCount; i++) {
            double progress = (double) i / (double) Math.max(1, pointCount - 1);
            double strength = 1.0D - progress;

            strength *= strength;

            if (strength <= 0.0D) {
                continue;
            }

            points.set(i, points.get(i).add(initialOffset.scale(strength)));
        }
    }

    private static HitResult clipFirst(
            ClientLevel level,
            LocalPlayer player,
            Vec3 from,
            Vec3 to
    ) {
        BlockHitResult blockHit = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        EntityHitResult entityHit = clipEntities(level, player, from, to);

        if (entityHit == null) {
            return blockHit;
        }

        if (blockHit.getType() == HitResult.Type.MISS) {
            return entityHit;
        }

        double blockDistance = from.distanceToSqr(blockHit.getLocation());
        double entityDistance = from.distanceToSqr(entityHit.getLocation());

        return entityDistance <= blockDistance ? entityHit : blockHit;
    }

    private static EntityHitResult clipEntities(
            ClientLevel level,
            LocalPlayer player,
            Vec3 from,
            Vec3 to
    ) {
        AABB sweepBox = new AABB(from, to).inflate(0.75D);

        Entity closestEntity = null;
        Vec3 closestHit = null;
        double closestDistance = Double.MAX_VALUE;

        List<Entity> entities = level.getEntities(
                player,
                sweepBox,
                entity -> entity != player
                        && entity.isAlive()
                        && entity.isPickable()
        );

        for (Entity entity : entities) {
            AABB hitbox = entity.getBoundingBox().inflate(0.30D);
            var clipped = hitbox.clip(from, to);

            if (clipped.isEmpty()) {
                continue;
            }

            Vec3 hit = clipped.get();
            double distance = from.distanceToSqr(hit);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestEntity = entity;
                closestHit = hit;
            }
        }

        if (closestEntity == null || closestHit == null) {
            return null;
        }

        return new EntityHitResult(closestEntity, closestHit);
    }

    private static void renderLine(
            RenderLevelStageEvent event,
            Minecraft mc,
            List<Vec3> points
    ) {
        RenderSystem.lineWidth(3.0F);

        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());

        var matrix = poseStack.last().pose();

        int red = 255;
        int green = 160;
        int blue = 40;
        int alpha = 255;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 first = points.get(i).subtract(camera);
            Vec3 second = points.get(i + 1).subtract(camera);

            vertexConsumer
                    .addVertex(matrix, (float) first.x, (float) first.y, (float) first.z)
                    .setColor(red, green, blue, alpha)
                    .setNormal(0.0F, 1.0F, 0.0F);

            vertexConsumer
                    .addVertex(matrix, (float) second.x, (float) second.y, (float) second.z)
                    .setColor(red, green, blue, alpha)
                    .setNormal(0.0F, 1.0F, 0.0F);
        }

        buffer.endBatch(RenderType.lines());

        RenderSystem.lineWidth(1.0F);
    }

    private static void renderFirstHitOverlay(
            RenderLevelStageEvent event,
            Minecraft mc,
            ClientLevel level,
            HitTarget hit,
            float partial
    ) {
        if (hit.type == HitTargetType.NONE) {
            return;
        }

        OutlineBufferSource outlines = mc.renderBuffers().outlineBufferSource();
        outlines.setColor(255, 160, 40, 255);

        PoseStack poseStack = event.getPoseStack();

        if (hit.type == HitTargetType.ENTITY) {
            Entity entity = level.getEntity(hit.entityId);

            if (entity == null || !entity.isAlive()) {
                return;
            }

            Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

            dispatcher.render(
                    entity,
                    entity.getX() - camera.x,
                    entity.getY() - camera.y,
                    entity.getZ() - camera.z,
                    entity.getYRot(),
                    partial,
                    poseStack,
                    outlines,
                    0x00F000F0
            );

            outlines.endOutlineBatch();
            return;
        }

        if (hit.type == HitTargetType.BLOCK) {
            BlockPos position = hit.blockPos;

            if (position == null) {
                return;
            }

            Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();

            AABB box = level.getBlockState(position)
                    .getShape(level, position)
                    .bounds()
                    .move(position);

            if (box.getSize() < 1.0E-6D) {
                box = new AABB(position);
            }

            renderBoxOutline(
                    poseStack,
                    mc.renderBuffers().bufferSource(),
                    camera,
                    box
            );
        }
    }

    private static void renderBoxOutline(
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffer,
            Vec3 camera,
            AABB worldBox
    ) {
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lines());
        var matrix = poseStack.last().pose();

        AABB box = worldBox.move(-camera.x, -camera.y, -camera.z);

        int red = 255;
        int green = 160;
        int blue = 40;
        int alpha = 255;

        drawBoxLines(vertexConsumer, matrix, box, red, green, blue, alpha);

        double epsilon = 0.0035D;

        for (double offsetX : new double[]{-epsilon, epsilon}) {
            for (double offsetY : new double[]{-epsilon, epsilon}) {
                for (double offsetZ : new double[]{-epsilon, epsilon}) {
                    drawBoxLines(
                            vertexConsumer,
                            matrix,
                            box.move(offsetX, offsetY, offsetZ),
                            red,
                            green,
                            blue,
                            alpha
                    );
                }
            }
        }

        buffer.endBatch(RenderType.lines());
    }

    private static void drawBoxLines(
            VertexConsumer vertexConsumer,
            org.joml.Matrix4f matrix,
            AABB box,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;

        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        line(vertexConsumer, matrix, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);

        line(vertexConsumer, matrix, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);

        line(vertexConsumer, matrix, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        line(vertexConsumer, matrix, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void line(
            VertexConsumer vertexConsumer,
            org.joml.Matrix4f matrix,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        vertexConsumer
                .addVertex(matrix, x0, y0, z0)
                .setColor(red, green, blue, alpha)
                .setNormal(0.0F, 1.0F, 0.0F);

        vertexConsumer
                .addVertex(matrix, x1, y1, z1)
                .setColor(red, green, blue, alpha)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private static boolean shouldRender(LocalPlayer player) {
        if (!externalEnable) {
            return false;
        }

        if (!player.hasData(ModAttachments.CYBERWARE)) {
            return false;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null) {
            return false;
        }

        InstalledLoc trajectoryCalculator = findInstalled(
                data,
                ModItems.EYEUPGRADES_TRAJECTORYCALCULATOR.get(),
                CyberwareSlot.EYES
        );

        if (trajectoryCalculator == null || !isEnabledIfToggleable(data, trajectoryCalculator)) {
            return false;
        }

        return hasWorkingVisualInterface(data);
    }

    private static boolean hasWorkingVisualInterface(PlayerCyberwareData data) {
        InstalledLoc monovision = findInstalled(
                data,
                ModItems.EYEUPGRADES_MONOVISION.get(),
                CyberwareSlot.EYES
        );

        if (monovision != null) {
            return isEnabledIfToggleable(data, monovision);
        }

        boolean hasCybereyes = findInstalled(
                data,
                ModItems.BASECYBERWARE_CYBEREYES.get(),
                CyberwareSlot.EYES
        ) != null
                || findInstalled(
                data,
                ModItems.EYEUPGRADES_MULTIOPTICS1.get(),
                CyberwareSlot.EYES
        ) != null
                || findInstalled(
                data,
                ModItems.EYEUPGRADES_MULTIOPTICS2.get(),
                CyberwareSlot.EYES
        ) != null
                || findInstalled(
                data,
                ModItems.EYEUPGRADES_MULTIOPTICS3.get(),
                CyberwareSlot.EYES
        ) != null
                || findInstalled(
                data,
                ModItems.EYEUPGRADES_MULTIOPTICS4.get(),
                CyberwareSlot.EYES
        ) != null;

        if (!hasCybereyes) {
            return false;
        }

        InstalledLoc hudjack = findInstalled(
                data,
                ModItems.EYEUPGRADES_HUDJACK.get(),
                CyberwareSlot.EYES
        );

        return hudjack != null && isEnabledIfToggleable(data, hudjack);
    }

    private static boolean isEnabledIfToggleable(PlayerCyberwareData data, InstalledLoc loc) {
        if (loc.stack().isEmpty()) {
            return false;
        }

        if (!loc.stack().is(ModTags.Items.TOGGLEABLE_CYBERWARE)) {
            return true;
        }

        return data.isEnabled(loc.slot(), loc.index());
    }

    private static InstalledLoc findInstalled(PlayerCyberwareData data, Item item, CyberwareSlot slot) {
        InstalledCyberware[] installed = data.getAll().get(slot);

        if (installed == null) {
            return null;
        }

        for (int index = 0; index < installed.length; index++) {
            InstalledCyberware cyberware = installed[index];

            if (cyberware == null) {
                continue;
            }

            ItemStack stack = cyberware.getItem();

            if (stack == null || stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() == item) {
                return new InstalledLoc(slot, index, stack);
            }
        }

        return null;
    }

    private record InstalledLoc(CyberwareSlot slot, int index, ItemStack stack) {}

    public static void setExternallyEnabled(boolean enabled) {
        externalEnable = enabled;
    }

    private enum HitTargetType {
        NONE,
        BLOCK,
        ENTITY
    }

    private static final class HitTarget {
        private HitTargetType type = HitTargetType.NONE;
        private BlockPos blockPos;
        private int entityId = -1;

        private void clear() {
            type = HitTargetType.NONE;
            blockPos = null;
            entityId = -1;
        }

        private void setFrom(HitResult hit) {
            clear();

            if (hit instanceof EntityHitResult entityHit) {
                type = HitTargetType.ENTITY;
                entityId = entityHit.getEntity().getId();
                return;
            }

            if (hit instanceof BlockHitResult blockHit) {
                type = HitTargetType.BLOCK;
                blockPos = blockHit.getBlockPos();
            }
        }
    }
}