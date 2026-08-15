package com.portable_workstations.common;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

public final class PortableWorkstationsProvider implements ICapabilitySerializable<CompoundTag> {
    private final PortableWorkstationsData data = new PortableWorkstationsData();
    private final LazyOptional<PortableWorkstationsData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        return capability == PortableWorkstationsCapability.DATA ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }
}
