package com.portable_workstations.common;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Portable_workstations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PortableWorkstationsEvents {
    private PortableWorkstationsEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof net.minecraft.world.entity.player.Player) {
            event.addCapability(Portable_workstations.location("workstations"), new PortableWorkstationsProvider());
        }
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            event.getOriginal().getCapability(PortableWorkstationsCapability.DATA).ifPresent(oldData ->
                    event.getEntity().getCapability(PortableWorkstationsCapability.DATA).ifPresent(newData -> newData.copyFrom(oldData)));
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortableWorkstationsNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortableWorkstationsNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortableWorkstationsNetwork.sync(player);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        player.getCapability(PortableWorkstationsCapability.DATA).ifPresent(data -> {
            data.tickFurnaces(player.serverLevel());
            data.tickBrewingStands(player.serverLevel(), player.blockPosition());
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
        });
    }
}
