package com.perigrine3.createcybernetics.common.quickhack.target;

import com.perigrine3.createcybernetics.common.quickhack.QuickhackCyberwareRef;
import com.perigrine3.createcybernetics.common.quickhack.QuickhackExecutionContext;

import java.util.List;

@FunctionalInterface
public interface QuickhackTarget {
    List<QuickhackCyberwareRef> resolve(QuickhackExecutionContext context);
}