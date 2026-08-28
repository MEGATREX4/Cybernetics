package com.perigrine3.createcybernetics.common.quickhack;

import java.util.List;

public record QuickhackInstruction(MicroprogramDefinition target, MicroprogramDefinition function, List<MicroprogramDefinition> modifiers) {

    public QuickhackInstruction {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    }
}