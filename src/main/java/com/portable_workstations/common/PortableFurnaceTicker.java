package com.portable_workstations.common;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.fabricmc.fabric.api.registry.FuelRegistry;

public final class PortableFurnaceTicker {
    private PortableFurnaceTicker() {
    }

    public static void tick(ServerLevel level, PortableFurnaceAccess portableFurnace, RecipeType<?> recipeType) {
        AbstractFurnaceBlockEntity furnace = portableFurnace.furnaceEntity();
        int[] data = new int[]{portableFurnace.getPortableData(0), portableFurnace.getPortableData(1), portableFurnace.getPortableData(2), portableFurnace.getPortableData(3)};
        boolean wasLit = data[0] > 0;
        boolean changed = wasLit;
        if (wasLit) {
            data[0]--;
        }

        ItemStack input = furnace.getItem(0);
        ItemStack fuel = furnace.getItem(1);
        boolean hasInput = !input.isEmpty();
        boolean hasFuel = !fuel.isEmpty();
        RecipeHolder<?> recipeHolder = hasInput
                ? (RecipeHolder<?>) ((java.util.Optional<?>) level.getRecipeManager().getRecipeFor((RecipeType) recipeType, new SingleRecipeInput(input), level)).orElse(null)
                : null;
        Recipe<?> recipe = recipeHolder == null ? null : recipeHolder.value();
        if (data[0] > 0 || hasFuel && hasInput) {
            if (recipe instanceof AbstractCookingRecipe cookingRecipe && data[3] <= 0) {
                data[3] = cookingRecipe.getCookingTime();
            }

            int burnTime = portableFurnace.getPortableBurnTime(fuel);
            if (data[0] <= 0 && recipeHolder != null && canBurn(level.registryAccess(), furnace, recipeHolder)) {
                data[0] = burnTime;
                data[1] = burnTime;
                if (burnTime > 0) {
                    changed = true;
                    if (fuel.getItem().hasCraftingRemainingItem()) {
                        furnace.setItem(1, new ItemStack(fuel.getItem().getCraftingRemainingItem()));
                    } else {
                        fuel.shrink(1);
                        furnace.setItem(1, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
                    }
                }
            }

            if (data[0] > 0 && recipeHolder != null && canBurn(level.registryAccess(), furnace, recipeHolder)) {
                data[2]++;
                if (data[2] >= data[3]) {
                    data[2] = 0;
                    data[3] = recipe instanceof AbstractCookingRecipe cookingRecipe ? cookingRecipe.getCookingTime() : 200;
                    burn(level.registryAccess(), furnace, recipeHolder);
                }
                changed = true;
            } else {
                data[2] = 0;
            }
        } else if (data[2] > 0) {
            data[2] = Math.max(data[2] - 2, 0);
            changed = true;
        }

        if (wasLit != data[0] > 0) {
            changed = true;
        }
        if (changed) {
            for (int index = 0; index < data.length; index++) {
                portableFurnace.setPortableData(index, data[index]);
            }
            furnace.setChanged();
        }
    }

    private static boolean canBurn(RegistryAccess registryAccess, AbstractFurnaceBlockEntity furnace, RecipeHolder<?> recipeHolder) {
        if (furnace.getItem(0).isEmpty()) {
            return false;
        }

        ItemStack result = assemble(recipeHolder.value(), furnace.getItem(0), registryAccess);
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = furnace.getItem(2);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItem(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= furnace.getMaxStackSize() && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private static void burn(RegistryAccess registryAccess, AbstractFurnaceBlockEntity furnace, RecipeHolder<?> recipeHolder) {
        if (!canBurn(registryAccess, furnace, recipeHolder)) {
            return;
        }

        ItemStack input = furnace.getItem(0);
        ItemStack result = assemble(recipeHolder.value(), input, registryAccess);
        ItemStack output = furnace.getItem(2);
        if (output.isEmpty()) {
            furnace.setItem(2, result.copy());
        } else if (ItemStack.isSameItem(output, result)) {
            output.grow(result.getCount());
        }

        if (input.is(Blocks.WET_SPONGE.asItem()) && !furnace.getItem(1).isEmpty() && furnace.getItem(1).is(Items.BUCKET)) {
            furnace.setItem(1, new ItemStack(Items.WATER_BUCKET));
        }
        input.shrink(1);
        furnace.setItem(0, input);
        furnace.setRecipeUsed(recipeHolder);
    }

    private static ItemStack assemble(Recipe<?> recipe, ItemStack input, RegistryAccess registryAccess) {
        return ((Recipe) recipe).assemble(new SingleRecipeInput(input), registryAccess);
    }
}
