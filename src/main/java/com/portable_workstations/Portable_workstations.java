package com.portable_workstations;

import com.portable_workstations.common.PortableWorkstationsCapability;
import com.portable_workstations.common.PortableWorkstationsEvents;
import com.portable_workstations.common.PortableWorkstationsInteractions;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.ModInitializer;

public class Portable_workstations implements ModInitializer {
    public static final String MODID = "portable_workstations";

    @Override
    public void onInitialize() {
        PortableWorkstationsCapability.register();
        PortableWorkstationsNetwork.register();
        PortableWorkstationsInteractions.register();
        PortableWorkstationsEvents.register();
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
