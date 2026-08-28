package com.perigrine3.createcybernetics.common.quickhack;

import net.minecraft.network.chat.Component;

public record MicroprogramDefinition(String id, MicroprogramCategory category, String displayName, int ramCost) {

    public Component getDisplayName() {
        return Component.literal(displayName);
    }

    public Component getDescription() {
        return Component.translatable("microprogram.createcybernetics." + id + ".desc");
    }
}