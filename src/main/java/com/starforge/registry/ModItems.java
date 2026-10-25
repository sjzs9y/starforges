package com.starforge.registry;

import com.starforge.Starforge;
import com.starforge.whisper.WhisperItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All items of the mod.
 *
 * <p>Since 1.20.5 the attack damage / attack speed of a tool is no longer baked into the item
 * constructor: it lives in {@link Item.Properties#attributes}. Use the {@code createAttributes}
 * helper of the matching item class so the vanilla defaults are preserved.</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Starforge.MOD_ID);

    // ------------------------------------------------------------------ materials

    public static final DeferredItem<Item> STARFORGE_INGOT = ITEMS.registerSimpleItem("starforge_ingot");

    public static final DeferredItem<BlockItem> STARFORGE_BLOCK = ITEMS.register("starforge_block",
            () -> new BlockItem(ModBlocks.STARFORGE_BLOCK.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> FLOWING_IMAGE = ITEMS.register("flowing_image",
            () -> new BlockItem(ModBlocks.FLOWING_IMAGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> UNCRAFTING_TABLE = ITEMS.register("uncrafting_table",
            () -> new BlockItem(ModBlocks.UNCRAFTING_TABLE.get(), new Item.Properties()));

    // ------------------------------------------------------------------ 轻语轻扬

    public static final DeferredItem<WhisperItem> WHISPER = ITEMS.register("whisper",
            () -> new WhisperItem(new Item.Properties().stacksTo(1)));

    // ------------------------------------------------------------------ tools

    public static final DeferredItem<SwordItem> STARFORGE_SWORD = ITEMS.register("starforge_sword",
            () -> new SwordItem(ModToolTiers.STARFORGE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(ModToolTiers.STARFORGE, 3, -2.4F))));

    public static final DeferredItem<PickaxeItem> STARFORGE_PICKAXE = ITEMS.register("starforge_pickaxe",
            () -> new PickaxeItem(ModToolTiers.STARFORGE, new Item.Properties()
                    .attributes(PickaxeItem.createAttributes(ModToolTiers.STARFORGE, 1.0F, -2.8F))));

    public static final DeferredItem<AxeItem> STARFORGE_AXE = ITEMS.register("starforge_axe",
            () -> new AxeItem(ModToolTiers.STARFORGE, new Item.Properties()
                    .attributes(AxeItem.createAttributes(ModToolTiers.STARFORGE, 5.0F, -3.0F))));

    public static final DeferredItem<ShovelItem> STARFORGE_SHOVEL = ITEMS.register("starforge_shovel",
            () -> new ShovelItem(ModToolTiers.STARFORGE, new Item.Properties()
                    .attributes(ShovelItem.createAttributes(ModToolTiers.STARFORGE, 1.5F, -3.0F))));

    public static final DeferredItem<HoeItem> STARFORGE_HOE = ITEMS.register("starforge_hoe",
            () -> new HoeItem(ModToolTiers.STARFORGE, new Item.Properties()
                    .attributes(HoeItem.createAttributes(ModToolTiers.STARFORGE, 0.0F, -3.0F))));

    // ------------------------------------------------------------------ armor

    public static final DeferredItem<ArmorItem> STARFORGE_HELMET = ITEMS.register("starforge_helmet",
            () -> new ArmorItem(ModArmorMaterials.STARFORGE, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(37))));

    public static final DeferredItem<ArmorItem> STARFORGE_CHESTPLATE = ITEMS.register("starforge_chestplate",
            () -> new ArmorItem(ModArmorMaterials.STARFORGE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(37))));

    public static final DeferredItem<ArmorItem> STARFORGE_LEGGINGS = ITEMS.register("starforge_leggings",
            () -> new ArmorItem(ModArmorMaterials.STARFORGE, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(37))));

    public static final DeferredItem<ArmorItem> STARFORGE_BOOTS = ITEMS.register("starforge_boots",
            () -> new ArmorItem(ModArmorMaterials.STARFORGE, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(37))));

    private ModItems() {
    }
}
