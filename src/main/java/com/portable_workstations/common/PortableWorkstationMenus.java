package com.portable_workstations.common;

import com.portable_workstations.network.PortableWorkstationsNetwork;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class PortableWorkstationMenus {
    private PortableWorkstationMenus() {
    }

    public static final class PortableFurnaceMenu extends FurnaceMenu {
        public PortableFurnaceMenu(int id, Inventory inventory, PortableFurnaceAccess furnace) {
            super(id, inventory, furnace.furnaceEntity(), furnace.portableDataAccess());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableBlastFurnaceMenu extends BlastFurnaceMenu {
        public PortableBlastFurnaceMenu(int id, Inventory inventory, PortableFurnaceAccess furnace) {
            super(id, inventory, furnace.furnaceEntity(), furnace.portableDataAccess());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableBrewingStandMenu extends BrewingStandMenu {
        public PortableBrewingStandMenu(int id, Inventory inventory, PortableBrewingStandBlockEntity brewingStand) {
            super(id, inventory, brewingStand, brewingStand.portableDataAccess());
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableCraftingMenu extends CraftingMenu {
        public PortableCraftingMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableFletchingMenu extends CraftingMenu {
        public PortableFletchingMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableAnvilMenu extends AnvilMenu {
        private final PortableWorkstationsData data;

        public PortableAnvilMenu(int id, Inventory inventory, PortableWorkstationsData data) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
            this.data = data;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        protected void onTake(Player player, ItemStack result) {
            super.onTake(player, result);
            float breakChance = 0.12F;
            if (!player.getAbilities().instabuild && player.getRandom().nextFloat() < breakChance) {
                boolean remains = data.damageAnvil();
                PortableWorkstationsNetwork.sync((ServerPlayer) player);
                if (!remains) {
                    ((ServerPlayer) player).closeContainer();
                }
            }
        }
    }

    public static final class PortableEnchantmentMenu extends EnchantmentMenu {
        private final PortableWorkstationsData data;
        private final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
        private final Player player;

        public PortableEnchantmentMenu(int id, Inventory inventory, PortableWorkstationsData data) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
            this.data = data;
            this.player = inventory.player;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void slotsChanged(Container container) {
            if (this.player.level().isClientSide) {
                return;
            }
            ItemStack item = this.getSlot(0).getItem();
            if (!item.isEmpty() && item.isEnchantable()) {
                int bookshelfPower = data.bookshelves();
                random.setSeed(this.getEnchantmentSeed());
                for (int index = 0; index < 3; index++) {
                    this.costs[index] = EnchantmentHelper.getEnchantmentCost(random, index, bookshelfPower, item);
                    this.enchantClue[index] = -1;
                    this.levelClue[index] = -1;
                    if (this.costs[index] < index + 1) {
                        this.costs[index] = 0;
                    }
                }

                for (int index = 0; index < 3; index++) {
                    if (this.costs[index] > 0) {
                        List<EnchantmentInstance> enchantments = getEnchantmentList(item, index, this.costs[index]);
                        if (!enchantments.isEmpty()) {
                            EnchantmentInstance enchantment = enchantments.get(random.nextInt(enchantments.size()));
                            Registry<net.minecraft.world.item.enchantment.Enchantment> registry = this.player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                            this.enchantClue[index] = registry.getId(enchantment.enchantment.value());
                            this.levelClue[index] = enchantment.level;
                        }
                    }
                }
            } else {
                for (int index = 0; index < 3; index++) {
                    this.costs[index] = 0;
                    this.enchantClue[index] = -1;
                    this.levelClue[index] = -1;
                }
            }
            this.broadcastChanges();
        }

        @Override
        public boolean clickMenuButton(Player player, int button) {
            if (button < 0 || button >= this.costs.length) {
                Util.logAndPauseIfInIde(player.getName() + " pressed invalid button id: " + button);
                return false;
            }

            ItemStack item = this.getSlot(0).getItem();
            ItemStack lapis = this.getSlot(1).getItem();
            int levelCost = button + 1;
            if ((lapis.isEmpty() || lapis.getCount() < levelCost) && !player.getAbilities().instabuild) {
                return false;
            }
            if (this.costs[button] <= 0 || item.isEmpty() || (player.experienceLevel < levelCost || player.experienceLevel < this.costs[button]) && !player.getAbilities().instabuild) {
                return false;
            }

            List<EnchantmentInstance> enchantments = getEnchantmentList(item, button, this.costs[button]);
            if (enchantments.isEmpty()) {
                return false;
            }

            player.onEnchantmentPerformed(item, levelCost);
            boolean isBook = item.is(Items.BOOK);
            ItemStack enchanted = item;
            if (isBook) {
                enchanted = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                for (EnchantmentInstance enchantment : enchantments) {
                    stored.set(enchantment.enchantment, enchantment.level);
                }
                enchanted.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                this.getSlot(0).set(enchanted);
            }
            for (EnchantmentInstance enchantment : enchantments) {
                if (!isBook) {
                    enchanted.enchant(enchantment.enchantment, enchantment.level);
                }
            }

            if (!player.getAbilities().instabuild) {
                lapis.shrink(levelCost);
                this.getSlot(1).set(lapis.isEmpty() ? ItemStack.EMPTY : lapis);
            }
            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                net.minecraft.advancements.CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, enchanted, levelCost);
            }
            this.setData(3, player.getEnchantmentSeed());
            this.getSlot(0).setChanged();
            slotsChanged(null);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, player.getRandom().nextFloat() * 0.1F + 0.9F);
            return true;
        }

        @Override
        public int getGoldCount() {
            ItemStack lapis = this.getSlot(1).getItem();
            return lapis.isEmpty() ? 0 : lapis.getCount();
        }

        private List<EnchantmentInstance> getEnchantmentList(ItemStack item, int slot, int cost) {
            random.setSeed((long) (this.getEnchantmentSeed() + slot));
            Registry<net.minecraft.world.item.enchantment.Enchantment> registry = this.player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            List<EnchantmentInstance> enchantments = EnchantmentHelper.selectEnchantment(random, item, cost, registry.holders().map(holder -> (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment>) holder));
            if (item.is(Items.BOOK) && enchantments.size() > 1) {
                enchantments.remove(random.nextInt(enchantments.size()));
            }
            return enchantments;
        }
    }

    public static final class PortableStonecutterMenu extends StonecutterMenu {
        public PortableStonecutterMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableGrindstoneMenu extends GrindstoneMenu {
        public PortableGrindstoneMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableLoomMenu extends LoomMenu {
        public PortableLoomMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableSmithingMenu extends SmithingMenu {
        public PortableSmithingMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }

    public static final class PortableCartographyMenu extends CartographyTableMenu {
        public PortableCartographyMenu(int id, Inventory inventory) {
            super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
