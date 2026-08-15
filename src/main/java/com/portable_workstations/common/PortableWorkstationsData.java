package com.portable_workstations.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PortableWorkstationsData {
    private static final String ENTRIES_TAG = "Entries";
    private static final String TYPE_TAG = "Type";
    private static final String STACK_TAG = "Stack";
    private static final String FURNACE_TAG = "Furnace";
    private static final String BREWING_STAND_TAG = "BrewingStand";
    private static final String BOOKSHELVES_TAG = "Bookshelves";

    private final Map<WorkstationType, WorkstationEntry> entries = new EnumMap<>(WorkstationType.class);
    private int bookshelves;

    public List<WorkstationEntry> entries() {
        return List.copyOf(entries.values());
    }

    public WorkstationEntry entry(WorkstationType type) {
        return entries.get(type);
    }

    public boolean isUnlocked(WorkstationType type) {
        return entries.containsKey(type);
    }

    public int bookshelves() {
        return bookshelves;
    }

    public boolean addBookshelf() {
        if (bookshelves >= 15 || !isUnlocked(WorkstationType.ENCHANTING_TABLE)) {
            return false;
        }
        bookshelves++;
        return true;
    }

    public boolean unlock(WorkstationType type, ItemStack source, ServerLevel level) {
        if (entries.containsKey(type)) {
            return false;
        }

        ItemStack displayStack = type == WorkstationType.ANVIL ? source.copyWithCount(1) : type.defaultStack();
        WorkstationEntry entry = new WorkstationEntry(type, displayStack);
        if (type == WorkstationType.FURNACE) {
            entry.furnace = createFurnace(source, level, false);
        } else if (type == WorkstationType.BLAST_FURNACE) {
            entry.furnace = createFurnace(source, level, true);
        } else if (type == WorkstationType.BREWING_STAND) {
            entry.brewingStand = createBrewingStand(source, level);
        }
        entries.put(type, entry);
        return true;
    }

    public ItemStack remove(WorkstationType type, ServerLevel level) {
        WorkstationEntry entry = entries.remove(type);
        if (entry == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = entry.displayStack.copyWithCount(1);
        if (entry.furnace != null) {
            entry.furnace.furnaceEntity().setLevel(level);
            BlockEntityType<?> blockEntityType = type == WorkstationType.BLAST_FURNACE ? BlockEntityType.BLAST_FURNACE : BlockEntityType.FURNACE;
            BlockItem.setBlockEntityData(result, blockEntityType, entry.furnace.furnaceEntity().saveWithoutMetadata());
        }
        if (entry.brewingStand != null) {
            entry.brewingStand.setLevel(level);
            BlockItem.setBlockEntityData(result, BlockEntityType.BREWING_STAND, entry.brewingStand.saveWithoutMetadata());
        }
        return result;
    }

    public boolean damageAnvil() {
        WorkstationEntry entry = entries.get(WorkstationType.ANVIL);
        if (entry == null) {
            return false;
        }

        BlockState state = Block.byItem(entry.displayStack.getItem()).defaultBlockState();
        BlockState damaged = AnvilBlock.damage(state);
        if (damaged == null) {
            entries.remove(WorkstationType.ANVIL);
            return false;
        }
        entry.displayStack = new ItemStack(damaged.getBlock().asItem());
        return true;
    }

    public void tickFurnaces(ServerLevel level) {
        for (WorkstationEntry entry : entries.values()) {
            if (entry.furnace == null) {
                continue;
            }
            entry.furnace.furnaceEntity().setLevel(level);
            if (!entry.furnace.furnaceEntity().getItem(0).isEmpty() || !entry.furnace.furnaceEntity().getItem(1).isEmpty() || entry.furnace.getPortableData(0) > 0 || entry.furnace.getPortableData(2) > 0) {
                PortableFurnaceTicker.tick(level, entry.furnace, entry.type == WorkstationType.BLAST_FURNACE ? net.minecraft.world.item.crafting.RecipeType.BLASTING : net.minecraft.world.item.crafting.RecipeType.SMELTING);
            }
        }
    }

    public void tickBrewingStands(ServerLevel level, BlockPos soundPos) {
        for (WorkstationEntry entry : entries.values()) {
            if (entry.brewingStand == null) {
                continue;
            }
            entry.brewingStand.setLevel(level);
            BrewingStandBlockEntity.serverTick(level, soundPos, Blocks.BREWING_STAND.defaultBlockState(), entry.brewingStand);
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag root = new CompoundTag();
        root.putInt(BOOKSHELVES_TAG, bookshelves);
        ListTag serializedEntries = new ListTag();
        for (WorkstationEntry entry : entries.values()) {
            CompoundTag serializedEntry = new CompoundTag();
            serializedEntry.putString(TYPE_TAG, entry.type.id());
            serializedEntry.put(STACK_TAG, entry.displayStack.save(new CompoundTag()));
            if (entry.furnace != null) {
                serializedEntry.put(FURNACE_TAG, entry.furnace.furnaceEntity().saveWithoutMetadata());
            }
            if (entry.brewingStand != null) {
                serializedEntry.put(BREWING_STAND_TAG, entry.brewingStand.saveWithoutMetadata());
            }
            serializedEntries.add(serializedEntry);
        }
        root.put(ENTRIES_TAG, serializedEntries);
        return root;
    }

    public void deserializeNBT(CompoundTag root) {
        entries.clear();
        bookshelves = Math.max(0, Math.min(15, root.getInt(BOOKSHELVES_TAG)));
        ListTag serializedEntries = root.getList(ENTRIES_TAG, 10);
        for (int index = 0; index < serializedEntries.size(); index++) {
            CompoundTag serializedEntry = serializedEntries.getCompound(index);
            WorkstationType type = WorkstationType.byId(serializedEntry.getString(TYPE_TAG));
            if (type == null || entries.containsKey(type)) {
                continue;
            }

            ItemStack stack = ItemStack.of(serializedEntry.getCompound(STACK_TAG));
            if (!type.matches(stack)) {
                stack = type.defaultStack();
            }
            WorkstationEntry entry = new WorkstationEntry(type, stack.copyWithCount(1));
            if (type == WorkstationType.FURNACE && serializedEntry.contains(FURNACE_TAG, 10)) {
                entry.furnace = createFurnaceFromTag(serializedEntry.getCompound(FURNACE_TAG), false);
            } else if (type == WorkstationType.BLAST_FURNACE && serializedEntry.contains(FURNACE_TAG, 10)) {
                entry.furnace = createFurnaceFromTag(serializedEntry.getCompound(FURNACE_TAG), true);
            } else if (type == WorkstationType.BREWING_STAND && serializedEntry.contains(BREWING_STAND_TAG, 10)) {
                entry.brewingStand = createBrewingStandFromTag(serializedEntry.getCompound(BREWING_STAND_TAG));
            }
            entries.put(type, entry);
        }
    }

    public void copyFrom(PortableWorkstationsData source) {
        deserializeNBT(source.serializeNBT());
    }

    private static PortableFurnaceAccess createFurnace(ItemStack source, ServerLevel level, boolean blast) {
        PortableFurnaceAccess furnace = blast
                ? new PortableBlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState())
                : new PortableFurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        furnace.furnaceEntity().setLevel(level);
        CompoundTag blockEntityTag = BlockItem.getBlockEntityData(source);
        if (blockEntityTag != null) {
            furnace.furnaceEntity().load(blockEntityTag);
        }
        return furnace;
    }

    private static PortableFurnaceAccess createFurnaceFromTag(CompoundTag tag, boolean blast) {
        PortableFurnaceAccess furnace = blast
                ? new PortableBlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState())
                : new PortableFurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        furnace.furnaceEntity().load(tag);
        return furnace;
    }

    private static PortableBrewingStandBlockEntity createBrewingStand(ItemStack source, ServerLevel level) {
        PortableBrewingStandBlockEntity brewingStand = new PortableBrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        brewingStand.setLevel(level);
        CompoundTag blockEntityTag = BlockItem.getBlockEntityData(source);
        if (blockEntityTag != null) {
            brewingStand.load(blockEntityTag);
        }
        return brewingStand;
    }

    private static PortableBrewingStandBlockEntity createBrewingStandFromTag(CompoundTag tag) {
        PortableBrewingStandBlockEntity brewingStand = new PortableBrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        brewingStand.load(tag);
        return brewingStand;
    }

    public static final class WorkstationEntry {
        private final WorkstationType type;
        private ItemStack displayStack;
        private PortableFurnaceAccess furnace;
        private PortableBrewingStandBlockEntity brewingStand;

        private WorkstationEntry(WorkstationType type, ItemStack displayStack) {
            this.type = type;
            this.displayStack = displayStack;
        }

        public WorkstationType type() {
            return type;
        }

        public ItemStack displayStack() {
            return displayStack;
        }

        public PortableFurnaceAccess furnace() {
            return furnace;
        }

        public PortableBrewingStandBlockEntity brewingStand() {
            return brewingStand;
        }
    }
}
