package com.perigrine3.createcybernetics.world.dimension;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.IEventBus;

public class ModDimensions {

    public static final ResourceKey<Level> CYBERSPACE_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace")
    );

    public static final ResourceKey<LevelStem> CYBERSPACE_STEM = ResourceKey.create(
            Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace")
    );

    public static final ResourceKey<DimensionType> CYBERSPACE_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, "cyberspace")
    );



    public static void register(IEventBus modEventBus) {
    }

}