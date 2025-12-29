package com.wdab.bettergrindstone.block.entity;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import com.wdab.bettergrindstone.screen.BetterGrindstoneScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BetterGrindstoneBlockEntity extends BlockEntity
        implements Inventory, SidedInventory, ExtendedScreenHandlerFactory<BlockPos> {
    public static final int SLOT_INPUT_TOP = 0;
    public static final int SLOT_INPUT_SIDE = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SIZE = 3;

    private static final int[] SLOTS_UP = new int[] { SLOT_INPUT_TOP };
    private static final int[] SLOTS_SIDE = new int[] { SLOT_INPUT_SIDE };
    private static final int[] SLOTS_DOWN = new int[] { SLOT_OUTPUT };

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    public BetterGrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_BE, pos, state);
    }

    // ---- Step 4 hook: called on rising redstone edge ----
    public void tryGrindOnce() {
        if (world == null || world.isClient())
            return;

        // For Option B, we will:
        // 1) compute the exact vanilla grindstone output for (slot0, slot1)
        // 2) if output is non-empty and slot2 can accept it, place output in slot2
        // 3) consume inputs exactly like "taking" the output in the UI
        //
        // For now: do nothing until we port the vanilla logic.
    }

    // ---- Inventory ----
    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty())
            onInventoryChanged();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        if (!result.isEmpty())
            onInventoryChanged();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }
        onInventoryChanged();
    }

    private void onInventoryChanged() {
        markDirty();
        if (world != null && !world.isClient()) {
            world.updateComparators(pos, getCachedState().getBlock());
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null)
            return false;
        if (world.getBlockEntity(pos) != this)
            return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        items.clear();
        onInventoryChanged();
    }

    // ---- Persistence (1.21.6+) ----
    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, items);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, items);
    }

    // ---- SidedInventory (hoppers) ----
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP)
            return SLOTS_UP;
        if (side == Direction.DOWN)
            return SLOTS_DOWN;
        return SLOTS_SIDE;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        if (slot == SLOT_INPUT_TOP)
            return dir == Direction.UP;
        if (slot == SLOT_INPUT_SIDE)
            return dir != Direction.UP && dir != Direction.DOWN;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT && dir == Direction.DOWN;
    }

    // ---- Extended screen opening (data-based) ----
    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    // ---- Screen ----
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.wdab-better-grindstone.better_grindstone");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BetterGrindstoneScreenHandler(
                syncId,
                playerInventory,
                this,
                ScreenHandlerContext.create(this.world, this.pos));
    }
}
