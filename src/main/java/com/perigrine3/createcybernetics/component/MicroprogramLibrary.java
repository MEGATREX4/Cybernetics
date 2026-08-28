package com.perigrine3.createcybernetics.component;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record MicroprogramLibrary(List<ResourceLocation> microprograms) {
    public static final int CAPACITY = 14;

    public static final MicroprogramLibrary EMPTY = new MicroprogramLibrary(List.of());

    public static final Codec<MicroprogramLibrary> CODEC =
            ResourceLocation.CODEC.listOf().xmap(MicroprogramLibrary::new, MicroprogramLibrary::microprograms);

    public MicroprogramLibrary {
        microprograms = List.copyOf(microprograms);
    }

    public int size() {
        return microprograms.size();
    }

    public boolean isFull() {
        return size() >= CAPACITY;
    }

    public boolean contains(ResourceLocation id) {
        return microprograms.contains(id);
    }

    public boolean canAdd(ResourceLocation id) {
        return id != null && !contains(id) && !isFull();
    }

    public MicroprogramLibrary add(ResourceLocation id) {
        if (!canAdd(id)) {
            return this;
        }

        List<ResourceLocation> updated = new ArrayList<>(microprograms);
        updated.add(id);

        return new MicroprogramLibrary(updated);
    }

    public MicroprogramLibrary remove(ResourceLocation id) {
        if (id == null || !contains(id)) {
            return this;
        }

        List<ResourceLocation> updated = new ArrayList<>(microprograms);
        updated.remove(id);

        return new MicroprogramLibrary(updated);
    }

    public MicroprogramLibrary clear() {
        return EMPTY;
    }
}