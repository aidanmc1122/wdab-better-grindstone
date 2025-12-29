package com.wdab.bettergrindstone.screen;

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

import static com.wdab.bettergrindstone.WDABBetterGrindstone.BETTER_GRINDSTONE_SCREEN_HANDLER;

public class BetterGrindstoneScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    @SuppressWarnings("unused")
    private final ScreenHandlerContext context;

    // Server-side constructor (BlockEntity calls this)
    public BetterGrindstoneScreenHandler(int syncId, PlayerInventory playerInv, Inventory inventory,
            ScreenHandlerContext context) {
        super(BETTER_GRINDSTONE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);

        this.inventory = inventory;
        this.context = context;

        // Block slots (3)
        this.addSlot(new Slot(inventory, 0, 62, 20)); // input top
        this.addSlot(new Slot(inventory, 1, 80, 20)); // input side
        this.addSlot(new Slot(inventory, 2, 134, 20)); // output

        // Player inventory
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

    // Client-side constructor (ExtendedScreenHandlerType uses this shape in your
    // mappings)
    public BetterGrindstoneScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(
                syncId,
                playerInv,
                readInventory(playerInv, pos),
                ScreenHandlerContext.EMPTY);
    }

    private static Inventory readInventory(PlayerInventory playerInv, BlockPos pos) {
        World world = playerInv.player.getEntityWorld(); // <-- your mappings do NOT have getWorld()
        if (world.getBlockEntity(pos) instanceof BetterGrindstoneBlockEntity be) {
            return be;
        }
        // Fallback: prevents client crash if desynced
        return new SimpleInventory(3);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack())
            return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        ItemStack copy = original.copy();

        int blockInvSize = 3;

        if (index < blockInvSize) {
            // Block -> Player
            if (!this.insertItem(original, blockInvSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player -> Block (inputs only: slots 0..1)
            if (!this.insertItem(original, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return copy;
    }
}
