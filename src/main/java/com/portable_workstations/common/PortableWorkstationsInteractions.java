package com.portable_workstations.common;

import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

public final class PortableWorkstationsInteractions {
    private PortableWorkstationsInteractions() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!isWorkstationOrBookshelf(stack)) {
                return InteractionResultHolder.pass(stack);
            }
            if (level.isClientSide) {
                return InteractionResultHolder.pass(stack);
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResultHolder.fail(stack);
            }
            PortableWorkstationsData data = PortableWorkstationsCapability.data((net.minecraft.server.level.ServerPlayer) player);
            WorkstationType type = WorkstationType.fromStack(stack);
            boolean isBookshelf = stack.is(Blocks.BOOKSHELF.asItem());
            if (type != null && data.isUnlocked(type)) {
                return InteractionResultHolder.pass(stack);
            }
            if (isBookshelf && (data.bookshelves() >= 15 || !data.isUnlocked(WorkstationType.ENCHANTING_TABLE))) {
                return InteractionResultHolder.pass(stack);
            }
            if (unlock(player, stack, serverLevel)) {
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.fail(stack);
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!isWorkstationOrBookshelf(stack)) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.FAIL;
            }
            PortableWorkstationsData data = PortableWorkstationsCapability.data((net.minecraft.server.level.ServerPlayer) player);
            WorkstationType type = WorkstationType.fromStack(stack);
            boolean isBookshelf = stack.is(Blocks.BOOKSHELF.asItem());
            if (type != null && data.isUnlocked(type)) {
                return InteractionResult.PASS;
            }
            if (isBookshelf && (data.bookshelves() >= 15 || !data.isUnlocked(WorkstationType.ENCHANTING_TABLE))) {
                return InteractionResult.PASS;
            }
            return unlock(player, stack, serverLevel) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        });
    }

    private static boolean isWorkstationOrBookshelf(ItemStack stack) {
        WorkstationType type = WorkstationType.fromStack(stack);
        boolean isBookshelf = stack.is(Blocks.BOOKSHELF.asItem());
        return type != null || isBookshelf;
    }

    private static boolean unlock(net.minecraft.world.entity.player.Player player, ItemStack stack, ServerLevel level) {
        WorkstationType type = WorkstationType.fromStack(stack);
        boolean isBookshelf = stack.is(Blocks.BOOKSHELF.asItem());
        PortableWorkstationsData data = PortableWorkstationsCapability.data((net.minecraft.server.level.ServerPlayer) player);
        if (isBookshelf) {
            if (data.addBookshelf()) {
                stack.shrink(1);
                PortableWorkstationsNetwork.sync((net.minecraft.server.level.ServerPlayer) player);
                return true;
            }
            return false;
        }
        if (type != null && data.unlock(type, stack, level)) {
            stack.shrink(1);
            PortableWorkstationsNetwork.sync((net.minecraft.server.level.ServerPlayer) player);
            return true;
        }
        return false;
    }
}
