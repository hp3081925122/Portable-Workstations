package com.portable_workstations.common;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

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
        if (data[0] > 0 || hasFuel && hasInput) {
            RecipeHolder<AbstractCookingRecipe> recipe = hasInput
                    ? level.recipeAccess().getRecipeFor((RecipeType<AbstractCookingRecipe>) recipeType, new SingleRecipeInput(input), level).orElse(null)
                    : null;
            if (recipe != null && data[3] <= 0) {
                data[3] = recipe.value().cookingTime();
            }

            int burnTime = portableFurnace.getPortableBurnTime(fuel, level.fuelValues());
            if (data[0] <= 0 && recipe != null && canBurn(level.registryAccess(), furnace, recipe)) {
                data[0] = burnTime;
                data[1] = burnTime;
                if (burnTime > 0) {
                    changed = true;
                    if (fuel.getCraftingRemainder() != null) {
                        furnace.setItem(1, fuel.getCraftingRemainder().create());
                    } else {
                        fuel.shrink(1);
                        furnace.setItem(1, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
                    }
                }
            }

            if (data[0] > 0 && recipe != null && canBurn(level.registryAccess(), furnace, recipe)) {
                data[2]++;
                if (data[2] >= data[3]) {
                    data[2] = 0;
                    data[3] = recipe.value().cookingTime();
                    burn(level.registryAccess(), furnace, recipe);
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

    private static boolean canBurn(RegistryAccess registryAccess, AbstractFurnaceBlockEntity furnace, RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (furnace.getItem(0).isEmpty() || recipe == null) {
            return false;
        }

        ItemStack result = recipe.value().assemble(new SingleRecipeInput(furnace.getItem(0)));
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

    private static void burn(RegistryAccess registryAccess, AbstractFurnaceBlockEntity furnace, RecipeHolder<? extends AbstractCookingRecipe> recipe) {
        if (!canBurn(registryAccess, furnace, recipe)) {
            return;
        }

        ItemStack input = furnace.getItem(0);
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(furnace.getItem(0)));
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
        furnace.setRecipeUsed(recipe);
    }
}
