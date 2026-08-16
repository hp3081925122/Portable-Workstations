package com.portable_workstations;

import com.mojang.logging.LogUtils;
import com.portable_workstations.common.PortableWorkstationsCapability;
import com.portable_workstations.common.PortableWorkstationsConfig;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Portable_workstations.MODID)
public class Portable_workstations {
    public static final String MODID = "portable_workstations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Portable_workstations(IEventBus modEventBus, ModContainer modContainer) {
        PortableWorkstationsConfig.load();
        PortableWorkstationsCapability.ATTACHMENTS.register(modEventBus);
        modEventBus.addListener(PortableWorkstationsNetwork::register);
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
