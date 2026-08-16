package com.portable_workstations.common;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.server.level.ServerPlayer;

public final class PortableWorkstationsEvents {
    private PortableWorkstationsEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PortableWorkstationsNetwork.sync(handler.getPlayer()));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                PortableWorkstationsCapability.data(newPlayer).copyFrom(PortableWorkstationsCapability.data(oldPlayer)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> PortableWorkstationsNetwork.sync(newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                PortableWorkstationsData data = PortableWorkstationsCapability.data(player);
            data.tickFurnaces(player.serverLevel());
            data.tickBrewingStands(player.serverLevel(), player.blockPosition());
            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastChanges();
            }
            }
        });
    }
}
