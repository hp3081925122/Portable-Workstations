package com.portable_workstations.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.fabricmc.fabric.api.registry.FuelRegistry;

public class PortableFurnaceBlockEntity extends FurnaceBlockEntity implements PortableFurnaceAccess {
    public PortableFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void setChanged() {
    }

    public AbstractFurnaceBlockEntity furnaceEntity() {
        return this;
    }

    public int getPortableData(int index) {
        return dataAccess.get(index);
    }

    public void setPortableData(int index, int value) {
        dataAccess.set(index, value);
    }

    public net.minecraft.world.inventory.ContainerData portableDataAccess() {
        return dataAccess;
    }

    public int getPortableBurnTime(ItemStack stack) {
        Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
        return burnTime == null ? 0 : burnTime;
    }
}
