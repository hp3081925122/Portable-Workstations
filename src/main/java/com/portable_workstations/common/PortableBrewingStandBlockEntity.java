package com.portable_workstations.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PortableBrewingStandBlockEntity extends BrewingStandBlockEntity {
    public PortableBrewingStandBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void setChanged() {
    }

    public ContainerData portableDataAccess() {
        return dataAccess;
    }
}
