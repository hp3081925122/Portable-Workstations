package com.portable_workstations.common;

import com.portable_workstations.Portable_workstations;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class PortableWorkstationsCapability {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Portable_workstations.MODID);
    public static final Supplier<AttachmentType<PortableWorkstationsData>> DATA = ATTACHMENTS.register(
            "workstations",
            () -> AttachmentType.serializable(PortableWorkstationsData::new).copyOnDeath().build());

    private PortableWorkstationsCapability() {
    }
}
