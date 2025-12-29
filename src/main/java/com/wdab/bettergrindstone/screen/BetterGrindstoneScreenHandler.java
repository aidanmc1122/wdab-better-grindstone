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
    private final Inventory inventory;
    private final ScreenHandlerContext context;

    // Server-side constructor (BlockEntity passes its inventory + context)
    public BetterGrindstoneScreenHandler(
            int syncId,
            PlayerInventory playerInv,
            Inventory inventory,
            ScreenHandlerContext context) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_SCREEN_HANDLER, syncId);
        checkSize(inventory, BetterGrindstoneBlockEntity.SIZE);

        this.inventory = inventory;
        this.context = context;

        // Inputs (match your texture slot positions)
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_TOP, 49, 19) {
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

        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE, 49, 40) {
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

        // Output
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_OUTPUT, 129, 34) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                // ✅ Call the method that exists in your BE
                if (inventory instanceof BetterGrindstoneBlockEntity be) {
                    World world = player.getEntityWorld();
                    be.onOutputTakenByPlayer(world, stack);
                }
                super.onTakeItem(player, stack);
            }
        });

        // Player inventory (vanilla-ish placement; adjust if your texture differs)
        this.addPlayerSlots(playerInv, 8, 84);
    }

    /**
     * Factory constructor for ExtendedScreenHandlerType:
     * signature must be (int, PlayerInventory, D) where D is opening data
     * (BlockPos).
     */
    public BetterGrindstoneScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(
                syncId,
                playerInv,
                getInventoryAt(playerInv, pos),
                ScreenHandlerContext.create(playerInv.player.getEntityWorld(), pos));
    }

    private static Inventory getInventoryAt(PlayerInventory playerInv, BlockPos pos) {
        World world = playerInv.player.getEntityWorld();
        if (world.getBlockEntity(pos) instanceof BetterGrindstoneBlockEntity be) {
            return be;
        }
        return new SimpleInventory(BetterGrindstoneBlockEntity.SIZE);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getStack();
        newStack = original.copy();

        int blockInvSize = BetterGrindstoneBlockEntity.SIZE;

        if (index < blockInvSize) {
            // from block -> player
            if (!this.insertItem(original, blockInvSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // from player -> block inputs only (0..2 exclusive of output slot)
            if (!this.insertItem(original, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (original.getCount() == newStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTakeItem(player, original);
        return newStack;
    }
}
