package com.perigrine3.createcybernetics.client.surgery;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.ICyberwareItem;
import com.perigrine3.createcybernetics.api.InstalledCyberware;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.common.surgery.DefaultOrgans;
import com.perigrine3.createcybernetics.common.surgery.RobosurgeonSlotMap;
import com.perigrine3.createcybernetics.item.cyberware.brain.CerebralProcessingUnitItem;
import com.perigrine3.createcybernetics.item.cyberware.organs.OregrinderItem;
import com.perigrine3.createcybernetics.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

public final class SurgeryPreviewDiagnostics {

    private SurgeryPreviewDiagnostics() {}

    public record Entry(CyberwareSlot slot, int slotIndex, int inventoryIndex, ItemStack stack, boolean staged) {}

    public record EnergyPreview(int generatedPerTick, int requiredPerTick) {
        public boolean sufficient() {
            return generatedPerTick >= requiredPerTick;
        }
    }

    public record Preview(List<Entry> finalLoadout, List<Component> warnings, EnergyPreview energy) {
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    public static Preview analyze(Player patient, PlayerCyberwareData data, IntFunction<ItemStack> guiStackGetter, IntPredicate stagedGetter, IntPredicate markedForRemovalGetter) {
        if (patient == null || data == null) {
            return emptyPreview();
        }

        List<Entry> finalLoadout = buildFinalLoadout(data, guiStackGetter, stagedGetter, markedForRemovalGetter);
        List<Component> warnings = new ArrayList<>();

        addRemovedOrganReplacementWarnings(data, finalLoadout, markedForRemovalGetter, warnings);
        addDefaultOrganWarnings(data, finalLoadout, guiStackGetter, markedForRemovalGetter, warnings);
        addInstallabilityWarnings(finalLoadout, warnings);
        addMissingRequirementWarnings(finalLoadout, warnings);
        addIncompatibilityWarnings(finalLoadout, warnings);
        addMaximumStackWarnings(finalLoadout, warnings);
        addOregrinderWarnings(finalLoadout, warnings);

        EnergyPreview energy = calculateEnergyPreview(patient, finalLoadout);

        if (!energy.sufficient() && energy.requiredPerTick() > 0) {
            warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.energy_shortfall", energy.generatedPerTick(), energy.requiredPerTick()).withStyle(ChatFormatting.RED));
        }

        return new Preview(List.copyOf(finalLoadout), deduplicateWarnings(warnings), energy);
    }

    public static Preview emptyPreview() {
        return new Preview(List.of(), List.of(), new EnergyPreview(0, 0));
    }

    public static Preview addWarning(Preview preview, Component warning) {
        if (preview == null) preview = emptyPreview();
        if (warning == null) return preview;

        List<Component> warnings = new ArrayList<>(preview.warnings());
        warnings.add(warning);
        return new Preview(preview.finalLoadout(), deduplicateWarnings(warnings), preview.energy());
    }

    private static List<Entry> buildFinalLoadout(PlayerCyberwareData data, IntFunction<ItemStack> guiStackGetter, IntPredicate stagedGetter, IntPredicate markedForRemovalGetter) {
        List<Entry> result = new ArrayList<>();

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            for (int slotIndex = 0; slotIndex < slot.size; slotIndex++) {
                int inventoryIndex = RobosurgeonSlotMap.toInventoryIndex(slot, slotIndex);
                if (inventoryIndex < 0) continue;

                ItemStack guiStack = safeCopy(guiStackGetter.apply(inventoryIndex));
                InstalledCyberware installed = data.get(slot, slotIndex);
                ItemStack installedStack = installed != null && installed.getItem() != null ? safeCopy(installed.getItem()) : ItemStack.EMPTY;
                boolean staged = stagedGetter.test(inventoryIndex);
                boolean markedForRemoval = markedForRemovalGetter.test(inventoryIndex);
                ItemStack finalStack = staged && !guiStack.isEmpty() ? guiStack : markedForRemoval ? ItemStack.EMPTY : installedStack;

                if (!finalStack.isEmpty()) {
                    result.add(new Entry(slot, slotIndex, inventoryIndex, finalStack, staged));
                }
            }
        }

        return result;
    }

    private static void addRemovedOrganReplacementWarnings(PlayerCyberwareData data, List<Entry> finalLoadout, IntPredicate markedForRemovalGetter, List<Component> warnings) {
        Set<String> reported = new HashSet<>();

        for (CyberwareSlot slot : CyberwareSlot.values()) {
            for (int slotIndex = 0; slotIndex < slot.size; slotIndex++) {
                int inventoryIndex = RobosurgeonSlotMap.toInventoryIndex(slot, slotIndex);
                if (inventoryIndex < 0 || !markedForRemovalGetter.test(inventoryIndex)) continue;

                InstalledCyberware installed = data.get(slot, slotIndex);
                ItemStack removedStack = installed != null && installed.getItem() != null ? installed.getItem() : ItemStack.EMPTY;
                if (removedStack.isEmpty()) continue;

                ItemStack defaultStackAtPosition = DefaultOrgans.get(slot, slotIndex);
                if (defaultStackAtPosition != null && !defaultStackAtPosition.isEmpty() && removedStack.getItem() == defaultStackAtPosition.getItem()) continue;

                if (!(removedStack.getItem() instanceof ICyberwareItem cyberware)) continue;
                if (!cyberware.replacesOrgan()) continue;

                Set<CyberwareSlot> replacedSlots = cyberware.getReplacedOrgans();
                if (replacedSlots == null || replacedSlots.isEmpty()) replacedSlots = Set.of(slot);

                TagKey<Item> replacementTag = cyberware.getReplacedOrganItemTag(removedStack, slot);

                for (CyberwareSlot replacedSlot : replacedSlots) {
                    for (int replacedIndex = 0; replacedIndex < replacedSlot.size; replacedIndex++) {
                        ItemStack defaultStack = DefaultOrgans.get(replacedSlot, replacedIndex);
                        if (defaultStack == null || defaultStack.isEmpty()) continue;
                        if (replacementTag != null && !defaultStack.is(replacementTag)) continue;
                        if (replacementTag == null && replacedSlot == slot && replacedIndex != slotIndex) continue;
                        if (hasSuitableReplacement(defaultStack, replacedSlot, finalLoadout)) continue;

                        String key = removedStack.getItem() + "|" + defaultStack.getItem();
                        if (!reported.add(key)) continue;

                        warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.organ_replacement_removed", removedStack.getHoverName(), defaultStack.getHoverName()).withStyle(ChatFormatting.RED));
                    }
                }
            }
        }
    }

    private static void addDefaultOrganWarnings(PlayerCyberwareData data, List<Entry> finalLoadout, IntFunction<ItemStack> guiStackGetter, IntPredicate markedForRemovalGetter, List<Component> warnings) {
        for (CyberwareSlot slot : CyberwareSlot.values()) {
            for (int slotIndex = 0; slotIndex < slot.size; slotIndex++) {
                int inventoryIndex = RobosurgeonSlotMap.toInventoryIndex(slot, slotIndex);
                if (inventoryIndex < 0 || !markedForRemovalGetter.test(inventoryIndex)) continue;

                InstalledCyberware installed = data.get(slot, slotIndex);
                ItemStack removedStack = installed != null && installed.getItem() != null ? installed.getItem() : ItemStack.EMPTY;
                if (removedStack.isEmpty()) removedStack = safeCopy(guiStackGetter.apply(inventoryIndex));

                ItemStack defaultStack = DefaultOrgans.get(slot, slotIndex);

                if (removedStack.isEmpty() || defaultStack == null || defaultStack.isEmpty()) continue;
                if (removedStack.getItem() != defaultStack.getItem()) continue;
                if (hasSuitableReplacement(defaultStack, slot, finalLoadout)) continue;

                warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.organ_without_replacement", removedStack.getHoverName()).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static boolean hasSuitableReplacement(ItemStack removedOrgan, CyberwareSlot removedSlot, List<Entry> finalLoadout) {
        for (Entry entry : finalLoadout) {
            if (entry.slot() != removedSlot) continue;

            ItemStack candidate = entry.stack();
            if (candidate.getItem() == removedOrgan.getItem()) return true;
            if (!(candidate.getItem() instanceof ICyberwareItem cyberware) || !cyberware.replacesOrgan()) continue;

            TagKey<Item> replacementTag = cyberware.getReplacedOrganItemTag(candidate, entry.slot());
            if (replacementTag != null && removedOrgan.is(replacementTag)) return true;

            Set<CyberwareSlot> replacedOrgans = cyberware.getReplacedOrgans();
            if (replacementTag == null && replacedOrgans != null && replacedOrgans.contains(removedSlot) && removedSlot != CyberwareSlot.ORGANS) return true;
        }

        return false;
    }

    private static void addInstallabilityWarnings(List<Entry> finalLoadout, List<Component> warnings) {
        for (Entry entry : finalLoadout) {
            if (!entry.staged()) continue;

            ItemStack stack = entry.stack();
            if (!(stack.getItem() instanceof ICyberwareItem cyberware)) continue;

            if (!cyberware.surgeryInstallable()) {
                warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.not_installable", stack.getHoverName()).withStyle(ChatFormatting.RED));
            }

            if (stack.getItem() instanceof CerebralProcessingUnitItem && !CerebralProcessingUnitItem.hasCompletedBiochipBinding(stack)) {
                warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.cpu_unbound", stack.getHoverName()).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static void addMissingRequirementWarnings(List<Entry> finalLoadout, List<Component> warnings) {
        for (Entry entry : finalLoadout) {
            ItemStack stack = entry.stack();
            if (!(stack.getItem() instanceof ICyberwareItem cyberware)) continue;

            Set<Item> requiredItems = safeItems(cyberware.requiresCyberware(stack, entry.slot()));
            Set<TagKey<Item>> requiredTags = safeTags(cyberware.requiresCyberwareTags(stack, entry.slot()));

            if (!requiredItems.isEmpty() && !containsAnyRequiredItem(finalLoadout, entry.slot(), requiredItems)) {
                Component requiredNames = joinItemNames(requiredItems);
                warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.missing_required_item", stack.getHoverName(), requiredNames).withStyle(ChatFormatting.RED));
            }

            if (!requiredTags.isEmpty() && !containsAnyRequiredTag(finalLoadout, entry.slot(), requiredTags)) {
                warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.missing_required_cyberware", stack.getHoverName()).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static boolean containsAnyRequiredItem(List<Entry> finalLoadout, CyberwareSlot slot, Set<Item> requiredItems) {
        for (Entry entry : finalLoadout) {
            if (entry.slot() != slot) continue;
            if (requiredItems.contains(entry.stack().getItem())) return true;
        }

        return false;
    }

    private static boolean containsAnyRequiredTag(List<Entry> finalLoadout, CyberwareSlot slot, Set<TagKey<Item>> requiredTags) {
        for (Entry entry : finalLoadout) {
            if (entry.slot() != slot) continue;

            for (TagKey<Item> requiredTag : requiredTags) {
                if (matchesTag(entry, requiredTag)) return true;
            }
        }

        return false;
    }

    private static void addIncompatibilityWarnings(List<Entry> finalLoadout, List<Component> warnings) {
        Set<String> reportedPairs = new HashSet<>();

        for (int firstIndex = 0; firstIndex < finalLoadout.size(); firstIndex++) {
            Entry first = finalLoadout.get(firstIndex);

            for (int secondIndex = firstIndex + 1; secondIndex < finalLoadout.size(); secondIndex++) {
                Entry second = finalLoadout.get(secondIndex);
                if (!areIncompatible(first, second)) continue;

                String firstName = first.stack().getHoverName().getString();
                String secondName = second.stack().getHoverName().getString();
                String pairKey = firstName.compareTo(secondName) <= 0 ? firstName + "|" + secondName : secondName + "|" + firstName;
                if (!reportedPairs.add(pairKey)) continue;

                warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.incompatible", first.stack().getHoverName(), second.stack().getHoverName()).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static boolean areIncompatible(Entry first, Entry second) {
        if (!(first.stack().getItem() instanceof ICyberwareItem firstCyberware)) return false;
        if (!(second.stack().getItem() instanceof ICyberwareItem secondCyberware)) return false;

        if (matchesAnyIncompatible(firstCyberware, first, second)) return true;
        if (matchesAnyIncompatible(secondCyberware, second, first)) return true;
        if (first.slot() != second.slot()) return false;

        return matchesAnySameSlotIncompatible(firstCyberware, first, second) || matchesAnySameSlotIncompatible(secondCyberware, second, first);
    }

    private static boolean matchesAnyIncompatible(ICyberwareItem cyberware, Entry source, Entry target) {
        Set<Item> badItems = safeItems(cyberware.incompatibleCyberware(source.stack(), source.slot()));
        if (badItems.contains(target.stack().getItem())) return true;

        Set<TagKey<Item>> badTags = safeTags(cyberware.incompatibleCyberwareTags(source.stack(), source.slot()));
        for (TagKey<Item> badTag : badTags) {
            if (matchesTag(target, badTag)) return true;
        }

        return false;
    }

    private static boolean matchesAnySameSlotIncompatible(ICyberwareItem cyberware, Entry source, Entry target) {
        Set<TagKey<Item>> badTags = safeTags(cyberware.sameSlotIncompatibleCyberwareTags(source.stack(), source.slot()));
        for (TagKey<Item> badTag : badTags) {
            if (matchesTag(target, badTag)) return true;
        }

        return false;
    }

    private static boolean matchesTag(Entry entry, TagKey<Item> tag) {
        if (entry.stack().is(tag)) return true;
        if (!(entry.stack().getItem() instanceof ICyberwareItem cyberware)) return false;
        return cyberware.matchesCyberwareTagAsInstalled(entry.stack(), entry.slot(), tag);
    }

    private static void addMaximumStackWarnings(List<Entry> finalLoadout, List<Component> warnings) {
        Set<String> reported = new HashSet<>();

        for (Entry entry : finalLoadout) {
            if (!(entry.stack().getItem() instanceof ICyberwareItem cyberware)) continue;

            int limit = Math.max(1, cyberware.maxStacksPerSlotType(entry.stack(), entry.slot()));
            int count = 0;

            for (Entry other : finalLoadout) {
                if (other.slot() == entry.slot() && other.stack().getItem() == entry.stack().getItem()) count++;
            }

            if (count <= limit) continue;

            String key = entry.stack().getItem() + "|" + entry.slot().name();
            if (!reported.add(key)) continue;

            warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.stack_limit", entry.stack().getHoverName(), limit).withStyle(ChatFormatting.RED));
        }
    }

    private static void addOregrinderWarnings(List<Entry> finalLoadout, List<Component> warnings) {
        Entry oregrinder = null;
        Entry wetware = null;

        for (Entry entry : finalLoadout) {
            if (entry.stack().getItem() instanceof OregrinderItem) oregrinder = entry;
            if (entry.stack().is(ModTags.Items.WETWARE_ITEM)) wetware = entry;
        }

        if (oregrinder != null && wetware != null) {
            warnings.add(Component.translatable("gui.createcybernetics.surgery.warning.incompatible", oregrinder.stack().getHoverName(), wetware.stack().getHoverName()).withStyle(ChatFormatting.RED));
        }
    }

    private static EnergyPreview calculateEnergyPreview(Player patient, List<Entry> finalLoadout) {
        long generated = 0L;
        long required = 0L;

        for (Entry entry : finalLoadout) {
            if (!(entry.stack().getItem() instanceof ICyberwareItem cyberware)) continue;
            generated += Math.max(0, cyberware.getEnergyGeneratedThisTick(patient, entry.stack(), entry.slot()));
            required += Math.max(0, cyberware.getEnergyUsedPerTick(patient, entry.stack(), entry.slot()));
        }

        return new EnergyPreview(clampToInt(generated), clampToInt(required));
    }

    private static Component joinItemNames(Set<Item> items) {
        Component result = Component.empty();
        boolean first = true;

        for (Item item : items) {
            if (!first) result = result.copy().append(Component.literal(", "));
            result = result.copy().append(new ItemStack(item).getHoverName());
            first = false;
        }

        return result;
    }

    private static List<Component> deduplicateWarnings(List<Component> warnings) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<Component> result = new ArrayList<>();

        for (Component warning : warnings) {
            if (seen.add(warning.getString())) result.add(warning);
        }

        return List.copyOf(result);
    }

    private static ItemStack safeCopy(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    private static Set<Item> safeItems(Set<Item> items) {
        return items == null ? Set.of() : items;
    }

    private static Set<TagKey<Item>> safeTags(Set<TagKey<Item>> tags) {
        return tags == null ? Set.of() : tags;
    }

    private static int clampToInt(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }
}
