package com.portable_workstations.network;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.common.PortableWorkstationsActions;
import com.portable_workstations.common.PortableWorkstationsCapability;
import com.portable_workstations.common.PortableWorkstationsData;
import com.portable_workstations.common.WorkstationType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class PortableWorkstationsNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Portable_workstations.location("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static int packetId;
    private static boolean registered;

    private PortableWorkstationsNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.registerMessage(packetId++, WorkstationActionPacket.class, WorkstationActionPacket::encode, WorkstationActionPacket::decode, WorkstationActionPacket::handle);
        CHANNEL.registerMessage(packetId++, WorkstationSyncPacket.class, WorkstationSyncPacket::encode, WorkstationSyncPacket::decode, WorkstationSyncPacket::handle);
    }

    public static void requestAction(WorkstationType type, boolean retrieve) {
        CHANNEL.sendToServer(new WorkstationActionPacket(type.id(), retrieve));
    }

    public static void sync(ServerPlayer player) {
        player.getCapability(PortableWorkstationsCapability.DATA).ifPresent(data -> {
            List<SyncEntry> entries = new ArrayList<>();
            for (PortableWorkstationsData.WorkstationEntry entry : data.entries()) {
                entries.add(new SyncEntry(entry.type().id(), entry.displayStack().copyWithCount(1)));
            }
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WorkstationSyncPacket(entries, data.bookshelves()));
        });
    }

    public record SyncEntry(String type, ItemStack stack) {
    }

    private record WorkstationActionPacket(String type, boolean retrieve) {
        private static void encode(WorkstationActionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.type);
            buffer.writeBoolean(packet.retrieve);
        }

        private static WorkstationActionPacket decode(FriendlyByteBuf buffer) {
            return new WorkstationActionPacket(buffer.readUtf(64), buffer.readBoolean());
        }

        private static void handle(WorkstationActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                WorkstationType type = WorkstationType.byId(packet.type);
                if (player != null && type != null) {
                    PortableWorkstationsActions.handleAction(player, type, packet.retrieve);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record WorkstationSyncPacket(List<SyncEntry> entries, int bookshelves) {
        private static void encode(WorkstationSyncPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.entries.size());
            for (SyncEntry entry : packet.entries) {
                buffer.writeUtf(entry.type);
                buffer.writeItem(entry.stack);
            }
            buffer.writeVarInt(packet.bookshelves);
        }

        private static WorkstationSyncPacket decode(FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<SyncEntry> entries = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                entries.add(new SyncEntry(buffer.readUtf(64), buffer.readItem()));
            }
            return new WorkstationSyncPacket(entries, buffer.readVarInt());
        }

        private static void handle(WorkstationSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.portable_workstations.client.PortableWorkstationsClientState.apply(packet.entries, packet.bookshelves)));
            context.setPacketHandled(true);
        }
    }
}
