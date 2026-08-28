package com.perigrine3.createcybernetics.common.quickhack;

import net.minecraft.network.chat.Component;

import java.util.List;

public record QuickhackExecutionResult(boolean success, boolean notifyTarget, List<Component> casterOutput, Component failureMessage) {

    public static QuickhackExecutionResult success(boolean notifyTarget, List<Component> casterOutput) {
        return new QuickhackExecutionResult(true, notifyTarget, casterOutput == null ? List.of() : List.copyOf(casterOutput), null);
    }

    public static QuickhackExecutionResult failure(Component failureMessage) {
        return new QuickhackExecutionResult(false, false, List.of(), failureMessage);
    }
}