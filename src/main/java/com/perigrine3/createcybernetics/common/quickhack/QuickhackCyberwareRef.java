package com.perigrine3.createcybernetics.common.quickhack;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.api.InstalledCyberware;

public record QuickhackCyberwareRef(CyberwareSlot slot, int index, InstalledCyberware installed) {
}