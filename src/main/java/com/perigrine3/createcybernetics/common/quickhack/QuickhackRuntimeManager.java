package com.perigrine3.createcybernetics.common.quickhack;

import com.perigrine3.createcybernetics.CreateCybernetics;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.attributes.ModAttributes;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.network.payload.CyberwareEnabledStatePayload;
import com.perigrine3.createcybernetics.network.payload.QuickhackClientStatePayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = CreateCybernetics.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class QuickhackRuntimeManager {
    private static final ResourceLocation CORRUPT_HUMANITY_MODIFIER = ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "quickhack_corrupt_humanity");
    private static final int OVERCLOCK_TOGGLE_DELAY = 5;

    private static final int DESYNC_POWERED_MIN_TICKS = 60;
    private static final int DESYNC_POWERED_MAX_TICKS = 140;
    private static final int DESYNC_UNPOWERED_MIN_TICKS = 100;
    private static final int DESYNC_UNPOWERED_MAX_TICKS = 240;

    private static final int MALFUNCTION_POWERED_MIN_TICKS = 60;
    private static final int MALFUNCTION_POWERED_MAX_TICKS = 140;
    private static final int MALFUNCTION_UNPOWERED_MIN_TICKS = 100;
    private static final int MALFUNCTION_UNPOWERED_MAX_TICKS = 240;

    private static final int MALFUNCTION_MOVEMENT_NORMAL_MIN_TICKS = 40;
    private static final int MALFUNCTION_MOVEMENT_NORMAL_MAX_TICKS = 100;
    private static final int MALFUNCTION_MOVEMENT_INVERTED_MIN_TICKS = 40;
    private static final int MALFUNCTION_MOVEMENT_INVERTED_MAX_TICKS = 100;

    private static final List<ScheduledAction> ACTIONS = new ArrayList<>();
    private static final List<PlayerToggleAction> PLAYER_TOGGLE_ACTIONS = new ArrayList<>();
    private static final List<RuntimeEffect> EFFECTS = new ArrayList<>();
    private static final Map<RuntimeEffect, PowerFluctuationState> POWER_FLUCTUATIONS = new HashMap<>();
    private static final Map<RuntimeEffect, MovementInvertState> MOVEMENT_INVERSION_STATES = new HashMap<>();

    private QuickhackRuntimeManager() {}

    public static void schedule(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, int delayTicks, Runnable action) {
        if (context == null || context.target() == null || action == null) return;
        long executeAt = context.target().level().getGameTime() + Math.max(0, delayTicks);
        ACTIONS.add(new ScheduledAction(context, refKeys(refs), executeAt, action));
    }

    public static void applyReboot(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent) {
        addEffect(context, refs, domain, RuntimeEffectType.REBOOT, durationTicks, persistent, 1.0F, 0);
    }

    public static void applyOverclock(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent, float intensity) {
        addEffect(context, refs, domain, RuntimeEffectType.OVERCLOCK, durationTicks, persistent, Math.max(1.0F, intensity), 0);
    }

    public static void applyDrain(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent) {
        addEffect(context, refs, domain, RuntimeEffectType.DRAIN, durationTicks, persistent, 2.0F, 0);
    }

    public static void applyCorrupt(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent, int humanityPenalty) {
        addEffect(context, refs, domain, RuntimeEffectType.CORRUPT, durationTicks, persistent, 1.0F, Math.max(0, humanityPenalty));
    }

    public static void applyDesync(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent) {
        addEffect(context, refs, domain, RuntimeEffectType.DESYNC, durationTicks, persistent, 1.0F, 0);
    }

    public static void applyMalfunction(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent) {
        addEffect(context, refs, domain, RuntimeEffectType.MALFUNCTION, durationTicks, persistent, 1.0F, 0);
    }

    public static void applyLoop(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, int durationTicks, boolean persistent) {
        addEffect(context, refs, domain, RuntimeEffectType.LOOP, durationTicks, persistent, 1.0F, 0);
    }

    public static boolean isRebooted(LivingEntity entity, InstalledCyberware installed) {
        if (entity == null || installed == null) return false;
        return hasEffect(entity, installed.getSlot(), installed.getIndex(), RuntimeEffectType.REBOOT);
    }

    public static boolean isOverclocked(LivingEntity entity, CyberwareSlot slot, int index) {
        return hasEffect(entity, slot, index, RuntimeEffectType.OVERCLOCK);
    }

    public static int getEnergyConsumptionMultiplier(LivingEntity entity, CyberwareSlot slot, int index) {
        if (entity == null || slot == null) return 1;
        RefKey key = new RefKey(slot, index);
        int multiplier = 1;

        for (RuntimeEffect effect : EFFECTS) {
            if (effect.type() != RuntimeEffectType.DRAIN) continue;
            if (!effect.targetId().equals(entity.getUUID())) continue;
            if (!effect.domain().isGlobalDrainTarget() && !effect.refs().contains(key)) continue;
            multiplier = Math.max(multiplier, Math.max(1, Math.round(effect.magnitude())));
        }

        return multiplier;
    }

    public static boolean isForcedPowerOff(LivingEntity entity, CyberwareSlot slot, int index) {
        if (entity == null || slot == null || index < 0) return false;

        RefKey key = new RefKey(slot, index);

        for (RuntimeEffect effect : EFFECTS) {
            if (effect.type() != RuntimeEffectType.DESYNC && effect.type() != RuntimeEffectType.MALFUNCTION) continue;
            if (!effect.targetId().equals(entity.getUUID())) continue;
            if (!effect.refs().contains(key)) continue;

            PowerFluctuationState state = POWER_FLUCTUATIONS.get(effect);
            if (state != null && !state.powered()) return true;
        }

        return false;
    }

    public static int getRuntimeHumanityPenalty(Player player) {
        if (player == null) return 0;
        int penalty = 0;

        for (RuntimeEffect effect : EFFECTS) {
            if (effect.type() != RuntimeEffectType.CORRUPT) continue;
            if (!effect.targetId().equals(player.getUUID())) continue;
            penalty += Math.max(0, effect.humanityPenalty());
        }

        return penalty;
    }

    public static boolean isArmInteractionDesynced(Player player, CyberwareSlot armSlot) {
        if (player == null || armSlot == null) return false;

        for (RuntimeEffect effect : EFFECTS) {
            if (effect.type() != RuntimeEffectType.DESYNC && effect.type() != RuntimeEffectType.MALFUNCTION) continue;
            if (!effect.targetId().equals(player.getUUID())) continue;
            if (!effect.domain().affectsArms()) continue;
            if (effect.refs().isEmpty() || containsSlot(effect.refs(), armSlot)) return true;
        }

        return false;
    }

    public static boolean hasOverclockedArms(Player player, CyberwareSlot armSlot) {
        if (player == null || armSlot == null) return false;

        for (RuntimeEffect effect : EFFECTS) {
            if (effect.type() != RuntimeEffectType.OVERCLOCK) continue;
            if (!effect.targetId().equals(player.getUUID())) continue;
            if (!effect.domain().affectsArms()) continue;
            if (effect.refs().isEmpty() || containsSlot(effect.refs(), armSlot)) return true;
        }

        return false;
    }

    public static boolean shouldDelayToggle(LivingEntity entity, CyberwareSlot slot, int index) {
        return isOverclocked(entity, slot, index);
    }

    public static void requestPlayerToggle(ServerPlayer player, PlayerCyberwareData data, CyberwareSlot slot, int index, boolean enabled) {
        if (player == null || data == null || slot == null) return;
        InstalledCyberware installed = data.get(slot, index);
        if (installed == null) return;

        if (!shouldDelayToggle(player, slot, index)) {
            data.setEnabled(slot, index, enabled);
            data.setDirty();
            PacketDistributor.sendToPlayer(player, new CyberwareEnabledStatePayload(slot.name(), index, enabled));
            return;
        }

        PLAYER_TOGGLE_ACTIONS.removeIf(action -> action.playerId().equals(player.getUUID()) && action.slot() == slot && action.index() == index);
        PLAYER_TOGGLE_ACTIONS.add(new PlayerToggleAction(player, data, player.getUUID(), slot, index, enabled, player.level().getGameTime() + OVERCLOCK_TOGGLE_DELAY));
    }

    public static int purge(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs) {
        if (context == null || context.target() == null) return 0;

        Set<RefKey> keys = refKeys(refs);
        UUID targetId = context.target().getUUID();
        int removed = 0;

        Iterator<RuntimeEffect> effectIterator = EFFECTS.iterator();
        while (effectIterator.hasNext()) {
            RuntimeEffect effect = effectIterator.next();
            if (!effect.targetId().equals(targetId)) continue;
            if (!keys.isEmpty() && !effect.refs().isEmpty() && !overlaps(keys, effect.refs())) continue;
            restoreRuntimeState(effect);
            effectIterator.remove();
            removed++;
        }

        Iterator<ScheduledAction> actionIterator = ACTIONS.iterator();
        while (actionIterator.hasNext()) {
            ScheduledAction action = actionIterator.next();
            if (!action.context().target().getUUID().equals(targetId)) continue;
            if (!keys.isEmpty() && !action.keys().isEmpty() && !overlaps(keys, action.keys())) continue;
            actionIterator.remove();
            removed++;
        }

        if (context.target() instanceof ServerPlayer player) syncCorruptHumanityModifier(player);
        syncClientState(context.target());
        context.sync();
        return removed;
    }

    public static void requestToggle(QuickhackExecutionContext context, QuickhackCyberwareRef ref, boolean enabled) {
        if (context == null || ref == null) return;

        if (shouldDelayToggle(context.target(), ref.slot(), ref.index())) {
            schedule(context, List.of(ref), OVERCLOCK_TOGGLE_DELAY, () -> {
                context.setEnabled(ref, enabled);
                context.sync();
            });
            return;
        }

        context.setEnabled(ref, enabled);
        context.sync();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        processEffects();
        processScheduledActions();
        processPlayerToggleActions();
    }

    private static void addEffect(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs, QuickhackTargetDomain domain, RuntimeEffectType type, int durationTicks, boolean persistent, float magnitude, int humanityPenalty) {
        if (context == null || context.target() == null) return;

        Set<RefKey> keys = refKeys(refs);
        removeMatchingEffect(context.target(), keys, type);

        long expireAt = context.target().level().getGameTime() + Math.max(1, durationTicks);
        List<StateSnapshot> snapshots = needsStateRestore(type) ? captureState(context, refs) : List.of();
        RuntimeEffect effect = new RuntimeEffect(context, context.target().getUUID(), keys, domain == null ? QuickhackTargetDomain.SYSTEM_ALL : domain, type, expireAt, magnitude, humanityPenalty, snapshots);
        EFFECTS.add(effect);

        if (context.target() instanceof ServerPlayer player) syncCorruptHumanityModifier(player);
        syncClientState(context.target());
    }

    private static void processEffects() {
        if (EFFECTS.isEmpty()) return;

        Iterator<RuntimeEffect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            RuntimeEffect effect = iterator.next();
            LivingEntity target = effect.context().target();

            if (target == null || target.isRemoved() || !target.isAlive()) {
                restoreRuntimeState(effect);
                iterator.remove();
                continue;
            }

            if (target.level().getGameTime() >= effect.expireAt()) {
                restoreRuntimeState(effect);
                iterator.remove();
                if (target instanceof ServerPlayer player) syncCorruptHumanityModifier(player);
                syncClientState(target);
                continue;
            }

            if (effect.type() == RuntimeEffectType.DESYNC) tickDesync(effect);
            if (effect.type() == RuntimeEffectType.MALFUNCTION) tickMalfunction(effect);
            if (effect.type() == RuntimeEffectType.LOOP) tickLoop(effect);
        }
    }

    private static void tickDesync(RuntimeEffect effect) {
        if (effect.domain() == QuickhackTargetDomain.ARMS || effect.domain() == QuickhackTargetDomain.LEGS || effect.domain() == QuickhackTargetDomain.MOTOR_CONTROL || effect.domain() == QuickhackTargetDomain.CYBEROPTICS) return;
        tickPowerFluctuation(effect);
    }

    private static void tickMalfunction(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();
        if (target.level().getGameTime() % 10L != 0L) return;

        switch (effect.domain()) {
            case ARMS -> malfunctionArms(target);
            case LEGS, MOTOR_CONTROL -> tickMalfunctionMovement(effect);
            case CYBEROPTICS -> {}
            case AUDITORY -> malfunctionAuditory(target);
            case POWER_SYSTEM -> malfunctionPower(effect);
            case PROCESSOR, CYBERDECK, NEURAL_SYSTEM -> malfunctionNeural(effect);
            default -> {
                malfunctionCyberware(effect);

                if (effect.domain().affectsLegs()) {
                    tickMalfunctionMovement(effect);
                }
            }
        }
    }

    private static void malfunctionArms(LivingEntity target) {
        if (target.getRandom().nextFloat() >= 0.35F) return;
        target.swing(target.getRandom().nextBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (target.isUsingItem() && target.getRandom().nextBoolean()) target.stopUsingItem();
    }

    private static void malfunctionAuditory(LivingEntity target) {
        if (target.getRandom().nextFloat() < 0.25F) target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30, 0, false, false, false));
    }

    private static void malfunctionPower(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();

        if (target.getRandom().nextFloat() < 0.55F) {
            effect.context().extractEnergy(50 + target.getRandom().nextInt(151));
        }

        tickPowerFluctuation(effect);
    }

    private static void malfunctionNeural(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();
        if (target.getRandom().nextFloat() < 0.25F) target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 30, 0, false, false, false));
        malfunctionCyberware(effect);
    }

    private static void malfunctionCyberware(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();

        for (RefKey key : effect.refs()) {
            QuickhackCyberwareRef ref = resolveRef(effect.context(), key);
            if (ref == null || ref.installed() == null) continue;
            if (ref.slot() == CyberwareSlot.EYES) continue;

            ItemStack stack = ref.installed().getItem();
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ICyberwareItem item)) continue;
            if (item.storesEnergy(target, stack, ref.slot()) || item.getEnergyGeneratedPerTick(target, stack, ref.slot()) > 0) continue;
            if (!item.isToggleableByWheel(stack, ref.slot())) continue;
            if (target.getRandom().nextFloat() < 0.20F) effect.context().setEnabled(ref, !effect.context().isEnabled(ref));
        }

        tickPowerFluctuation(effect);
        effect.context().sync();
    }

    private static void tickPowerFluctuation(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();
        long gameTime = target.level().getGameTime();

        PowerFluctuationState state = POWER_FLUCTUATIONS.get(effect);

        if (state == null) {
            POWER_FLUCTUATIONS.put(effect, new PowerFluctuationState(true, gameTime + randomPowerStateDuration(effect, true)));
            return;
        }

        if (gameTime < state.nextChangeAt()) return;

        boolean powered = !state.powered();
        POWER_FLUCTUATIONS.put(effect, new PowerFluctuationState(powered, gameTime + randomPowerStateDuration(effect, powered)));
    }

    private static int randomPowerStateDuration(RuntimeEffect effect, boolean powered) {
        LivingEntity target = effect.context().target();

        int minTicks;
        int maxTicks;

        if (effect.type() == RuntimeEffectType.DESYNC) {
            if (powered) {
                minTicks = DESYNC_POWERED_MIN_TICKS;
                maxTicks = DESYNC_POWERED_MAX_TICKS;
            } else {
                minTicks = DESYNC_UNPOWERED_MIN_TICKS;
                maxTicks = DESYNC_UNPOWERED_MAX_TICKS;
            }
        } else {
            if (powered) {
                minTicks = MALFUNCTION_POWERED_MIN_TICKS;
                maxTicks = MALFUNCTION_POWERED_MAX_TICKS;
            } else {
                minTicks = MALFUNCTION_UNPOWERED_MIN_TICKS;
                maxTicks = MALFUNCTION_UNPOWERED_MAX_TICKS;
            }
        }

        return minTicks + target.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    private static void tickMalfunctionMovement(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();
        long gameTime = target.level().getGameTime();

        MovementInvertState state = MOVEMENT_INVERSION_STATES.get(effect);

        if (state == null) {
            boolean inverted = target.getRandom().nextBoolean();
            MOVEMENT_INVERSION_STATES.put(effect, new MovementInvertState(inverted, gameTime + randomMovementStateDuration(target, inverted)));

            if (inverted) {
                syncClientState(target);
            }

            return;
        }

        if (gameTime < state.nextChangeAt()) return;

        boolean inverted = !state.inverted();
        MOVEMENT_INVERSION_STATES.put(effect, new MovementInvertState(inverted, gameTime + randomMovementStateDuration(target, inverted)));
        syncClientState(target);
    }

    private static int randomMovementStateDuration(LivingEntity target, boolean inverted) {
        int minTicks = inverted ? MALFUNCTION_MOVEMENT_INVERTED_MIN_TICKS : MALFUNCTION_MOVEMENT_NORMAL_MIN_TICKS;
        int maxTicks = inverted ? MALFUNCTION_MOVEMENT_INVERTED_MAX_TICKS : MALFUNCTION_MOVEMENT_NORMAL_MAX_TICKS;

        return minTicks + target.getRandom().nextInt(maxTicks - minTicks + 1);
    }

    private static void tickLoop(RuntimeEffect effect) {
        LivingEntity target = effect.context().target();
        if (target.level().getGameTime() % 20L != 0L) return;

        for (RefKey key : effect.refs()) {
            QuickhackCyberwareRef ref = resolveRef(effect.context(), key);
            if (ref == null) continue;
            effect.context().setEnabled(ref, !effect.context().isEnabled(ref));
        }

        effect.context().sync();
    }

    private static void processScheduledActions() {
        if (ACTIONS.isEmpty()) return;

        List<ScheduledAction> due = new ArrayList<>();
        Iterator<ScheduledAction> iterator = ACTIONS.iterator();
        while (iterator.hasNext()) {
            ScheduledAction action = iterator.next();
            LivingEntity target = action.context().target();
            if (target == null || target.isRemoved() || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            if (target.level().getGameTime() >= action.executeAt()) {
                due.add(action);
                iterator.remove();
            }
        }

        for (ScheduledAction action : due) action.action().run();
    }

    private static void processPlayerToggleActions() {
        if (PLAYER_TOGGLE_ACTIONS.isEmpty()) return;

        Iterator<PlayerToggleAction> iterator = PLAYER_TOGGLE_ACTIONS.iterator();
        while (iterator.hasNext()) {
            PlayerToggleAction action = iterator.next();
            ServerPlayer player = action.player();
            if (player == null || player.isRemoved() || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            if (player.level().getGameTime() < action.executeAt()) continue;

            InstalledCyberware installed = action.data().get(action.slot(), action.index());
            if (installed != null) {
                action.data().setEnabled(action.slot(), action.index(), action.enabled());
                action.data().setDirty();
                PacketDistributor.sendToPlayer(player, new CyberwareEnabledStatePayload(action.slot().name(), action.index(), action.enabled()));
            }

            iterator.remove();
        }
    }

    private static void syncCorruptHumanityModifier(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(ModAttributes.HUMANITY);
        if (instance == null) return;

        instance.removeModifier(CORRUPT_HUMANITY_MODIFIER);
        int penalty = getRuntimeHumanityPenalty(player);
        if (penalty > 0) instance.addTransientModifier(new AttributeModifier(CORRUPT_HUMANITY_MODIFIER, -penalty, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void syncClientState(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return;

        boolean invertMovement = false;
        boolean invertVision = false;
        boolean motorLatency = false;
        boolean opticsMalfunction = false;
        boolean powerMalfunction = false;

        for (RuntimeEffect effect : EFFECTS) {
            if (!effect.targetId().equals(player.getUUID())) continue;

            if (effect.type() == RuntimeEffectType.DESYNC && effect.domain().affectsLegs()) {
                invertMovement = true;
            }

            if (effect.type() == RuntimeEffectType.MALFUNCTION && effect.domain().affectsLegs()) {
                MovementInvertState state = MOVEMENT_INVERSION_STATES.get(effect);

                if (state != null && state.inverted()) {
                    invertMovement = true;
                }
            }

            if (effect.type() == RuntimeEffectType.DESYNC && effect.domain() == QuickhackTargetDomain.CYBEROPTICS) invertVision = true;
            if (effect.type() == RuntimeEffectType.OVERCLOCK && effect.domain().affectsLegs()) motorLatency = true;
            if (effect.type() == RuntimeEffectType.MALFUNCTION && effect.domain() == QuickhackTargetDomain.CYBEROPTICS) opticsMalfunction = true;
            if (effect.type() == RuntimeEffectType.MALFUNCTION && effect.domain() == QuickhackTargetDomain.POWER_SYSTEM) powerMalfunction = true;
        }

        PacketDistributor.sendToPlayer(player, new QuickhackClientStatePayload(invertMovement, invertVision, motorLatency, opticsMalfunction, powerMalfunction));
    }

    private static void removeMatchingEffect(LivingEntity target, Set<RefKey> keys, RuntimeEffectType type) {
        Iterator<RuntimeEffect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            RuntimeEffect effect = iterator.next();
            if (!effect.targetId().equals(target.getUUID())) continue;
            if (effect.type() != type) continue;
            if (!keys.isEmpty() && !effect.refs().isEmpty() && !overlaps(keys, effect.refs())) continue;
            restoreRuntimeState(effect);
            iterator.remove();
        }

        if (target instanceof ServerPlayer player) syncCorruptHumanityModifier(player);
    }

    private static void restoreRuntimeState(RuntimeEffect effect) {
        if (effect == null) return;

        POWER_FLUCTUATIONS.remove(effect);
        MOVEMENT_INVERSION_STATES.remove(effect);

        if (effect.snapshots().isEmpty()) return;

        for (StateSnapshot snapshot : effect.snapshots()) {
            QuickhackCyberwareRef ref = resolveRef(effect.context(), snapshot.key());
            if (ref == null) continue;
            effect.context().setEnabled(ref, snapshot.enabled());
            effect.context().setPowered(ref, snapshot.powered());
        }

        effect.context().sync();
    }

    private static List<StateSnapshot> captureState(QuickhackExecutionContext context, List<QuickhackCyberwareRef> refs) {
        List<StateSnapshot> snapshots = new ArrayList<>();
        if (refs == null) return snapshots;

        for (QuickhackCyberwareRef ref : refs) {
            if (ref == null || ref.installed() == null) continue;
            snapshots.add(new StateSnapshot(new RefKey(ref.slot(), ref.index()), context.isEnabled(ref), ref.installed().isPowered()));
        }

        return List.copyOf(snapshots);
    }

    private static boolean needsStateRestore(RuntimeEffectType type) {
        return type == RuntimeEffectType.DESYNC || type == RuntimeEffectType.MALFUNCTION || type == RuntimeEffectType.LOOP;
    }

    private static boolean hasEffect(LivingEntity entity, CyberwareSlot slot, int index, RuntimeEffectType type) {
        if (entity == null || slot == null) return false;
        RefKey key = new RefKey(slot, index);

        for (RuntimeEffect effect : EFFECTS) {
            if (effect.type() != type) continue;
            if (!effect.targetId().equals(entity.getUUID())) continue;
            if (effect.refs().contains(key)) return true;
        }

        return false;
    }

    private static boolean containsSlot(Set<RefKey> keys, CyberwareSlot slot) {
        for (RefKey key : keys) {
            if (key.slot() == slot) return true;
        }

        return false;
    }

    private static QuickhackCyberwareRef resolveRef(QuickhackExecutionContext context, RefKey key) {
        if (context == null || key == null) return null;
        InstalledCyberware installed = context.targetData().get(key.slot(), key.index());
        if (installed == null || installed.getItem() == null || installed.getItem().isEmpty()) return null;
        return new QuickhackCyberwareRef(key.slot(), key.index(), installed);
    }

    private static Set<RefKey> refKeys(List<QuickhackCyberwareRef> refs) {
        Set<RefKey> keys = new HashSet<>();
        if (refs == null) return keys;

        for (QuickhackCyberwareRef ref : refs) {
            if (ref != null) keys.add(new RefKey(ref.slot(), ref.index()));
        }

        return keys;
    }

    private static boolean overlaps(Set<RefKey> first, Set<RefKey> second) {
        for (RefKey key : first) {
            if (second.contains(key)) return true;
        }

        return false;
    }

    private enum RuntimeEffectType {
        REBOOT,
        OVERCLOCK,
        DRAIN,
        CORRUPT,
        DESYNC,
        MALFUNCTION,
        LOOP
    }

    private record RefKey(CyberwareSlot slot, int index) {}
    private record PowerFluctuationState(boolean powered, long nextChangeAt) {}
    private record MovementInvertState(boolean inverted, long nextChangeAt) {}
    private record StateSnapshot(RefKey key, boolean enabled, boolean powered) {}
    private record RuntimeEffect(QuickhackExecutionContext context, UUID targetId, Set<RefKey> refs, QuickhackTargetDomain domain, RuntimeEffectType type, long expireAt, float magnitude, int humanityPenalty, List<StateSnapshot> snapshots) {}
    private record ScheduledAction(QuickhackExecutionContext context, Set<RefKey> keys, long executeAt, Runnable action) {}
    private record PlayerToggleAction(ServerPlayer player, PlayerCyberwareData data, UUID playerId, CyberwareSlot slot, int index, boolean enabled, long executeAt) {}
}