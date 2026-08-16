package com.portable_workstations.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private CompoundTag pendingRoot;
    private HolderLookup.Provider provider;

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

    public void setProvider(HolderLookup.Provider provider) {
        this.provider = provider;
        if (pendingRoot != null) {
            CompoundTag root = pendingRoot;
            pendingRoot = null;
            deserializeNBT(root, provider);
        }
    }

    public boolean addBookshelf() {
        if (bookshelves >= 15 || !isUnlocked(WorkstationType.ENCHANTING_TABLE)) {
            return false;
        }
        bookshelves++;
        return true;
    }

    public boolean unlock(WorkstationType type, ItemStack source, ServerLevel level) {
        provider = level.registryAccess();
        if (entries.containsKey(type)) {
            return false;
        }

        ItemStack displayStack = type == WorkstationType.ANVIL ? source.copyWithCount(1) : type.defaultStack();
        WorkstationEntry entry = new WorkstationEntry(type, displayStack);
        if (type == WorkstationType.FURNACE || type == WorkstationType.BLAST_FURNACE || type == WorkstationType.SMOKER) {
            entry.furnace = createFurnace(source, level, type);
        } else if (type == WorkstationType.BREWING_STAND) {
            entry.brewingStand = createBrewingStand(source, level);
        }
        entries.put(type, entry);
        return true;
    }

    public ItemStack remove(WorkstationType type, ServerLevel level) {
        provider = level.registryAccess();
        WorkstationEntry entry = entries.remove(type);
        if (entry == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = entry.displayStack.copyWithCount(1);
        if (entry.furnace != null) {
            entry.furnace.furnaceEntity().setLevel(level);
            BlockEntityType<?> blockEntityType = type == WorkstationType.BLAST_FURNACE ? BlockEntityType.BLAST_FURNACE : type == WorkstationType.SMOKER ? BlockEntityType.SMOKER : BlockEntityType.FURNACE;
            BlockItem.setBlockEntityData(result, blockEntityType, entry.furnace.furnaceEntity().saveWithoutMetadata(provider));
        }
        if (entry.brewingStand != null) {
            entry.brewingStand.setLevel(level);
            BlockItem.setBlockEntityData(result, BlockEntityType.BREWING_STAND, entry.brewingStand.saveWithoutMetadata(provider));
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
        provider = level.registryAccess();
        for (WorkstationEntry entry : entries.values()) {
            if (entry.furnace == null) {
                continue;
            }
            entry.furnace.furnaceEntity().setLevel(level);
            if (!entry.furnace.furnaceEntity().getItem(0).isEmpty() || !entry.furnace.furnaceEntity().getItem(1).isEmpty() || entry.furnace.getPortableData(0) > 0 || entry.furnace.getPortableData(2) > 0) {
                PortableFurnaceTicker.tick(level, entry.furnace, entry.type == WorkstationType.BLAST_FURNACE ? net.minecraft.world.item.crafting.RecipeType.BLASTING : entry.type == WorkstationType.SMOKER ? net.minecraft.world.item.crafting.RecipeType.SMOKING : net.minecraft.world.item.crafting.RecipeType.SMELTING);
            }
        }
    }

    public void tickBrewingStands(ServerLevel level, BlockPos soundPos) {
        provider = level.registryAccess();
        for (WorkstationEntry entry : entries.values()) {
            if (entry.brewingStand == null) {
                continue;
            }
            entry.brewingStand.setLevel(level);
            BrewingStandBlockEntity.serverTick(level, soundPos, Blocks.BREWING_STAND.defaultBlockState(), entry.brewingStand);
        }
    }

    public CompoundTag serializeNBT() {
        if (provider == null) {
            return pendingRoot == null ? new CompoundTag() : pendingRoot.copy();
        }
        return serializeNBT(provider);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        this.provider = provider;
        CompoundTag root = new CompoundTag();
        root.putInt(BOOKSHELVES_TAG, bookshelves);
        ListTag serializedEntries = new ListTag();
        for (WorkstationEntry entry : entries.values()) {
            CompoundTag serializedEntry = new CompoundTag();
            serializedEntry.putString(TYPE_TAG, entry.type.id());
            serializedEntry.put(STACK_TAG, entry.displayStack.save(provider));
            if (entry.furnace != null) {
                serializedEntry.put(FURNACE_TAG, entry.furnace.furnaceEntity().saveWithoutMetadata(provider));
            }
            if (entry.brewingStand != null) {
                serializedEntry.put(BREWING_STAND_TAG, entry.brewingStand.saveWithoutMetadata(provider));
            }
            serializedEntries.add(serializedEntry);
        }
        root.put(ENTRIES_TAG, serializedEntries);
        return root;
    }

    public void deserializeNBT(CompoundTag root) {
        pendingRoot = root.copy();
        entries.clear();
        bookshelves = Math.max(0, Math.min(15, root.getInt(BOOKSHELVES_TAG)));
        if (provider != null) {
            CompoundTag pending = pendingRoot;
            pendingRoot = null;
            deserializeNBT(pending, provider);
        }
    }

    private void deserializeNBT(CompoundTag root, HolderLookup.Provider provider) {
        entries.clear();
        bookshelves = Math.max(0, Math.min(15, root.getInt(BOOKSHELVES_TAG)));
        ListTag serializedEntries = root.getList(ENTRIES_TAG, 10);
        for (int index = 0; index < serializedEntries.size(); index++) {
            CompoundTag serializedEntry = serializedEntries.getCompound(index);
            WorkstationType type = WorkstationType.byId(serializedEntry.getString(TYPE_TAG));
            if (type == null || entries.containsKey(type)) {
                continue;
            }

            ItemStack stack = ItemStack.parseOptional(provider, serializedEntry.getCompound(STACK_TAG));
            if (!type.matches(stack)) {
                stack = type.defaultStack();
            }
            WorkstationEntry entry = new WorkstationEntry(type, stack.copyWithCount(1));
            if ((type == WorkstationType.FURNACE || type == WorkstationType.BLAST_FURNACE || type == WorkstationType.SMOKER) && serializedEntry.contains(FURNACE_TAG, 10)) {
                entry.furnace = createFurnaceFromTag(serializedEntry.getCompound(FURNACE_TAG), type, provider);
            } else if (type == WorkstationType.BREWING_STAND && serializedEntry.contains(BREWING_STAND_TAG, 10)) {
                entry.brewingStand = createBrewingStandFromTag(serializedEntry.getCompound(BREWING_STAND_TAG), provider);
            }
            entries.put(type, entry);
        }
    }

    public void copyFrom(PortableWorkstationsData source) {
        CompoundTag copy = source.serializeNBT();
        deserializeNBT(copy);
    }

    private static PortableFurnaceAccess createFurnace(ItemStack source, ServerLevel level, WorkstationType type) {
        PortableFurnaceAccess furnace = type == WorkstationType.BLAST_FURNACE
                ? new PortableBlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState())
                : type == WorkstationType.SMOKER
                ? new PortableSmokerBlockEntity(BlockPos.ZERO, Blocks.SMOKER.defaultBlockState())
                : new PortableFurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        furnace.furnaceEntity().setLevel(level);
        CustomData blockEntityData = source.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            furnace.furnaceEntity().loadWithComponents(blockEntityData.copyTag(), level.registryAccess());
        }
        return furnace;
    }

    private static PortableFurnaceAccess createFurnaceFromTag(CompoundTag tag, WorkstationType type, HolderLookup.Provider provider) {
        PortableFurnaceAccess furnace = type == WorkstationType.BLAST_FURNACE
                ? new PortableBlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState())
                : type == WorkstationType.SMOKER
                ? new PortableSmokerBlockEntity(BlockPos.ZERO, Blocks.SMOKER.defaultBlockState())
                : new PortableFurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        furnace.furnaceEntity().loadWithComponents(tag, provider);
        return furnace;
    }

    private static PortableBrewingStandBlockEntity createBrewingStand(ItemStack source, ServerLevel level) {
        PortableBrewingStandBlockEntity brewingStand = new PortableBrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        brewingStand.setLevel(level);
        CustomData blockEntityData = source.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            brewingStand.loadWithComponents(blockEntityData.copyTag(), level.registryAccess());
        }
        return brewingStand;
    }

    private static PortableBrewingStandBlockEntity createBrewingStandFromTag(CompoundTag tag, HolderLookup.Provider provider) {
        PortableBrewingStandBlockEntity brewingStand = new PortableBrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        brewingStand.loadWithComponents(tag, provider);
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
