package com.perigrine3.createcybernetics.item.macroprogram;

import com.perigrine3.createcybernetics.CreateCybernetics;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public final class ModMacroprogramArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateCybernetics.MODID);

    public static final Holder<ArmorMaterial> BASIC = ARMOR_MATERIALS.register("macroprogram_basic_armor",
            () -> create("macroprogram_basic_armor"));

    public static final Holder<ArmorMaterial> INTERMEDIATE = ARMOR_MATERIALS.register("macroprogram_intermediate_armor",
            () -> create("macroprogram_intermediate_armor"));

    public static final Holder<ArmorMaterial> ADVANCED = ARMOR_MATERIALS.register("macroprogram_advanced_armor",
            () -> create("macroprogram_advanced_armor"));

    public static final Holder<ArmorMaterial> ADMINISTRATOR_ACCESS = ARMOR_MATERIALS.register("macroprogram_administrator_access_armor",
            () -> create("macroprogram_administrator_access_armor"));

    private ModMacroprogramArmorMaterials() {}

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }

    private static ArmorMaterial create(String textureName) {
        return new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                    map.put(ArmorItem.Type.BOOTS, 0);
                    map.put(ArmorItem.Type.LEGGINGS, 0);
                    map.put(ArmorItem.Type.CHESTPLATE, 0);
                    map.put(ArmorItem.Type.HELMET, 0);
                    map.put(ArmorItem.Type.BODY, 0);
                }),
                0,
                SoundEvents.ARMOR_EQUIP_GENERIC,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(CreateCybernetics.MODID, textureName))),
                0.0F,
                0.0F
        );
    }
}