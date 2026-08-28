package com.perigrine3.createcybernetics.item.generic;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.capabilities.ModAttachments;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.quickhack.MicroprogramDefinition;
import com.perigrine3.createcybernetics.component.MicroprogramLibrary;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class MicroprogramItem extends Item {
    private static final int DOWNLOAD_DURATION = 40;
    private static final int PROGRESS_BAR_SEGMENTS = 20;

    private final MicroprogramDefinition definition;

    public MicroprogramItem(Properties properties, MicroprogramDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    public MicroprogramDefinition getDefinition() {
        return definition;
    }

    public ResourceLocation getMicroprogramId() {
        return ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, definition.id());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(definition.displayName()).withStyle(getCategoryFormatting());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(getCategoryName()).withStyle(getCategoryFormatting()));
        tooltip.add(Component.translatable("microprogram.createcybernetics." + definition.id() + ".desc").withStyle(getCategoryFormatting()));
        tooltip.add(Component.literal("RAM Cost: " + definition.ramCost()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Hold Right Click to Download").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.cyberspace_only").withStyle(ChatFormatting.RED), true);
            }

            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!level.dimension().equals(ModDimensions.CYBERSPACE_LEVEL)) return;

        int elapsedTicks = DOWNLOAD_DURATION - remainingUseDuration;
        int percent = Mth.clamp(Math.round((elapsedTicks / (float) DOWNLOAD_DURATION) * 100.0F), 0, 100);
        float progress = percent / 100.0F;

        displayDownloadProgress(player, percent, progress);
        spawnDownloadParticles(serverLevel, player, player.getUsedItemHand(), progress);

        if (remainingUseDuration <= 1) {
            completeDownload(player, serverLevel, stack);
            player.stopUsingItem();
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DOWNLOAD_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }

    private void completeDownload(ServerPlayer player, ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return;

        if (!player.hasData(ModAttachments.CYBERWARE)) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.no_cyberdeck").withStyle(ChatFormatting.RED), true);
            return;
        }

        PlayerCyberwareData data = player.getData(ModAttachments.CYBERWARE);

        if (data == null || !data.hasSpecificItem(ModItems.BRAINUPGRADES_CYBERDECK.get(), CyberwareSlot.BRAIN)) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.no_cyberdeck").withStyle(ChatFormatting.RED), true);
            return;
        }

        ItemStack libraryShard = data.getCyberdeckStack(PlayerCyberwareData.CYBERDECK_MICROPROGRAM_CHIP_SLOT);

        if (libraryShard.isEmpty() || !libraryShard.is(ModItems.DATA_SHARD_MICROPROGRAMS.get())) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.no_library").withStyle(ChatFormatting.RED), true);
            return;
        }

        ResourceLocation microprogramId = getMicroprogramId();
        MicroprogramLibrary library = MicroprogramDataShardItem.getLibrary(libraryShard);

        if (library.contains(microprogramId)) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.already_downloaded", definition.displayName()).withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        if (library.isFull()) {
            player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.library_full").withStyle(ChatFormatting.RED), true);
            return;
        }

        ItemStack updatedShard = libraryShard.copy();

        if (!MicroprogramDataShardItem.addMicroprogram(updatedShard, microprogramId)) return;

        data.setCyberdeckStack(PlayerCyberwareData.CYBERDECK_MICROPROGRAM_CHIP_SLOT, updatedShard);
        data.setDirty();
        ModAttachments.syncCyberware(player);

        spawnCompletionBurst(level, player, player.getUsedItemHand());

        stack.shrink(1);

        player.displayClientMessage(Component.translatable("message.createcybernetics.microprogram.downloaded", definition.displayName()).withStyle(getCategoryFormatting()), true);
    }

    private void displayDownloadProgress(ServerPlayer player, int percent, float progress) {
        int filledSegments = Mth.clamp(Math.round(progress * PROGRESS_BAR_SEGMENTS), 0, PROGRESS_BAR_SEGMENTS);
        String filled = "█".repeat(filledSegments);
        String empty = "░".repeat(PROGRESS_BAR_SEGMENTS - filledSegments);

        Component message = Component.literal("DOWNLOADING ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(definition.displayName()).withStyle(getCategoryFormatting()))
                .append(Component.literal("  [").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(filled).withStyle(getCategoryFormatting()))
                .append(Component.literal(empty).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(percent + "%").withStyle(ChatFormatting.WHITE));

        player.displayClientMessage(message, true);
    }

    private void spawnDownloadParticles(ServerLevel level, ServerPlayer player, InteractionHand hand, float progress) {
        DustParticleOptions particle = getCategoryParticle();
        Vec3 handPos = getHandPosition(player, hand);
        int particleCount = 1 + Mth.floor(progress * 7.0F);

        for (int i = 0; i < particleCount; i++) {
            double x = handPos.x + (player.getRandom().nextDouble() - 0.5D) * 0.18D;
            double y = handPos.y + (player.getRandom().nextDouble() - 0.5D) * 0.12D;
            double z = handPos.z + (player.getRandom().nextDouble() - 0.5D) * 0.18D;

            double xSpeed = (player.getRandom().nextDouble() - 0.5D) * 0.025D;
            double ySpeed = 0.025D + player.getRandom().nextDouble() * (0.025D + progress * 0.035D);
            double zSpeed = (player.getRandom().nextDouble() - 0.5D) * 0.025D;

            level.sendParticles(particle, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0D);
        }
    }

    private void spawnCompletionBurst(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        DustParticleOptions particle = getCategoryParticle();
        Vec3 handPos = getHandPosition(player, hand);

        for (int i = 0; i < 24; i++) {
            double x = handPos.x + (player.getRandom().nextDouble() - 0.5D) * 0.16D;
            double y = handPos.y + (player.getRandom().nextDouble() - 0.5D) * 0.12D;
            double z = handPos.z + (player.getRandom().nextDouble() - 0.5D) * 0.16D;

            double xSpeed = (player.getRandom().nextDouble() - 0.5D) * 0.16D;
            double ySpeed = 0.06D + player.getRandom().nextDouble() * 0.12D;
            double zSpeed = (player.getRandom().nextDouble() - 0.5D) * 0.16D;

            level.sendParticles(particle, x, y, z, 0, xSpeed, ySpeed, zSpeed, 1.0D);
        }
    }

    private Vec3 getHandPosition(ServerPlayer player, InteractionHand hand) {
        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0D, look.z);

        if (horizontalLook.lengthSqr() < 0.0001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        Vec3 right = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
        boolean mainArmRight = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        boolean usingRightArm = hand == InteractionHand.MAIN_HAND ? mainArmRight : !mainArmRight;
        double side = usingRightArm ? 1.0D : -1.0D;

        return player.position().add(horizontalLook.scale(0.28D)).add(right.scale(0.34D * side)).add(0.0D, 1.05D, 0.0D);
    }

    private DustParticleOptions getCategoryParticle() {
        return switch (definition.category()) {
            case TARGET -> new DustParticleOptions(new Vector3f(0.15F, 0.95F, 1.0F), 0.85F);
            case FUNCTION -> new DustParticleOptions(new Vector3f(0.2F, 1.0F, 0.25F), 0.85F);
            case MODIFIER -> new DustParticleOptions(new Vector3f(0.8F, 0.3F, 1.0F), 0.85F);
        };
    }

    private String getCategoryName() {
        return switch (definition.category()) {
            case TARGET -> "TARGET";
            case FUNCTION -> "FUNCTION";
            case MODIFIER -> "MODIFIER";
        };
    }

    private ChatFormatting getCategoryFormatting() {
        return switch (definition.category()) {
            case TARGET -> ChatFormatting.AQUA;
            case FUNCTION -> ChatFormatting.GREEN;
            case MODIFIER -> ChatFormatting.LIGHT_PURPLE;
        };
    }
}