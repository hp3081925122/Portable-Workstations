package com.portable_workstations.network;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.common.PortableWorkstationsActions;
import com.portable_workstations.common.PortableWorkstationsCapability;
import com.portable_workstations.common.PortableWorkstationsData;
import com.portable_workstations.common.WorkstationType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

public final class PortableWorkstationsNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private PortableWorkstationsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToServer(WorkstationActionPayload.TYPE, WorkstationActionPayload.CODEC, PortableWorkstationsNetwork::handleAction)
                .playToClient(WorkstationSyncPayload.TYPE, WorkstationSyncPayload.CODEC, PortableWorkstationsNetwork::handleSync);
    }

    public static void requestAction(WorkstationType type, boolean retrieve) {
        ClientPacketDistributor.sendToServer(new WorkstationActionPayload(type.id(), retrieve));
    }

    public static void sync(ServerPlayer player) {
        PortableWorkstationsData data = player.getData(PortableWorkstationsCapability.DATA);
        List<SyncEntry> entries = new ArrayList<>();
        for (PortableWorkstationsData.WorkstationEntry entry : data.entries()) {
            entries.add(new SyncEntry(entry.type().id(), entry.displayStack().copyWithCount(1)));
        }
        PacketDistributor.sendToPlayer(player, new WorkstationSyncPayload(entries, data.bookshelves()));
    }

    private static void handleAction(WorkstationActionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            WorkstationType type = WorkstationType.byId(payload.workstationType());
            if (type != null) {
                PortableWorkstationsActions.handleAction(player, type, payload.retrieve());
            }
        }
    }

    private static void handleSync(WorkstationSyncPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> com.portable_workstations.client.PortableWorkstationsClientState.apply(payload.entries(), payload.bookshelves()));
    }

    public record SyncEntry(String type, ItemStack stack) {
    }

    private record WorkstationActionPayload(String workstationType, boolean retrieve) implements CustomPacketPayload {
        private static final Type<WorkstationActionPayload> TYPE =
                new Type<>(Portable_workstations.location("workstation_action"));
        private static final StreamCodec<RegistryFriendlyByteBuf, WorkstationActionPayload> CODEC =
                CustomPacketPayload.codec(WorkstationActionPayload::write, WorkstationActionPayload::new);

        private WorkstationActionPayload(RegistryFriendlyByteBuf buffer) {
            this(buffer.readUtf(64), buffer.readBoolean());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(workstationType);
            buffer.writeBoolean(retrieve);
        }

        @Override
        public Type<WorkstationActionPayload> type() {
            return TYPE;
        }
    }

    private record WorkstationSyncPayload(List<SyncEntry> entries, int bookshelves) implements CustomPacketPayload {
        private static final Type<WorkstationSyncPayload> TYPE =
                new Type<>(Portable_workstations.location("workstation_sync"));
        private static final StreamCodec<RegistryFriendlyByteBuf, WorkstationSyncPayload> CODEC =
                CustomPacketPayload.codec(WorkstationSyncPayload::write, WorkstationSyncPayload::new);

        private WorkstationSyncPayload(RegistryFriendlyByteBuf buffer) {
            this(readEntries(buffer), buffer.readVarInt());
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(entries.size());
            for (SyncEntry entry : entries) {
                buffer.writeUtf(entry.type());
                ItemStack.STREAM_CODEC.encode(buffer, entry.stack());
            }
            buffer.writeVarInt(bookshelves);
        }

        private static List<SyncEntry> readEntries(RegistryFriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<SyncEntry> entries = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                entries.add(new SyncEntry(buffer.readUtf(64), ItemStack.STREAM_CODEC.decode(buffer)));
            }
            return entries;
        }

        @Override
        public Type<WorkstationSyncPayload> type() {
            return TYPE;
        }
    }
}
