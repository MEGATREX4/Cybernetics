package com.perigrine3.createcybernetics.common.quickhack;

import net.minecraft.network.chat.Component;

import java.util.List;

public record QuickhackFunctionResult(boolean success, List<Component> casterOutput) {

    public static QuickhackFunctionResult success(List<Component> casterOutput) {
        return new QuickhackFunctionResult(true, casterOutput == null ? List.of() : List.copyOf(casterOutput));
    }

    public static QuickhackFunctionResult failure() {
        return new QuickhackFunctionResult(false, List.of());
    }
}