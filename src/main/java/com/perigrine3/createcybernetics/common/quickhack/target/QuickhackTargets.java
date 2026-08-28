package com.perigrine3.createcybernetics.common.quickhack.target;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackCyberwareRef;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionContext;
import com.perigrine3.createcybernetics.item.ModItems;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QuickhackTargets {
    private static final Map<String, QuickhackTarget> TARGETS = new LinkedHashMap<>();

    static {
        register("system_all", QuickhackTargets::resolveSystemAll);
        register("processor", context -> resolveSlots(context, CyberwareSlot.BRAIN));
        register("cyberoptics", context -> resolveSlots(context, CyberwareSlot.EYES));
        register("auditory", context -> resolveSlots(context, CyberwareSlot.BRAIN));
        register("motor_control", context -> resolveSlots(context, CyberwareSlot.MUSCLE, CyberwareSlot.BONE, CyberwareSlot.LARM, CyberwareSlot.RARM, CyberwareSlot.LLEG, CyberwareSlot.RLEG));
        register("arms", context -> resolveSlots(context, CyberwareSlot.LARM, CyberwareSlot.RARM));
        register("legs", context -> resolveSlots(context, CyberwareSlot.LLEG, CyberwareSlot.RLEG));
        register("internal_organs", context -> resolveSlots(context, CyberwareSlot.HEART, CyberwareSlot.LUNGS, CyberwareSlot.ORGANS));
        register("skeleton", context -> resolveSlots(context, CyberwareSlot.BONE));
        register("dermal", context -> resolveSlots(context, CyberwareSlot.SKIN));
        register("power_system", QuickhackTargets::resolvePowerSystem);
        register("cyberdeck", QuickhackTargets::resolveCyberdeck);
        register("neural_system", context -> resolveSlots(context, CyberwareSlot.BRAIN, CyberwareSlot.MUSCLE));
        register("active_cyberware", QuickhackTargets::resolveActiveCyberware);
        register("highest_draw", QuickhackTargets::resolveHighestDraw);
        register("random_implant", QuickhackTargets::resolveRandomImplant);
    }

    private QuickhackTargets() {}

    public static QuickhackTarget get(String id) {
        if (id == null) return null;
        return TARGETS.get(id);
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static List<QuickhackCyberwareRef> resolveAll(QuickhackExecutionContext context) {
        return resolveSystemAll(context);
    }

    private static void register(String id, QuickhackTarget target) {
        TARGETS.put(id, target);
    }

    private static List<QuickhackCyberwareRef> resolveSystemAll(QuickhackExecutionContext context) {
        List<QuickhackCyberwareRef> resolved = new ArrayList<>();

        for (var entry : context.targetData().getAll().entrySet()) {
            InstalledCyberware[] installed = entry.getValue();
            if (installed == null) continue;

            for (int index = 0; index < installed.length; index++) {
                InstalledCyberware cyberware = installed[index];
                if (cyberware == null || cyberware.getItem() == null || cyberware.getItem().isEmpty()) continue;

                resolved.add(new QuickhackCyberwareRef(entry.getKey(), index, cyberware));
            }
        }

        return List.copyOf(resolved);
    }

    private static List<QuickhackCyberwareRef> resolveSlots(QuickhackExecutionContext context, CyberwareSlot... slots) {
        List<QuickhackCyberwareRef> resolved = new ArrayList<>();

        for (CyberwareSlot slot : slots) {
            InstalledCyberware[] installed = context.targetData().getAll().get(slot);
            if (installed == null) continue;

            for (int index = 0; index < installed.length; index++) {
                InstalledCyberware cyberware = installed[index];
                if (cyberware == null || cyberware.getItem() == null || cyberware.getItem().isEmpty()) continue;

                resolved.add(new QuickhackCyberwareRef(slot, index, cyberware));
            }
        }

        return List.copyOf(resolved);
    }

    private static List<QuickhackCyberwareRef> resolvePowerSystem(QuickhackExecutionContext context) {
        List<QuickhackCyberwareRef> resolved = new ArrayList<>();

        for (QuickhackCyberwareRef ref : resolveSystemAll(context)) {
            ItemStack stack = ref.installed().getItem();

            if (stack.is(ModTags.Items.ENERGY_GENERATING_CYBERWARE)) {
                resolved.add(ref);
                continue;
            }

            if (stack.getItem() instanceof ICyberwareItem item && item.getEnergyCapacity(context.target(), stack, ref.slot()) > 0) {
                resolved.add(ref);
            }
        }

        return List.copyOf(resolved);
    }

    private static List<QuickhackCyberwareRef> resolveCyberdeck(QuickhackExecutionContext context) {
        List<QuickhackCyberwareRef> resolved = new ArrayList<>();

        for (QuickhackCyberwareRef ref : resolveSlots(context, CyberwareSlot.BRAIN)) {
            if (ref.installed().getItem().is(ModItems.BRAINUPGRADES_CYBERDECK.get())) {
                resolved.add(ref);
            }
        }

        return List.copyOf(resolved);
    }

    private static List<QuickhackCyberwareRef> resolveActiveCyberware(QuickhackExecutionContext context) {
        List<QuickhackCyberwareRef> resolved = new ArrayList<>();

        for (QuickhackCyberwareRef ref : resolveSystemAll(context)) {
            if (!context.isEnabled(ref)) continue;

            ItemStack stack = ref.installed().getItem();

            if (stack.getItem() instanceof ICyberwareItem item && item.requiresEnergyToFunction(context.target(), stack, ref.slot()) && !ref.installed().isPowered()) {
                continue;
            }

            resolved.add(ref);
        }

        return List.copyOf(resolved);
    }

    private static List<QuickhackCyberwareRef> resolveHighestDraw(QuickhackExecutionContext context) {
        QuickhackCyberwareRef best = null;
        int bestDraw = Integer.MIN_VALUE;

        for (QuickhackCyberwareRef ref : resolveSystemAll(context)) {
            ItemStack stack = ref.installed().getItem();
            if (!(stack.getItem() instanceof ICyberwareItem item)) continue;

            int draw = Math.max(0, item.getEnergyUsedPerTick(context.target(), stack, ref.slot())) + Math.max(0, item.getEnergyActivationCost(context.target(), stack, ref.slot()));

            if (draw > bestDraw) {
                bestDraw = draw;
                best = ref;
            }
        }

        return best == null ? List.of() : List.of(best);
    }

    private static List<QuickhackCyberwareRef> resolveRandomImplant(QuickhackExecutionContext context) {
        List<QuickhackCyberwareRef> installed = resolveSystemAll(context);
        if (installed.isEmpty()) return List.of();

        return List.of(installed.get(context.target().getRandom().nextInt(installed.size())));
    }
}
