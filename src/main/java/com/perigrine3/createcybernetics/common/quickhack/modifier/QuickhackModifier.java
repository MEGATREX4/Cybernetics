package com.perigrine3.createcybernetics.common.quickhack.modifier;

import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionContext;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackFunctionSettings;

@FunctionalInterface
public interface QuickhackModifier {
    void apply(QuickhackExecutionContext context, QuickhackFunctionSettings settings);
}