package com.portable_workstations;

import com.mojang.logging.LogUtils;
import com.portable_workstations.common.PortableWorkstationsConfig;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Portable_workstations.MODID)
public class Portable_workstations {
    public static final String MODID = "portable_workstations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Portable_workstations() {
        PortableWorkstationsConfig.load();
        PortableWorkstationsNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
