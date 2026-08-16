package com.portable_workstations.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.portable_workstations.Portable_workstations;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;

public final class PortableWorkstationsCapability {
    private static final Codec<PortableWorkstationsData> CODEC = Codec.PASSTHROUGH.xmap(dynamic -> {
        PortableWorkstationsData data = new PortableWorkstationsData();
        data.deserializeNBT((CompoundTag) dynamic.convert(NbtOps.INSTANCE).getValue());
        return data;
    }, data -> new Dynamic<>(NbtOps.INSTANCE, data.serializeNBT()));
    public static final AttachmentType<PortableWorkstationsData> DATA = AttachmentRegistry.<PortableWorkstationsData>builder()
            .persistent(CODEC)
            .copyOnDeath()
            .initializer(PortableWorkstationsData::new)
            .buildAndRegister(Portable_workstations.location("workstations"));

    private PortableWorkstationsCapability() {
    }

    public static void register() {
    }

    public static PortableWorkstationsData data(ServerPlayer player) {
        PortableWorkstationsData data = player.getAttachedOrCreate(DATA);
        data.setProvider(player.serverLevel().registryAccess());
        return data;
    }
}
