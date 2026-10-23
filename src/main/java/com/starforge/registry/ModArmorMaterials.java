package com.starforge.registry;

import com.starforge.Starforge;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Armor material of the mod.
 *
 * <p>Since 1.20.5 an {@link ArmorMaterial} is a plain record that has to be registered in the
 * {@code minecraft:armor_material} registry. The {@link ArmorMaterial.Layer} name decides which
 * texture is used: {@code assets/starforge/textures/models/armor/starforge_layer_1.png} and
 * {@code .../starforge_layer_2.png}.</p>
 */
public final class ModArmorMaterials {

    public static final DeferredRegister<ArmorMaterial> MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Starforge.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STARFORGE = MATERIALS.register(
            "starforge",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 4,
                            ArmorItem.Type.CHESTPLATE, 9,
                            ArmorItem.Type.LEGGINGS, 7,
                            ArmorItem.Type.BOOTS, 4,
                            ArmorItem.Type.BODY, 14),
                    22,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.STARFORGE_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(Starforge.MOD_ID, "starforge"))),
                    3.0F,
                    0.1F));

    private ModArmorMaterials() {
    }
}
