package com.portable_workstations.common;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PortableWorkstationsData implements ValueIOSerializable {
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
        if (type == WorkstationType.FURNACE || type == WorkstationType.BLAST_FURNACE || type == WorkstationType.SMOKER) {
            entry.furnace = createFurnace(source, level, type);
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
            BlockEntityType<?> blockEntityType = type == WorkstationType.BLAST_FURNACE ? BlockEntityType.BLAST_FURNACE : type == WorkstationType.SMOKER ? BlockEntityType.SMOKER : BlockEntityType.FURNACE;
            result.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntityType, entry.furnace.furnaceEntity().saveWithoutMetadata(level.registryAccess())));
        }
        if (entry.brewingStand != null) {
            entry.brewingStand.setLevel(level);
            result.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(BlockEntityType.BREWING_STAND, entry.brewingStand.saveWithoutMetadata(level.registryAccess())));
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
                PortableFurnaceTicker.tick(level, entry.furnace, entry.type == WorkstationType.BLAST_FURNACE ? net.minecraft.world.item.crafting.RecipeType.BLASTING : entry.type == WorkstationType.SMOKER ? net.minecraft.world.item.crafting.RecipeType.SMOKING : net.minecraft.world.item.crafting.RecipeType.SMELTING);
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

    @Override
    public void serialize(ValueOutput output) {
        output.putInt(BOOKSHELVES_TAG, bookshelves);
        ValueOutput.ValueOutputList serializedEntries = output.childrenList(ENTRIES_TAG);
        for (WorkstationEntry entry : entries.values()) {
            ValueOutput serializedEntry = serializedEntries.addChild();
            serializedEntry.putString(TYPE_TAG, entry.type.id());
            serializedEntry.store(STACK_TAG, ItemStack.CODEC, entry.displayStack);
            if (entry.furnace != null) {
                entry.furnace.furnaceEntity().saveWithoutMetadata(serializedEntry.child(FURNACE_TAG));
            }
            if (entry.brewingStand != null) {
                entry.brewingStand.saveWithoutMetadata(serializedEntry.child(BREWING_STAND_TAG));
            }
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        entries.clear();
        bookshelves = Math.max(0, Math.min(15, input.getIntOr(BOOKSHELVES_TAG, 0)));
        HolderLookup.Provider provider = input.lookup();
        for (ValueInput serializedEntry : input.childrenListOrEmpty(ENTRIES_TAG)) {
            WorkstationType type = WorkstationType.byId(serializedEntry.getStringOr(TYPE_TAG, ""));
            if (type == null || entries.containsKey(type)) {
                continue;
            }

            ItemStack stack = serializedEntry.read(STACK_TAG, ItemStack.CODEC).orElse(ItemStack.EMPTY);
            if (!type.matches(stack)) {
                stack = type.defaultStack();
            }
            WorkstationEntry entry = new WorkstationEntry(type, stack.copyWithCount(1));
            if (type == WorkstationType.FURNACE || type == WorkstationType.BLAST_FURNACE || type == WorkstationType.SMOKER) {
                serializedEntry.child(FURNACE_TAG).ifPresent(value -> entry.furnace = createFurnaceFromInput(value, type));
            } else if (type == WorkstationType.BREWING_STAND) {
                serializedEntry.child(BREWING_STAND_TAG).ifPresent(value -> entry.brewingStand = createBrewingStandFromInput(value));
            }
            entries.put(type, entry);
        }
    }

    private static PortableFurnaceAccess createFurnace(ItemStack source, ServerLevel level, WorkstationType type) {
        PortableFurnaceAccess furnace = type == WorkstationType.BLAST_FURNACE
                ? new PortableBlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState())
                : type == WorkstationType.SMOKER
                ? new PortableSmokerBlockEntity(BlockPos.ZERO, Blocks.SMOKER.defaultBlockState())
                : new PortableFurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        furnace.furnaceEntity().setLevel(level);
        TypedEntityData<BlockEntityType<?>> blockEntityData = source.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            furnace.furnaceEntity().loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), blockEntityData.copyTagWithoutId()));
        }
        return furnace;
    }

    private static PortableFurnaceAccess createFurnaceFromInput(ValueInput input, WorkstationType type) {
        PortableFurnaceAccess furnace = type == WorkstationType.BLAST_FURNACE
                ? new PortableBlastFurnaceBlockEntity(BlockPos.ZERO, Blocks.BLAST_FURNACE.defaultBlockState())
                : type == WorkstationType.SMOKER
                ? new PortableSmokerBlockEntity(BlockPos.ZERO, Blocks.SMOKER.defaultBlockState())
                : new PortableFurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
        furnace.furnaceEntity().loadWithComponents(input);
        return furnace;
    }

    private static PortableBrewingStandBlockEntity createBrewingStand(ItemStack source, ServerLevel level) {
        PortableBrewingStandBlockEntity brewingStand = new PortableBrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        brewingStand.setLevel(level);
        TypedEntityData<BlockEntityType<?>> blockEntityData = source.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null) {
            brewingStand.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), blockEntityData.copyTagWithoutId()));
        }
        return brewingStand;
    }

    private static PortableBrewingStandBlockEntity createBrewingStandFromInput(ValueInput input) {
        PortableBrewingStandBlockEntity brewingStand = new PortableBrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        brewingStand.loadWithComponents(input);
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
