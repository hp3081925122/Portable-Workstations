package com.portable_workstations.network;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.common.PortableWorkstationsActions;
import com.portable_workstations.common.PortableWorkstationsCapability;
import com.portable_workstations.common.PortableWorkstationsData;
import com.portable_workstations.common.WorkstationType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
        PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            WorkstationType type = WorkstationType.byId(payload.typeId());
            if (type != null) {
                context.server().execute(() -> PortableWorkstationsActions.handleAction(context.player(), type, payload.retrieve()));
            }
        });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncPayload.TYPE, (payload, context) ->
                context.client().execute(() -> com.portable_workstations.client.PortableWorkstationsClientState.apply(payload.entries(), payload.bookshelves())));
    }

    public static void requestAction(WorkstationType type, boolean retrieve) {
        ClientPlayNetworking.send(new ActionPayload(type.id(), retrieve));
    }

    public static void sync(ServerPlayer player) {
        PortableWorkstationsData data = PortableWorkstationsCapability.data(player);
        List<SyncEntry> entries = new ArrayList<>();
        for (PortableWorkstationsData.WorkstationEntry entry : data.entries()) {
            entries.add(new SyncEntry(entry.type().id(), entry.displayStack().copyWithCount(1)));
        }
        ServerPlayNetworking.send(player, new SyncPayload(entries, data.bookshelves()));
    }

    public record SyncEntry(String type, ItemStack stack) {
    }

    private record ActionPayload(String typeId, boolean retrieve) implements CustomPacketPayload {
        private static final Type<ActionPayload> TYPE = new Type<>(ACTION_ID);
        private static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeUtf(payload.typeId());
                    buffer.writeBoolean(payload.retrieve());
                },
                buffer -> new ActionPayload(buffer.readUtf(64), buffer.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private record SyncPayload(List<SyncEntry> entries, int bookshelves) implements CustomPacketPayload {
        private static final Type<SyncPayload> TYPE = new Type<>(SYNC_ID);
        private static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> CODEC = StreamCodec.of(
                (buffer, payload) -> {
                    buffer.writeVarInt(payload.entries().size());
                    for (SyncEntry entry : payload.entries()) {
                        buffer.writeUtf(entry.type());
                        ItemStack.STREAM_CODEC.encode(buffer, entry.stack());
                    }
                    buffer.writeVarInt(payload.bookshelves());
                },
                buffer -> {
                    int count = buffer.readVarInt();
                    List<SyncEntry> entries = new ArrayList<>();
                    for (int index = 0; index < count; index++) {
                        entries.add(new SyncEntry(buffer.readUtf(64), ItemStack.STREAM_CODEC.decode(buffer)));
                    }
                    return new SyncPayload(entries, buffer.readVarInt());
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
