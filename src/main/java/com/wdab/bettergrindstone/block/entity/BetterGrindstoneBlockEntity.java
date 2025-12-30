package com.wdab.bettergrindstone.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import com.wdab.bettergrindstone.screen.BetterGrindstoneScreenHandler;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class BetterGrindstoneBlockEntity extends net.minecraft.block.entity.BlockEntity
        implements Inventory, SidedInventory, ExtendedScreenHandlerFactory<BlockPos> {

    // Slot indices
    public static final int SLOT_INPUT_TOP = 0;
    public static final int SLOT_INPUT_SIDE = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SIZE = 3;

    // Sided inventory slot arrays
    private static final int[] SLOTS_UP = new int[] { SLOT_INPUT_TOP };
    private static final int[] SLOTS_SIDE = new int[] { SLOT_INPUT_SIDE };
    private static final int[] SLOTS_DOWN = new int[] { SLOT_OUTPUT };

    // Internal inventory
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    // Track if output has been finalized (i.e., grinding completed)
    private boolean outputFinalized = false;

    // Constructor
    public BetterGrindstoneBlockEntity(BlockPos pos, net.minecraft.block.BlockState state) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_BE, pos, state);
    }

    // Check if an item stack is a valid grind input
    public static boolean isValidGrindInput(ItemStack stack) {
        return !stack.isEmpty() && (stack.isDamageable() || EnchantmentHelper.hasEnchantments(stack));
    }

    // Check if the candidate item stack can be placed in the specified slot
    public boolean isCompatibleWithOtherSlot(int slot, ItemStack candidate) {
        // Check if candidate is valid grind input
        if (!isValidGrindInput(candidate))
            return false;

        // Check compatibility with the other slot
        ItemStack other = (slot == SLOT_INPUT_TOP) ? getStack(SLOT_INPUT_SIDE) : getStack(SLOT_INPUT_TOP);
        if (other.isEmpty())
            return true;

        // Determine if grinding would produce an output
        ItemStack out = getOutputStack(
                slot == SLOT_INPUT_TOP ? candidate : other,
                slot == SLOT_INPUT_TOP ? other : candidate);
        return !out.isEmpty();
    }

    // Recompute the preview output if needed
    private void recomputePreviewOutputIfNeeded() {
        // Only compute if output slot is empty
        if (!getStack(SLOT_OUTPUT).isEmpty())
            return;

        // Compute new output based on current inputs
        ItemStack out = getOutputStack(getStack(SLOT_INPUT_TOP), getStack(SLOT_INPUT_SIDE));
        if (!out.isEmpty()) {
            setStackInternal(SLOT_OUTPUT, out);
            outputFinalized = false;
            onInventoryChanged();
        }
    }

    // Attempt to grind once when triggered
    public void tryGrindOnce() {
        if (world == null || world.isClient())
            return;

        // Ensure output is computed
        if (getStack(SLOT_OUTPUT).isEmpty()) {
            recomputePreviewOutputIfNeeded();
        }

        // If there's no output or no inputs, do nothing
        if (getStack(SLOT_OUTPUT).isEmpty())
            return;

        // Finalize grinding process
        if (getStack(SLOT_INPUT_TOP).isEmpty() && getStack(SLOT_INPUT_SIDE).isEmpty())
            return;

        // Mark output as finalized
        outputFinalized = true;

        // Spawn experience orbs and effects
        spawnXpAndEffects(world);

        // Clear input slots
        setStackInternal(SLOT_INPUT_TOP, ItemStack.EMPTY);
        setStackInternal(SLOT_INPUT_SIDE, ItemStack.EMPTY);

        // Notify inventory change
        onInventoryChanged();
    }

    // Handle when the output is taken by the player
    public void onOutputTakenByPlayer(World world, ItemStack takenOutput) {
        if (world.isClient())
            return;

        // If no output was taken, do nothing
        if (takenOutput == null || takenOutput.isEmpty())
            return;

        // If there's no output or no inputs, do nothing
        if (getStack(SLOT_INPUT_TOP).isEmpty() && getStack(SLOT_INPUT_SIDE).isEmpty())
            return;

        // Mark output as finalized
        outputFinalized = true;

        // Spawn experience orbs and effects
        spawnXpAndEffects(world);

        // Clear input slots
        setStackInternal(SLOT_INPUT_TOP, ItemStack.EMPTY);
        setStackInternal(SLOT_INPUT_SIDE, ItemStack.EMPTY);

        // Notify inventory change
        onInventoryChanged();
    }

    // Spawn experience orbs and play grindstone effects
    private void spawnXpAndEffects(World world) {
        if (world instanceof ServerWorld serverWorld) {
            int xp = getExperience(serverWorld);
            if (xp > 0) {
                ExperienceOrbEntity.spawn(serverWorld, Vec3d.ofCenter(this.pos), xp);
            }
        }
        world.syncWorldEvent(WorldEvents.GRINDSTONE_USED, this.pos, 0);
    }

    // Calculate total experience to award based on inputs
    private int getExperience(World world) {
        int i = 0;
        i += getExperience(getStack(SLOT_INPUT_TOP));
        i += getExperience(getStack(SLOT_INPUT_SIDE));
        if (i > 0) {
            int j = (int) Math.ceil(i / 2.0);
            return j + world.random.nextInt(j);
        } else {
            return 0;
        }
    }

    // Calculate experience from a single item stack
    private int getExperience(ItemStack stack) {
        int i = 0;
        ItemEnchantmentsComponent ench = EnchantmentHelper.getEnchantments(stack);

        for (Entry<RegistryEntry<Enchantment>> entry : ench.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> registryEntry = (RegistryEntry<Enchantment>) entry.getKey();
            int level = entry.getIntValue();
            if (!registryEntry.isIn(EnchantmentTags.CURSE)) {
                i += registryEntry.value().getMinPower(level);
            }
        }
        return i;
    }

    // Determine the output stack based on the two input stacks
    private ItemStack getOutputStack(ItemStack firstInput, ItemStack secondInput) {
        // Check if there are any inputs
        boolean hasAny = !firstInput.isEmpty() || !secondInput.isEmpty();

        // If no inputs, return empty
        if (!hasAny) {
            return ItemStack.EMPTY;

        // If only one item with count 1, grind it
        } else if (firstInput.getCount() <= 1 && secondInput.getCount() <= 1) {
            boolean both = !firstInput.isEmpty() && !secondInput.isEmpty();
            if (!both) {
                ItemStack stack = !firstInput.isEmpty() ? firstInput : secondInput;
                return !EnchantmentHelper.hasEnchantments(stack) ? ItemStack.EMPTY : grind(stack.copy());
            } else {
                return combineItems(firstInput, secondInput);
            }

        // If one item has count > 1, cannot grind
        } else {
            return ItemStack.EMPTY;
        }
    }

    // Combine two items into one ground item
    private ItemStack combineItems(ItemStack firstInput, ItemStack secondInput) {
        // If items are not of the same type, cannot combine
        if (!firstInput.isOf(secondInput.getItem())) {
            return ItemStack.EMPTY;
            
        // If both items are damageable, combine their durability
        } else {
            int i = Math.max(firstInput.getMaxDamage(), secondInput.getMaxDamage());
            int j = firstInput.getMaxDamage() - firstInput.getDamage();
            int k = secondInput.getMaxDamage() - secondInput.getDamage();
            int l = j + k + i * 5 / 100;

            int m = 1;
            if (!firstInput.isDamageable()) {
                if (firstInput.getMaxCount() < 2 || !ItemStack.areEqual(firstInput, secondInput)) {
                    return ItemStack.EMPTY;
                }
                m = 2;
            }

            ItemStack itemStack = firstInput.copyWithCount(m);
            if (itemStack.isDamageable()) {
                itemStack.set(DataComponentTypes.MAX_DAMAGE, i);
                itemStack.setDamage(Math.max(i - l, 0));
            }

            transferEnchantments(itemStack, secondInput);
            return grind(itemStack);
        }
    }

    // Transfer enchantments from source to target, preserving curses
    private void transferEnchantments(ItemStack target, ItemStack source) {
        EnchantmentHelper.apply(target, components -> {
            ItemEnchantmentsComponent sourceEnch = EnchantmentHelper.getEnchantments(source);

            for (Entry<RegistryEntry<Enchantment>> entry : sourceEnch.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> ench = (RegistryEntry<Enchantment>) entry.getKey();
                if (!ench.isIn(EnchantmentTags.CURSE) || components.getLevel(ench) == 0) {
                    components.add(ench, entry.getIntValue());
                }
            }
        });
    }

    // Grind an item stack by removing non-curse enchantments and adjusting repair cost
    private ItemStack grind(ItemStack item) {
        ItemEnchantmentsComponent kept = EnchantmentHelper.apply(
                item, components -> components.remove(enchantment -> !enchantment.isIn(EnchantmentTags.CURSE)));

        if (item.isOf(Items.ENCHANTED_BOOK) && kept.isEmpty()) {
            item = item.withItem(Items.BOOK);
        }

        int cost = 0;
        for (int j = 0; j < kept.getSize(); j++) {
            cost = AnvilScreenHandler.getNextCost(cost);
        }

        item.set(DataComponentTypes.REPAIR_COST, cost);
        return item;
    }

    // Get the size of the inventory
    @Override
    public int size() {
        return items.size();
    }

    // Check if the inventory is empty
    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    // Get the item stack in the specified slot
    @Override
    public ItemStack getStack(int slot) {
        if (slot == SLOT_OUTPUT && suppressPreviewOutputDropOnce && !outputFinalized) {
            return ItemStack.EMPTY;
        }
        return items.get(slot);
    }

    // Remove a specified amount of items from a slot
    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
                setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);
                outputFinalized = false;
                recomputePreviewOutputIfNeeded();
            }
            onInventoryChanged();
        }
        return result;
    }

    // Remove the entire stack from a slot
    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        if (!result.isEmpty()) {
            if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
                setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);
                outputFinalized = false;
                recomputePreviewOutputIfNeeded();
            }
            onInventoryChanged();
        }
        return result;
    }

    // Set the item stack in the specified slot
    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount())
            stack.setCount(stack.getMaxCount());

        if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
            setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);
            outputFinalized = false;
            recomputePreviewOutputIfNeeded();
        }

        onInventoryChanged();
    }

    // Internal method to set stack without triggering recompute
    private void setStackInternal(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount())
            stack.setCount(stack.getMaxCount());
    }

    // Handle inventory changes
    private void onInventoryChanged() {
        markDirty();
        if (world != null && !world.isClient()) {
            world.updateComparators(pos, getCachedState().getBlock());
        }
    }

    // Check if a player can use this block entity
    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null)
            return false;
        if (world.getBlockEntity(pos) != this)
            return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    // Clear the inventory
    @Override
    public void clear() {
        items.clear();
        outputFinalized = false;
        onInventoryChanged();
    }

    // Write the block entity data to storage
    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, items);
        view.putBoolean("OutputFinalized", outputFinalized);
    }

    // Read the block entity data from storage
    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, items);
        outputFinalized = view.getBoolean("OutputFinalized", false);
        if (items.get(SLOT_OUTPUT).isEmpty()) {
            outputFinalized = false;
        }
    }

    // Get available slots for a given side
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP)
            return SLOTS_UP;
        if (side == Direction.DOWN)
            return SLOTS_DOWN;
        return SLOTS_SIDE;
    }

    // Determine if an item stack can be inserted into a slot from a given direction
    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        if (slot != SLOT_INPUT_TOP && slot != SLOT_INPUT_SIDE)
            return false;

        if (!getStack(SLOT_OUTPUT).isEmpty())
            return false;

        if (!isValidGrindInput(stack))
            return false;

        if (!isCompatibleWithOtherSlot(slot, stack))
            return false;

        if (slot == SLOT_INPUT_TOP)
            return dir == Direction.UP;
        if (slot == SLOT_INPUT_SIDE)
            return dir != Direction.UP && dir != Direction.DOWN;
        return false;
    }

    // Determine if an item stack can be extracted from a slot from a given direction
    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT && dir == Direction.DOWN && outputFinalized;
    }

    // Get the position data for opening the screen
    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    // Get the display name for the block entity
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.wdab-better-grindstone.better_grindstone");
    }

    // Create the screen handler for the block entity
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BetterGrindstoneScreenHandler(
                syncId,
                playerInventory,
                this,
                ScreenHandlerContext.create(this.world, this.pos));
    }

    // Flag to suppress preview output drop once
    private boolean suppressPreviewOutputDropOnce = false;

    // Suppress preview output drop once
    public void suppressPreviewOutputDropOnce() {
        if (!outputFinalized) {
            this.suppressPreviewOutputDropOnce = true;
        }
    }
}
