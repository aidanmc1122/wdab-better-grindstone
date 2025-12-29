package com.wdab.bettergrindstone.screen;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;

import net.minecraft.enchantment.EnchantmentHelper;
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

    public BetterGrindstoneScreenHandler(int syncId, PlayerInventory playerInv, Inventory inventory,
            ScreenHandlerContext context) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);

        this.inventory = inventory;
        this.context = context;

        // Input 0
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_TOP, 62, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                if (!(inventory instanceof BetterGrindstoneBlockEntity be))
                    return false;
                return BetterGrindstoneBlockEntity.isValidGrindInput(stack)
                        && be.isCompatibleWithOtherSlot(BetterGrindstoneBlockEntity.SLOT_INPUT_TOP, stack)
                        && be.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty();
            }
        });

        // Input 1
        this.addSlot(new Slot(inventory, BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE, 80, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                if (!(inventory instanceof BetterGrindstoneBlockEntity be))
                    return false;
                return BetterGrindstoneBlockEntity.isValidGrindInput(stack)
                        && be.isCompatibleWithOtherSlot(BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE, stack)
                        && be.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty();
            }
        });

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
                    // IMPORTANT: use the taken stack, NOT be.getStack(OUTPUT) (it may be empty
                    // already).
                    be.onOutputTakenByPlayer(world, stack);
                }
                super.onTakeItem(player, stack);
            }
        });

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

    // ExtendedScreenHandlerType factory constructor
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
            // from block -> player
            if (!this.insertItem(original, blockInvSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // from player -> block inputs only
            if (!this.insertItem(original, 0, 2, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        // Ensure output slot hooks fire even on shift-click
        slot.onTakeItem(player, original);

        return copy;
    }
}
