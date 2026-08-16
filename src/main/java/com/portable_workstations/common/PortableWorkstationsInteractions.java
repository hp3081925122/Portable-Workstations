package com.portable_workstations.common;

import com.portable_workstations.Portable_workstations;
import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Portable_workstations.MODID)
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
        if (type != null && !PortableWorkstationsConfig.isAllowed(stack)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        PortableWorkstationsData data = event.getEntity().getData(PortableWorkstationsCapability.DATA);
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
    }
}
