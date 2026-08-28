package com.perigrine3.createcybernetics.item.generic;

import net.minecraft.ChatFormatting;

public enum MicroprogramType {
    TARGET("microprogram.createcybernetics.type.target", ChatFormatting.AQUA),
    FUNCTION("microprogram.createcybernetics.type.function", ChatFormatting.GOLD),
    MODIFIER("microprogram.createcybernetics.type.modifier", ChatFormatting.LIGHT_PURPLE);

    private final String translationKey;
    private final ChatFormatting color;

    MicroprogramType(String translationKey, ChatFormatting color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public ChatFormatting getColor() {
        return color;
    }
}