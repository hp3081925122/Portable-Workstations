package com.portable_workstations.client;

import com.portable_workstations.common.WorkstationType;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PortableWorkstationsClientState {
    private static List<ClientEntry> entries = List.of();
    private static int bookshelves;

    private PortableWorkstationsClientState() {
    }

    public static void apply(List<PortableWorkstationsNetwork.SyncEntry> syncedEntries, int syncedBookshelves) {
        List<ClientEntry> next = new ArrayList<>();
        for (PortableWorkstationsNetwork.SyncEntry syncedEntry : syncedEntries) {
            WorkstationType type = WorkstationType.byId(syncedEntry.type());
            if (type != null && type.matches(syncedEntry.stack())) {
                next.add(new ClientEntry(type, syncedEntry.stack().copyWithCount(1)));
            }
        }
        entries = List.copyOf(next);
        bookshelves = Math.max(0, Math.min(15, syncedBookshelves));
    }

    public static List<ClientEntry> entries() {
        return entries;
    }

    public record ClientEntry(WorkstationType type, ItemStack stack) {
    }
}
