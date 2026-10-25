package com.starforge.uncrafting;

import com.starforge.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * 拆解台容器：1 个输入槽 + 3×3 产物显示槽 + 玩家背包。
 *
 * <p>产物槽是「预览」：里面显示的是当前配方需要的材料，点击任意一个产物槽会把
 * 3×3 的材料一次性发给玩家，并消耗一份输入物品。这样既保留拆解台的直观布局，
 * 又不会让玩家把预览物品当成真实库存拿走。</p>
 *
 * <p>界面没有方块实体，输入物品只存在于这次打开的菜单里；关闭界面时会把剩余输入
 * 还给玩家，和 Simple Uncrafting Table 的做法一致。</p>
 */
public final class UncraftingMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int OUTPUT_END = 9;
    private static final int PLAYER_INV_START = 10;
    private static final int PLAYER_INV_END = 46;

    private final Player player;
    private final UncraftingContainer container;
    private final List<CraftingRecipe> recipes = new ArrayList<>();

    private int recipeIndex;
    private int recipeOutputCount;
    private boolean refreshing;
    private ItemStack lastInput = ItemStack.EMPTY;

    public UncraftingMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.UNCRAFTING_TABLE.get(), containerId);
        this.player = playerInventory.player;
        this.container = new UncraftingContainer(this);

        this.addSlot(new Slot(container, INPUT_SLOT, 36, 35));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new OutputSlot(container, OUTPUT_START + row * 3 + column,
                        94 + column * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    // ------------------------------------------------------------------ 配方预览

    /** 输入槽内容发生变化时由容器回调。 */
    private void onContainerChanged() {
        if (refreshing || player.level().isClientSide()) {
            return;
        }
        refreshOutputs();
    }

    private void refreshOutputs() {
        if (refreshing || player.level().isClientSide()) {
            return;
        }

        refreshing = true;
        try {
            clearOutputs();
            recipes.clear();
            recipeOutputCount = 0;

            ItemStack input = container.getItem(INPUT_SLOT);
            if (input.isEmpty()) {
                lastInput = ItemStack.EMPTY;
            } else if (player.level() instanceof ServerLevel level) {
                if (!ItemStack.isSameItem(input, lastInput)) {
                    recipeIndex = 0;
                }
                lastInput = input.copy();

                for (RecipeHolder<CraftingRecipe> holder
                        : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                    CraftingRecipe recipe = holder.value();
                    ItemStack result = recipe.getResultItem(level.registryAccess());
                    if (!result.isEmpty()
                            && ItemStack.isSameItem(result, input)
                            && result.getCount() <= input.getCount()) {
                        recipes.add(recipe);
                    }
                }

                if (!recipes.isEmpty()) {
                    if (recipeIndex >= recipes.size()) {
                        recipeIndex = 0;
                    }
                    showCurrentRecipe(level);
                }
            }
        } finally {
            refreshing = false;
        }
        broadcastChanges();
    }

    private void showCurrentRecipe(ServerLevel level) {
        CraftingRecipe recipe = recipes.get(recipeIndex);
        ItemStack result = recipe.getResultItem(level.registryAccess());
        recipeOutputCount = result.getCount();

        List<Ingredient> grid = gridOf(recipe);
        for (int i = 0; i < 9; i++) {
            Ingredient ingredient = grid.get(i);
            ItemStack[] options = ingredient.getItems();
            if (options.length > 0) {
                container.setItem(OUTPUT_START + i, options[0].copyWithCount(1));
            }
        }
    }

    /** 把任意 3×3 以内的配方铺成固定的 9 格，保持有序配方的位置。 */
    private static List<Ingredient> gridOf(CraftingRecipe recipe) {
        List<Ingredient> grid = new ArrayList<>(9);
        List<Ingredient> ingredients = recipe.getIngredients();

        if (recipe instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int index = row * width + column;
                    if (row < height && column < width && index < ingredients.size()) {
                        grid.add(ingredients.get(index));
                    } else {
                        grid.add(Ingredient.EMPTY);
                    }
                }
            }
        } else {
            for (int i = 0; i < 9; i++) {
                grid.add(i < ingredients.size() ? ingredients.get(i) : Ingredient.EMPTY);
            }
        }
        return grid;
    }

    private void clearOutputs() {
        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }
    }

    private void takeAllOutputs(Player player) {
        if (player.level().isClientSide() || !canCraft()) {
            return;
        }

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack output = container.getItem(i);
            if (output.isEmpty()) {
                continue;
            }
            ItemStack copy = output.copy();
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        }

        ItemStack input = container.getItem(INPUT_SLOT);
        int remaining = input.getCount() - recipeOutputCount;
        container.setItem(INPUT_SLOT, remaining <= 0 ? ItemStack.EMPTY : input.copyWithCount(remaining));
        broadcastChanges();
    }

    public boolean canCraft() {
        ItemStack input = container.getItem(INPUT_SLOT);
        return !input.isEmpty() && recipeOutputCount > 0 && input.getCount() >= recipeOutputCount;
    }

    public int getRecipeCount() {
        return recipes.size();
    }

    /** 客户端点界面上的 &lt; &gt; 按钮时，服务端切换显示哪一条匹配配方。 */
    public void cycleRecipe(int delta) {
        if (recipes.size() <= 1 || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        recipeIndex = Math.floorMod(recipeIndex + delta, recipes.size());
        refreshing = true;
        try {
            clearOutputs();
            showCurrentRecipe(level);
        } finally {
            refreshing = false;
        }
        broadcastChanges();
    }

    // ------------------------------------------------------------------ 容器交互

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= OUTPUT_START && slotId <= OUTPUT_END) {
            if (!player.level().isClientSide()) {
                takeAllOutputs(player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= OUTPUT_START && index <= OUTPUT_END) {
            if (!player.level().isClientSide()) {
                takeAllOutputs(player);
            }
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        boolean moved;
        if (index == INPUT_SLOT) {
            moved = this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false);
        } else {
            moved = this.moveItemStackTo(stack, INPUT_SLOT, OUTPUT_START, false);
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) {
            return;
        }

        refreshing = true;
        try {
            ItemStack input = container.getItem(INPUT_SLOT);
            container.setItem(INPUT_SLOT, ItemStack.EMPTY);
            clearOutputs();
            if (!input.isEmpty() && !player.getInventory().add(input)) {
                player.drop(input, false);
            }
        } finally {
            refreshing = false;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static final class OutputSlot extends Slot {

        OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static final class UncraftingContainer extends SimpleContainer {

        private final UncraftingMenu menu;

        UncraftingContainer(UncraftingMenu menu) {
            super(10);
            this.menu = menu;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            this.menu.onContainerChanged();
        }
    }
}
