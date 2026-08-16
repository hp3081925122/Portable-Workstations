package com.portable_workstations.network;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.common.PortableWorkstationsActions;
import com.portable_workstations.common.PortableWorkstationsCapability;
import com.portable_workstations.common.PortableWorkstationsData;
import com.portable_workstations.common.WorkstationType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PortableWorkstationsNetwork {
    private static final ResourceLocation ACTION_ID = Portable_workstations.location("action");
    private static final ResourceLocation SYNC_ID = Portable_workstations.location("sync");

    private PortableWorkstationsNetwork() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION_ID, (server, player, handler, buffer, responseSender) -> {
            String typeId = buffer.readUtf(64);
            boolean retrieve = buffer.readBoolean();
            server.execute(() -> {
                WorkstationType type = WorkstationType.byId(typeId);
                if (type != null) {
                    PortableWorkstationsActions.handleAction(player, type, retrieve);
                }
            });
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_ID, (client, handler, buffer, responseSender) -> {
            int count = buffer.readVarInt();
            List<SyncEntry> entries = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                entries.add(new SyncEntry(buffer.readUtf(64), buffer.readItem()));
            }
            int bookshelves = buffer.readVarInt();
            client.execute(() -> com.portable_workstations.client.PortableWorkstationsClientState.apply(entries, bookshelves));
        });
    }

    public static void requestAction(WorkstationType type, boolean retrieve) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeUtf(type.id());
        buffer.writeBoolean(retrieve);
        ClientPlayNetworking.send(ACTION_ID, buffer);
    }

    public static void sync(ServerPlayer player) {
        PortableWorkstationsData data = PortableWorkstationsCapability.data(player);
        FriendlyByteBuf buffer = PacketByteBufs.create();
        List<PortableWorkstationsData.WorkstationEntry> entries = data.entries();
        buffer.writeVarInt(entries.size());
        for (PortableWorkstationsData.WorkstationEntry entry : entries) {
            buffer.writeUtf(entry.type().id());
            buffer.writeItem(entry.displayStack().copyWithCount(1));
        }
        buffer.writeVarInt(data.bookshelves());
        ServerPlayNetworking.send(player, SYNC_ID, buffer);
    }

    public record SyncEntry(String type, ItemStack stack) {
    }
}
