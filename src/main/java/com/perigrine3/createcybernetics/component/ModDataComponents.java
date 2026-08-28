package com.perigrine3.createcybernetics.component;

import com.mojang.serialization.Codec;
import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public final class ModDataComponents {
    private ModDataComponents() {}

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(CreateCybernetics.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> COORDINATES =
            register("coordinates", builder -> builder.persistent(BlockPos.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PotionContents>> POTION_AUTOINJECTOR_CONTENTS =
            register("potion_autoinjector_contents", builder -> builder
                    .persistent(PotionContents.CODEC)
                    .networkSynchronized(PotionContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SECONDARY_DYED_COLOR =
            register("secondary_dyed_color", builder -> builder
                    .persistent(Codec.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MicroprogramLibrary>> MICROPROGRAM_LIBRARY =
            register("microprogram_library", builder -> builder
                    .persistent(MicroprogramLibrary.CODEC));

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name,
            UnaryOperator<DataComponentType.Builder<T>> builderOperator
    ) {
        return DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}