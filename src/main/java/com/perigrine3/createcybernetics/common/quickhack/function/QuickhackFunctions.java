package com.perigrine3.createcybernetics.common.quickhack.function;

import com.perigrine3.createcybernetics.api.CyberwareDegradationCause;
import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.common.durability.CyberwareDurabilityManager;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackCyberwareRef;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionContext;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackFunctionResult;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackFunctionSettings;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackRuntimeManager;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackTargetDomain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuickhackFunctions {
    private static final Map<String, QuickhackFunction> FUNCTIONS = new LinkedHashMap<>();

    private static final int BASE_RUNTIME_DURATION = 20 * 60;
    private static final int BASE_LOOP_DURATION = 20 * 60;
    private static final int SPIKE_DAMAGE = 40;
    private static final int OVERCLOCK_DAMAGE = 20;
    private static final int BURNOUT_MIN_FIRE_TICKS = 20 * 5;
    private static final int BURNOUT_MAX_FIRE_TICKS = 20 * 12;

    static {
        register("reboot", QuickhackFunctions::executeReboot);
        register("overclock", QuickhackFunctions::executeOverclock);
        register("drain", QuickhackFunctions::executeDrain);
        register("spike", QuickhackFunctions::executeSpike);
        register("burnout", QuickhackFunctions::executeBurnout);
        register("toggle_off", QuickhackFunctions::executeToggleOff);
        register("toggle_on", QuickhackFunctions::executeToggleOn);
        register("malfunction", QuickhackFunctions::executeMalfunction);
        register("loop", QuickhackFunctions::executeLoop);
        register("ping", QuickhackFunctions::executePing);
        register("trace", QuickhackFunctions::executeTrace);
        register("corrupt", QuickhackFunctions::executeCorrupt);
        register("purge", QuickhackFunctions::executePurge);
        register("desync", QuickhackFunctions::executeDesync);
    }

    private QuickhackFunctions() {}

    public static QuickhackFunction get(String id) {
        if (id == null) return null;
        if ("disable".equals(id)) id = "reboot";
        return FUNCTIONS.get(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    private static void register(String id, QuickhackFunction function) {
        FUNCTIONS.put(id, function);
    }

    private static QuickhackFunctionResult executeReboot(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("REBOOT");

        QuickhackRuntimeManager.applyReboot(context, targets, settings.targetDomain(), settings.durationTicks(BASE_RUNTIME_DURATION), settings.persistent());
        return affected("REBOOTED", targets.size());
    }

    private static QuickhackFunctionResult executeOverclock(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("OVERCLOCK");

        int damage = settings.scaledAmount(OVERCLOCK_DAMAGE);
        int damaged = damageCyberwareDistributed(context, targets, damage);
        int duration = settings.durationTicks(BASE_RUNTIME_DURATION);

        QuickhackRuntimeManager.applyOverclock(context, targets, settings.targetDomain(), duration, settings.persistent(), settings.intensityMultiplier());

        boolean ignited = context.target().getRandom().nextFloat() < 0.15F;
        if (ignited) context.target().igniteForSeconds(2.0F);

        List<Component> output = new ArrayList<>();
        output.add(Component.literal("OVERCLOCKED: " + targets.size()).withStyle(ChatFormatting.GOLD));
        output.add(Component.literal("INTEGRITY STRESS: " + damaged).withStyle(ChatFormatting.RED));
        if (ignited) output.add(Component.literal("THERMAL FAULT").withStyle(ChatFormatting.RED));

        return QuickhackFunctionResult.success(output);
    }

    private static QuickhackFunctionResult executeDrain(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("DRAIN");

        QuickhackRuntimeManager.applyDrain(context, targets, settings.targetDomain(), settings.durationTicks(BASE_RUNTIME_DURATION), settings.persistent());
        return affected(settings.targetDomain().isGlobalDrainTarget() ? "GLOBAL ENERGY DRAIN x2" : "ENERGY DRAIN x2", targets.size());
    }

    private static QuickhackFunctionResult executeSpike(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("SPIKE");

        int damage = settings.scaledAmount(SPIKE_DAMAGE);
        int damaged = damageCyberwareDistributed(context, targets, damage);
        return affected("INTEGRITY SPIKE", damaged);
    }

    private static QuickhackFunctionResult executeBurnout(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("BURNOUT");

        int baseFireTicks = BURNOUT_MIN_FIRE_TICKS + context.target().getRandom().nextInt(BURNOUT_MAX_FIRE_TICKS - BURNOUT_MIN_FIRE_TICKS + 1);
        int intensityScaledTicks = settings.scaledAmount(baseFireTicks);
        int fireTicks = settings.durationTicks(intensityScaledTicks);

        context.target().igniteForSeconds(fireTicks / 20.0F);

        return QuickhackFunctionResult.success(List.of(Component.literal("THERMAL OVERLOAD: " + String.format("%.1f", fireTicks / 20.0F) + "s").withStyle(ChatFormatting.RED)));
    }

    private static QuickhackFunctionResult executeToggleOff(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        return executeToggle(context, targets, false);
    }

    private static QuickhackFunctionResult executeToggleOn(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        return executeToggle(context, targets, true);
    }

    private static QuickhackFunctionResult executeToggle(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, boolean enabled) {
        if (targets.isEmpty()) return noTargets(enabled ? "TOGGLE ON" : "TOGGLE OFF");

        int toggled = 0;

        for (QuickhackCyberwareRef ref : targets) {
            ItemStack stack = ref.installed().getItem();
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ICyberwareItem item)) continue;
            if (!item.isToggleableByWheel(stack, ref.slot())) continue;

            QuickhackRuntimeManager.requestToggle(context, ref, enabled);
            toggled++;
        }

        return affected(enabled ? "TOGGLED ON" : "TOGGLED OFF", toggled);
    }

    private static QuickhackFunctionResult executeMalfunction(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("MALFUNCTION");

        QuickhackRuntimeManager.applyMalfunction(context, targets, settings.targetDomain(), settings.durationTicks(BASE_RUNTIME_DURATION), settings.persistent());
        return affected("MALFUNCTION", targets.size());
    }

    private static QuickhackFunctionResult executeLoop(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("LOOP");

        QuickhackRuntimeManager.applyLoop(context, targets, settings.targetDomain(), settings.durationTicks(BASE_LOOP_DURATION), settings.persistent());
        return affected("LOOP STARTED", targets.size());
    }

    private static QuickhackFunctionResult executePing(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        List<Component> output = new ArrayList<>();
        output.add(Component.literal("TARGET SCAN // ").withStyle(ChatFormatting.AQUA).append(context.target().getDisplayName()));

        if (targets.isEmpty()) {
            output.add(Component.literal("NO CYBERWARE DETECTED").withStyle(ChatFormatting.DARK_GRAY));
            return QuickhackFunctionResult.success(output);
        }

        CyberwareSlot currentSlot = null;

        for (QuickhackCyberwareRef ref : targets) {
            if (ref.slot() != currentSlot) {
                currentSlot = ref.slot();
                output.add(Component.literal(""));
                output.add(Component.literal("[" + getSlotName(currentSlot) + "]").withStyle(ChatFormatting.DARK_AQUA));
            }

            output.add(Component.literal("  ").append(ref.installed().getItem().getHoverName()).withStyle(ChatFormatting.GRAY));
        }

        return QuickhackFunctionResult.success(output);
    }

    private static QuickhackFunctionResult executeTrace(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        List<Component> output = new ArrayList<>();
        output.add(Component.literal("SYSTEM TRACE // ").withStyle(ChatFormatting.LIGHT_PURPLE).append(context.target().getDisplayName()));

        if (targets.isEmpty()) {
            output.add(Component.literal("NO CYBERWARE DETECTED").withStyle(ChatFormatting.DARK_GRAY));
            return QuickhackFunctionResult.success(output);
        }

        for (QuickhackCyberwareRef ref : targets) {
            ItemStack stack = ref.installed().getItem();
            int integrity = Mth.clamp(Math.round(CyberwareDurabilityManager.getEffectiveDurabilityPercent(ref.installed()) * 100.0F), 0, 100);
            boolean enabled = context.isEnabled(ref);
            boolean powered = ref.installed().isPowered();
            boolean rebooted = QuickhackRuntimeManager.isRebooted(context.target(), ref.installed());

            String state;

            if (rebooted) {
                state = "Rebooting";
            } else if (!enabled) {
                state = "Disabled";
            } else if (!powered) {
                state = "Unpowered";
            } else if (CyberwareDurabilityManager.isBroken(ref.installed())) {
                state = "Damaged";
            } else {
                state = "Online";
            }

            output.add(Component.literal(""));
            output.add(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
            output.add(Component.literal("  Slot: " + getSlotName(ref.slot())).withStyle(ChatFormatting.GRAY));
            output.add(Component.literal("  Integrity: " + integrity + "%").withStyle(getIntegrityColor(integrity)));
            output.add(Component.literal("  State: " + state).withStyle(getStateColor(state)));
        }

        return QuickhackFunctionResult.success(output);
    }

    private static QuickhackFunctionResult executeCorrupt(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("CORRUPT");

        int penalty = 0;

        for (QuickhackCyberwareRef ref : targets) {
            ItemStack stack = ref.installed().getItem();
            if (stack == null || stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ICyberwareItem item)) continue;

            int baseline = Math.max(0, item.getHumanityCost());
            if (baseline <= 0) continue;

            penalty += Math.max(1, Mth.ceil(baseline * 0.25F * settings.intensityMultiplier()));
        }

        QuickhackRuntimeManager.applyCorrupt(context, targets, settings.targetDomain(), settings.durationTicks(BASE_RUNTIME_DURATION), settings.persistent(), penalty);

        return QuickhackFunctionResult.success(List.of(Component.literal("HUMANITY PENALTY +" + penalty).withStyle(ChatFormatting.DARK_PURPLE)));
    }

    private static QuickhackFunctionResult executePurge(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        int purged = QuickhackRuntimeManager.purge(context, targets);
        return QuickhackFunctionResult.success(List.of(Component.literal("PURGED EFFECTS: " + purged).withStyle(ChatFormatting.GREEN)));
    }

    private static QuickhackFunctionResult executeDesync(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings) {
        if (targets.isEmpty()) return noTargets("DESYNC");

        QuickhackRuntimeManager.applyDesync(context, targets, settings.targetDomain(), settings.durationTicks(BASE_RUNTIME_DURATION), settings.persistent());

        String effect = switch (settings.targetDomain()) {
            case ARMS, MOTOR_CONTROL -> "50% INTERACTION FAILURE";
            case LEGS -> "MOVEMENT INPUT INVERTED";
            case CYBEROPTICS -> "VISUAL OUTPUT INVERTED";
            default -> "POWER STATE DESYNCHRONIZED";
        };

        return QuickhackFunctionResult.success(List.of(Component.literal("DESYNC: " + effect).withStyle(ChatFormatting.DARK_PURPLE)));
    }

    private static int damageCyberwareDistributed(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, int totalAmount) {
        if (context == null || targets == null || targets.isEmpty() || totalAmount <= 0) return 0;

        List<QuickhackCyberwareRef> remainingTargets = new ArrayList<>();

        for (QuickhackCyberwareRef ref : targets) {
            if (ref == null || ref.installed() == null) continue;
            remainingTargets.add(ref);
        }

        if (remainingTargets.isEmpty()) return 0;

        int remainingDamage = totalAmount;
        int affected = 0;

        while (remainingDamage > 0 && !remainingTargets.isEmpty()) {
            int selectedIndex = context.target().getRandom().nextInt(remainingTargets.size());
            QuickhackCyberwareRef ref = remainingTargets.remove(selectedIndex);

            int damage = Mth.ceil((float) remainingDamage / (remainingTargets.size() + 1));
            int damaged = CyberwareDurabilityManager.damageDirect(context.target(), context.targetData(), ref.installed(), CyberwareDegradationCause.GENERAL_DAMAGE, damage);

            remainingDamage -= damage;

            if (damaged > 0) affected++;
        }

        if (affected > 0) context.sync();
        return affected;
    }

    private static QuickhackFunctionResult noTargets(String function) {
        return QuickhackFunctionResult.success(List.of(Component.literal(function + ": NO MATCHING CYBERWARE").withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static QuickhackFunctionResult affected(String operation, int count) {
        return QuickhackFunctionResult.success(List.of(Component.literal(operation + ": " + count).withStyle(ChatFormatting.AQUA)));
    }

    private static String getSlotName(CyberwareSlot slot) {
        if (slot == null) return "Unknown";

        return switch (slot) {
            case BRAIN -> "Brain";
            case EYES -> "Eyes";
            case HEART -> "Heart";
            case LUNGS -> "Lungs";
            case ORGANS -> "Internal Organs";
            case RARM -> "Right Arm";
            case LARM -> "Left Arm";
            case RLEG -> "Right Leg";
            case LLEG -> "Left Leg";
            case MUSCLE -> "Muscle";
            case BONE -> "Skeleton";
            case SKIN -> "Dermal";
        };
    }

    private static ChatFormatting getIntegrityColor(int integrity) {
        if (integrity <= 25) return ChatFormatting.RED;
        if (integrity <= 50) return ChatFormatting.GOLD;
        if (integrity <= 75) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }

    private static ChatFormatting getStateColor(String state) {
        return switch (state) {
            case "Online" -> ChatFormatting.GREEN;
            case "Rebooting" -> ChatFormatting.AQUA;
            case "Disabled" -> ChatFormatting.RED;
            case "Unpowered" -> ChatFormatting.DARK_RED;
            case "Damaged" -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };
    }
}
