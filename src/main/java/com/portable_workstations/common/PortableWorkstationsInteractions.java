package com.portable_workstations.common;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Portable_workstations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PortableWorkstationsInteractions {
    private PortableWorkstationsInteractions() {
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        WorkstationType type = WorkstationType.fromStack(stack);
        boolean isBookshelf = stack.is(Blocks.BOOKSHELF.asItem());
        if (type == null && !isBookshelf) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        event.getEntity().getCapability(PortableWorkstationsCapability.DATA).ifPresent(data -> {
            if (isBookshelf) {
                if (data.addBookshelf()) {
                    stack.shrink(1);
                    PortableWorkstationsNetwork.sync((net.minecraft.server.level.ServerPlayer) event.getEntity());
                }
                return;
            }

            if (type != null && data.unlock(type, stack, level)) {
                stack.shrink(1);
                PortableWorkstationsNetwork.sync((net.minecraft.server.level.ServerPlayer) event.getEntity());
            }
        });
    }
}
