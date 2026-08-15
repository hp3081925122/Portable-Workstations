package com.portable_workstations.common;

import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public final class PortableWorkstationsActions {
    private PortableWorkstationsActions() {
    }

    public static void handleAction(ServerPlayer player, WorkstationType type, boolean retrieve) {
        player.getCapability(PortableWorkstationsCapability.DATA).ifPresent(data -> {
            if (!data.isUnlocked(type)) {
                PortableWorkstationsNetwork.sync(player);
                return;
            }
            if (retrieve) {
                retrieve(player, data, type);
            } else {
                open(player, data, type);
            }
        });
    }

    private static void retrieve(ServerPlayer player, PortableWorkstationsData data, WorkstationType type) {
        player.closeContainer();
        net.minecraft.world.item.ItemStack stack = data.remove(type, player.serverLevel());
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        PortableWorkstationsNetwork.sync(player);
    }

    private static void open(ServerPlayer player, PortableWorkstationsData data, WorkstationType type) {
        PortableWorkstationsData.WorkstationEntry entry = data.entry(type);
        if (entry == null) {
            return;
        }

        if (type == WorkstationType.FURNACE || type == WorkstationType.BLAST_FURNACE) {
            entry.furnace().furnaceEntity().setLevel(player.serverLevel());
        } else if (type == WorkstationType.BREWING_STAND) {
            entry.brewingStand().setLevel(player.serverLevel());
        }
        switch (type) {
            case FURNACE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableFurnaceMenu(id, inventory, entry.furnace()), Component.translatable("container.furnace")));
            case CRAFTING_TABLE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableCraftingMenu(id, inventory), Component.translatable("container.crafting")));
            case ANVIL -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableAnvilMenu(id, inventory, data), Component.translatable("container.repair")));
            case ENCHANTING_TABLE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableEnchantmentMenu(id, inventory, data), Component.translatable("container.enchant")));
            case STONECUTTER -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableStonecutterMenu(id, inventory), Component.translatable("container.stonecutter")));
            case BLAST_FURNACE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableBlastFurnaceMenu(id, inventory, entry.furnace()), Component.translatable("container.blast_furnace")));
            case BREWING_STAND -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableBrewingStandMenu(id, inventory, entry.brewingStand()), Component.translatable("container.brewing")));
            case ENDER_CHEST -> openEnderChest(player);
            case LOOM -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableLoomMenu(id, inventory), Component.translatable("container.loom")));
            case GRINDSTONE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableGrindstoneMenu(id, inventory), Component.translatable("container.grindstone_title")));
            case SMITHING_TABLE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableSmithingMenu(id, inventory), Component.translatable("container.upgrade")));
            case CARTOGRAPHY_TABLE -> player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PortableWorkstationMenus.PortableCartographyMenu(id, inventory), Component.translatable("container.cartography_table")));
        }
    }

    private static void openEnderChest(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()), Component.translatable("container.enderchest")));
        player.awardStat(Stats.OPEN_ENDERCHEST);
    }
}
