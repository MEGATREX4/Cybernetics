package com.perigrine3.createcybernetics.common.macroprogram;

import net.minecraft.network.chat.Component;

public record MacroprogramDefinition(String id, MacroprogramCategory category, int activeTicks, int cooldownTicks, int ramCost) {

    public Component getDisplayName() {
        return Component.translatable("macroprogram.createcybernetics." + id + ".name");
    }

    public Component getDescription() {
        return Component.translatable("macroprogram.createcybernetics." + id + ".desc");
    }

    public boolean hasDuration() {
        return activeTicks > 0;
    }

    public boolean hasCooldown() {
        return cooldownTicks > 0;
    }

    public boolean hasRamCost() {
        return ramCost > 0;
    }
}