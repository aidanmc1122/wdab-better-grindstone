package com.wdab.bettergrindstone.screen;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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
    public BetterGrindstoneScreenHandler(int syncId, PlayerInventory playerInv, Inventory inventory,
            ScreenHandlerContext context) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);

        this.inventory = inventory;
        this.context = context;

        // Inputs
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_TOP, 62, 20));
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE, 80, 20));

        // Output
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_OUTPUT, 134, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                if (inventory instanceof BetterGrindstoneBlockEntity be) {
                    World world = player.getEntityWorld();
                    be.onOutputTaken(world);
                }
                super.onTakeItem(player, stack);
            }
        });

        // Player inventory (3 rows)
        int startX = 8;
        int startY = 51;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }

        // Hotbar
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, startX + col * 18, hotbarY));
        }
    }

    /**
     * Factory constructor for ExtendedScreenHandlerType:
     * signature must be (int, PlayerInventory, D) where D is the opening data
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
        // Fallback to a dummy inventory to avoid crashing if something is off
        return new net.minecraft.inventory.SimpleInventory(3);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack())
            return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        newStack = original.copy();

        int blockInvSize = 3;

        if (index < blockInvSize) {
            // from block -> player
            if (!this.insertItem(original, blockInvSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // from player -> block inputs only (0..2 exclusive of output slot 2)
            if (!this.insertItem(original, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return newStack;
    }
}
