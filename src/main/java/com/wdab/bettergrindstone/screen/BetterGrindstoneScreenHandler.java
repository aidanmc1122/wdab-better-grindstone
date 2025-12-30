package com.wdab.bettergrindstone.screen;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BetterGrindstoneScreenHandler extends ScreenHandler {
    // Inventory and context for the grindstone
    private final Inventory inventory;
    private final ScreenHandlerContext context;

    // Constructor
    public BetterGrindstoneScreenHandler(
            int syncId,
            PlayerInventory playerInv,
            Inventory inventory,
            ScreenHandlerContext context) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_SCREEN_HANDLER, syncId);
        checkSize(inventory, BetterGrindstoneBlockEntity.SIZE);

        // Initialize inventory and context
        this.inventory = inventory;
        this.context = context;

        // Add top input slot
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_TOP, 49, 19) {
            // Override to validate item insertion
            @Override
            public boolean canInsert(ItemStack stack) {
                if (!(inventory instanceof BetterGrindstoneBlockEntity be)) {
                    return false;
                }
                return BetterGrindstoneBlockEntity.isValidGrindInput(stack)
                        && be.isCompatibleWithOtherSlot(BetterGrindstoneBlockEntity.SLOT_INPUT_TOP, stack)
                        && be.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty();
            }
        });

        // Add side input slot
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE, 49, 40) {
            // Override to validate item insertion
            @Override
            public boolean canInsert(ItemStack stack) {
                if (!(inventory instanceof BetterGrindstoneBlockEntity be)) {
                    return false;
                }
                return BetterGrindstoneBlockEntity.isValidGrindInput(stack)
                        && be.isCompatibleWithOtherSlot(BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE, stack)
                        && be.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty();
            }
        });

        // Add output slot
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_OUTPUT, 129, 34) {
            // Override to prevent item insertion
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            // Override to handle output item taken by player
            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                if (inventory instanceof BetterGrindstoneBlockEntity be) {
                    World world = player.getEntityWorld();
                    be.onOutputTakenByPlayer(world, stack);
                }
                super.onTakeItem(player, stack);
            }
        });

        // Add player inventory slots
        this.addPlayerSlots(playerInv, 8, 84);
    }

    // Helper method to add player inventory slots
    public BetterGrindstoneScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(
                syncId,
                playerInv,
                getInventoryAt(playerInv, pos),
                ScreenHandlerContext.create(playerInv.player.getEntityWorld(), pos));
    }

    // Helper method to get the inventory at a specific position
    private static Inventory getInventoryAt(PlayerInventory playerInv, BlockPos pos) {
        World world = playerInv.player.getEntityWorld();
        if (world.getBlockEntity(pos) instanceof BetterGrindstoneBlockEntity be) {
            return be;
        }
        return new SimpleInventory(BetterGrindstoneBlockEntity.SIZE);
    }

    // Add player inventory and hotbar slots
    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    // Handle shift-click item transfers
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        // Check if the slot has an item
        if (slot == null || !slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        // Get the original stack
        ItemStack original = slot.getStack();

        // Copy the original stack for return value
        newStack = original.copy();

        // Determine the size of the block inventory
        int blockInvSize = BetterGrindstoneBlockEntity.SIZE;

        // Handle item transfer between block inventory and player inventory
        boolean moved;
        if (index < blockInvSize) {
            moved = this.insertItem(original, blockInvSize, this.slots.size(), true);
            // Attempt to move item from block inventory to player inventory
            if (!moved)
                return ItemStack.EMPTY;

            // Notify block entity if output slot item is taken
            if (index == BetterGrindstoneBlockEntity.SLOT_OUTPUT
                    && this.inventory instanceof BetterGrindstoneBlockEntity be) {
                be.onOutputTakenByPlayer(player.getEntityWorld(), newStack);
            }
        } else {
            // Attempt to move item from player inventory to block inventory
            moved = this.insertItem(original, 0, 2, false);
            if (!moved)
                return ItemStack.EMPTY;
        }

        // Update the slot based on the transfer result
        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        // Check if the item count has changed after transfer
        if (original.getCount() == newStack.getCount()) {
            return ItemStack.EMPTY;
        }

        // Notify the slot that an item has been taken
        slot.onTakeItem(player, original);
        return newStack;
    }
}
