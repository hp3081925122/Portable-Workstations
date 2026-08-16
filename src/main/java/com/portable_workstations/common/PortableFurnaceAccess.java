package com.portable_workstations.common;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;

public interface PortableFurnaceAccess {
    AbstractFurnaceBlockEntity furnaceEntity();

    int getPortableData(int index);

    void setPortableData(int index, int value);

    ContainerData portableDataAccess();

    int getPortableBurnTime(ItemStack fuel, FuelValues fuelValues);
}
