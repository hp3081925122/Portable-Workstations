package com.portable_workstations.common;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum WorkstationType {
    FURNACE("furnace", Blocks.FURNACE),
    CRAFTING_TABLE("crafting_table", Blocks.CRAFTING_TABLE),
    ANVIL("anvil", Blocks.ANVIL),
    ENCHANTING_TABLE("enchanting_table", Blocks.ENCHANTING_TABLE),
    STONECUTTER("stonecutter", Blocks.STONECUTTER),
    BLAST_FURNACE("blast_furnace", Blocks.BLAST_FURNACE),
    BREWING_STAND("brewing_stand", Blocks.BREWING_STAND),
    ENDER_CHEST("ender_chest", Blocks.ENDER_CHEST),
    LOOM("loom", Blocks.LOOM),
    GRINDSTONE("grindstone", Blocks.GRINDSTONE),
    SMITHING_TABLE("smithing_table", Blocks.SMITHING_TABLE),
    FLETCHING_TABLE("fletching_table", Blocks.FLETCHING_TABLE),
    CARTOGRAPHY_TABLE("cartography_table", Blocks.CARTOGRAPHY_TABLE);

    private final String id;
    private final Block defaultBlock;

    WorkstationType(String id, Block defaultBlock) {
        this.id = id;
        this.defaultBlock = defaultBlock;
    }

    public String id() {
        return id;
    }

    public Block defaultBlock() {
        return defaultBlock;
    }

    public ItemStack defaultStack() {
        return new ItemStack(defaultBlock.asItem());
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Block block = Block.byItem(stack.getItem());
        if (this == ANVIL) {
            return block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL;
        }
        return block == defaultBlock;
    }

    public static WorkstationType fromStack(ItemStack stack) {
        for (WorkstationType type : values()) {
            if (type.matches(stack)) {
                return type;
            }
        }
        return null;
    }

    public static WorkstationType byId(String id) {
        for (WorkstationType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
