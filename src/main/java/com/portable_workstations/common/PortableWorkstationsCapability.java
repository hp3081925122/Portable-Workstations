package com.portable_workstations.common;

import com.portable_workstations.Portable_workstations;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Portable_workstations.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PortableWorkstationsCapability {
    public static final Capability<PortableWorkstationsData> DATA = CapabilityManager.get(new CapabilityToken<>() {
    });

    private PortableWorkstationsCapability() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(PortableWorkstationsData.class);
    }
}
