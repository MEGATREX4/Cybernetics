package com.perigrine3.createcybernetics.common.quickhack.function;

import com.perigrine3.createcybernetics.common.quickhack.QuickhackCyberwareRef;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionContext;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackFunctionResult;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackFunctionSettings;

import java.util.List;

@FunctionalInterface
public interface QuickhackFunction {
    QuickhackFunctionResult execute(QuickhackExecutionContext context, List<QuickhackCyberwareRef> targets, QuickhackFunctionSettings settings);
}