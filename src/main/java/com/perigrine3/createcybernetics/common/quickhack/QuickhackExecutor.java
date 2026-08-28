package com.perigrine3.createcybernetics.common.quickhack;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.common.quickhack.function.QuickhackFunction;
import com.perigrine3.createcybernetics.common.quickhack.function.QuickhackFunctions;
import com.perigrine3.createcybernetics.common.quickhack.modifier.QuickhackModifier;
import com.perigrine3.createcybernetics.common.quickhack.modifier.QuickhackModifiers;
import com.perigrine3.createcybernetics.common.quickhack.target.QuickhackTarget;
import com.perigrine3.createcybernetics.common.quickhack.target.QuickhackTargets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuickhackExecutor {
    private static final double CONTAGION_RANGE = 2.5D;
    private static final float CONTAGION_SUCCESS_CHANCE = 0.50F;

    private QuickhackExecutor() {}

    public static QuickhackExecutionResult execute(QuickhackExecutionContext context) {
        if (context == null) return QuickhackExecutionResult.failure(Component.translatable("message.createcybernetics.quickhack.execution.invalid_context"));

        List<MicroprogramDefinition> nodes = QuickhackProgramData.getNodes(context.quickhack());

        if (!QuickhackProgramData.isValidProgram(nodes)) {
            return QuickhackExecutionResult.failure(Component.translatable("message.createcybernetics.quickhack.execution.invalid_program"));
        }

        List<QuickhackInstruction> instructions = compile(nodes);

        if (instructions.isEmpty()) {
            return QuickhackExecutionResult.failure(Component.translatable("message.createcybernetics.quickhack.execution.invalid_program"));
        }

        List<Component> casterOutput = new ArrayList<>();
        boolean notifyTarget = false;
        boolean executedAny = false;

        for (QuickhackInstruction instruction : instructions) {
            QuickhackTarget target = QuickhackTargets.get(instruction.target().id());
            if (target == null) return unsupported(instruction.target());

            QuickhackFunction function = QuickhackFunctions.get(instruction.function().id());
            if (function == null) return unsupported(instruction.function());

            QuickhackFunctionSettings settings = new QuickhackFunctionSettings();
            settings.setTargetDomain(QuickhackTargetDomain.fromId(instruction.target().id()));

            boolean durationModifierApplied = false;

            for (MicroprogramDefinition modifierDefinition : instruction.modifiers()) {
                if (isDurationModifier(modifierDefinition.id())) {
                    if (durationModifierApplied) continue;
                    durationModifierApplied = true;
                }

                QuickhackModifier modifier = QuickhackModifiers.get(modifierDefinition.id());
                if (modifier == null) return unsupported(modifierDefinition);
                modifier.apply(context, settings);
            }

            List<QuickhackCyberwareRef> resolvedTargets = applySelectionModifiers(context, target.resolve(context), settings);

            if (settings.delayTicks() > 0) {
                scheduleFunction(context, target, function, resolvedTargets, settings, settings.delayTicks(), casterOutput, instruction.function());
            } else {
                QuickhackFunctionResult functionResult = executeFunction(context, target, function, resolvedTargets, settings);

                if (!functionResult.success()) {
                    return QuickhackExecutionResult.failure(Component.translatable("message.createcybernetics.quickhack.execution.function_failed", instruction.function().getDisplayName()));
                }

                casterOutput.addAll(functionResult.casterOutput());
            }

            for (int repeat = 1; repeat < settings.repeatCount(); repeat++) {
                int repeatDelay = settings.delayTicks() + repeat * 20;
                scheduleFunction(context, target, function, resolvedTargets, settings, repeatDelay, casterOutput, instruction.function());
            }

            executedAny = true;

            if (!settings.silent()) notifyTarget = true;
        }

        if (!executedAny) {
            return QuickhackExecutionResult.failure(Component.translatable("message.createcybernetics.quickhack.execution.no_functions"));
        }

        return QuickhackExecutionResult.success(notifyTarget, casterOutput);
    }

    private static boolean isDurationModifier(String id) {
        return "duration_plus".equals(id)
                || "duration_plus_plus".equals(id)
                || "persistent".equals(id);
    }

    private static QuickhackFunctionResult executeFunction(QuickhackExecutionContext context, QuickhackTarget target, QuickhackFunction function, List<QuickhackCyberwareRef> resolvedTargets, QuickhackFunctionSettings settings) {
        QuickhackFunctionResult result = function.execute(context, resolvedTargets, settings);

        if (!result.success() || !settings.contagion()) return result;

        int infected = spreadContagion(context, target, function, settings);

        if (infected <= 0) return result;

        List<Component> output = new ArrayList<>(result.casterOutput());
        output.add(Component.literal("CONTAGION: " + infected + " secondary target" + (infected == 1 ? "" : "s")).withStyle(ChatFormatting.DARK_PURPLE));
        return QuickhackFunctionResult.success(output);
    }

    private static int spreadContagion(QuickhackExecutionContext originalContext, QuickhackTarget target, QuickhackFunction function, QuickhackFunctionSettings settings) {
        LivingEntity primaryTarget = originalContext.physicalTarget();
        AABB area = primaryTarget.getBoundingBox().inflate(CONTAGION_RANGE, CONTAGION_RANGE, CONTAGION_RANGE);

        List<LivingEntity> nearby = primaryTarget.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive() && entity != primaryTarget && entity != originalContext.caster());
        int infected = 0;

        for (LivingEntity entity : nearby) {
            QuickhackExecutionContext secondaryContext = QuickhackExecutionContext.create(originalContext.caster(), entity, originalContext.quickhack());
            if (secondaryContext == null) continue;

            List<QuickhackCyberwareRef> secondaryTargets = applySelectionModifiers(secondaryContext, target.resolve(secondaryContext), settings);
            if (secondaryTargets.isEmpty()) continue;
            if (primaryTarget.getRandom().nextFloat() >= CONTAGION_SUCCESS_CHANCE) continue;

            QuickhackFunctionResult secondaryResult = function.execute(secondaryContext, secondaryTargets, settings);
            if (!secondaryResult.success()) continue;

            infected++;

            if (!settings.silent()) {
                secondaryContext.target().sendSystemMessage(Component.translatable("message.createcybernetics.quickhack.target.success", originalContext.programName()));
            }
        }

        return infected;
    }

    private static void scheduleFunction(QuickhackExecutionContext context, QuickhackTarget target, QuickhackFunction function, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings, int delayTicks, List<Component> immediateOutput, MicroprogramDefinition definition) {
        immediateOutput.add(Component.literal("QUEUED: " + definition.displayName() + " +" + String.format("%.1f", delayTicks / 20.0F) + "s").withStyle(ChatFormatting.DARK_AQUA));

        QuickhackRuntimeManager.schedule(context, targets, delayTicks, () -> {
            if (!context.target().isAlive()) return;

            QuickhackFunctionResult result = executeFunction(context, target, function, targets, settings);

            for (Component output : result.casterOutput()) {
                context.caster().displayClientMessage(output, false);
            }
        });
    }

    private static List<QuickhackCyberwareRef> applySelectionModifiers(QuickhackExecutionContext context, List<QuickhackCyberwareRef> original, QuickhackFunctionSettings settings) {
        List<QuickhackCyberwareRef> result = original == null ? new ArrayList<>() : new ArrayList<>(original);

        if (settings.cascade() && !result.isEmpty()) {
            Set<CyberwareSlot> slots = new HashSet<>();

            for (QuickhackCyberwareRef ref : result) {
                slots.add(ref.slot());
            }

            result.clear();

            for (QuickhackCyberwareRef ref : QuickhackTargets.resolveAll(context)) {
                if (slots.contains(ref.slot())) result.add(ref);
            }
        }

        if (settings.randomize() && result.size() > 1) {
            QuickhackCyberwareRef selected = result.get(context.target().getRandom().nextInt(result.size()));
            result = new ArrayList<>(List.of(selected));
        }

        return List.copyOf(result);
    }

    private static List<QuickhackInstruction> compile(List<MicroprogramDefinition> nodes) {
        List<QuickhackInstruction> instructions = new ArrayList<>();
        MicroprogramDefinition currentTarget = null;

        for (int index = 0; index < nodes.size(); index++) {
            MicroprogramDefinition node = nodes.get(index);

            if (node.category() == MicroprogramCategory.TARGET) {
                currentTarget = node;
                continue;
            }

            if (node.category() != MicroprogramCategory.FUNCTION || currentTarget == null) continue;

            List<MicroprogramDefinition> modifiers = new ArrayList<>();
            int modifierIndex = index + 1;

            while (modifierIndex < nodes.size()) {
                MicroprogramDefinition next = nodes.get(modifierIndex);
                if (next.category() != MicroprogramCategory.MODIFIER) break;

                modifiers.add(next);
                modifierIndex++;
            }

            instructions.add(new QuickhackInstruction(currentTarget, node, modifiers));
            index = modifierIndex - 1;
        }

        return List.copyOf(instructions);
    }

    private static QuickhackExecutionResult unsupported(MicroprogramDefinition definition) {
        return QuickhackExecutionResult.failure(Component.translatable("message.createcybernetics.quickhack.execution.unsupported_microprogram", definition.getDisplayName()));
    }
}