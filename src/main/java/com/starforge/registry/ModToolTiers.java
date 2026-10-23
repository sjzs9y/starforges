package com.starforge.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Tool material of the mod.
 *
 * <p>In 1.21 a {@link Tier} no longer exposes a numeric "mining level". Instead it declares the
 * block tag it cannot harvest ({@link #getIncorrectBlocksForDrops()}), which is the same mechanism
 * vanilla uses for its tool materials. Re-using the netherite tag means star-forged tools can mine
 * exactly the same blocks as netherite tools.</p>
 */
public final class ModToolTiers {

    public static final Tier STARFORGE = new Tier() {

        @Override
        public int getUses() {
            return 2400;
        }

        @Override
        public float getSpeed() {
            return 10.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 5.0F;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 22;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.STARFORGE_INGOT.get());
        }
    };

    private ModToolTiers() {
    }
}
