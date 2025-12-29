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
import net.minecraft.screen.AnvilScreenHandler; // ✅ FIX
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

    public static final int SLOT_INPUT_TOP = 0;
    public static final int SLOT_INPUT_SIDE = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SIZE = 3;

    private static final int[] SLOTS_UP = new int[] { SLOT_INPUT_TOP };
    private static final int[] SLOTS_SIDE = new int[] { SLOT_INPUT_SIDE };
    private static final int[] SLOTS_DOWN = new int[] { SLOT_OUTPUT };

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    public BetterGrindstoneBlockEntity(BlockPos pos, net.minecraft.block.BlockState state) {
        super(WDABBetterGrindstone.BETTER_GRINDSTONE_BE, pos, state);
    }

    // -------------------------------------------------------------------------
    // 4.2: Vanilla grindstone logic (ported from GrindstoneScreenHandler)
    // -------------------------------------------------------------------------

    private void recomputeOutputVanilla() {
        // Only compute/refresh if output is empty.
        // If output is non-empty, treat it as "ready/locked" until extracted.
        if (!getStack(SLOT_OUTPUT).isEmpty()) {
            return;
        }

        ItemStack out = getOutputStack(getStack(SLOT_INPUT_TOP), getStack(SLOT_INPUT_SIDE));
        if (!ItemStack.areEqual(out, getStack(SLOT_OUTPUT))) {
            setStackInternal(SLOT_OUTPUT, out);
            onInventoryChanged();
        }
    }

    public void onOutputTaken(World world) {
        if (world.isClient())
            return;

        if (getStack(SLOT_INPUT_TOP).isEmpty() && getStack(SLOT_INPUT_SIDE).isEmpty()) {
            return;
        }

        spawnXpAndEffects(world);
        setStackInternal(SLOT_INPUT_TOP, ItemStack.EMPTY);
        setStackInternal(SLOT_INPUT_SIDE, ItemStack.EMPTY);

        onInventoryChanged();
    }

    public void tryGrindOnce() {
        if (world == null || world.isClient())
            return;

        recomputeOutputVanilla();
        if (getStack(SLOT_OUTPUT).isEmpty())
            return;

        if (getStack(SLOT_INPUT_TOP).isEmpty() && getStack(SLOT_INPUT_SIDE).isEmpty())
            return;

        spawnXpAndEffects(world);
        setStackInternal(SLOT_INPUT_TOP, ItemStack.EMPTY);
        setStackInternal(SLOT_INPUT_SIDE, ItemStack.EMPTY);
        onInventoryChanged();
    }

    private void spawnXpAndEffects(World world) {
        if (world instanceof ServerWorld serverWorld) {
            int xp = getExperience(serverWorld);
            if (xp > 0) {
                ExperienceOrbEntity.spawn(serverWorld, Vec3d.ofCenter(this.pos), xp);
            }
        }
        world.syncWorldEvent(WorldEvents.GRINDSTONE_USED, this.pos, 0);
    }

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

    private ItemStack getOutputStack(ItemStack firstInput, ItemStack secondInput) {
        boolean hasAny = !firstInput.isEmpty() || !secondInput.isEmpty();
        if (!hasAny) {
            return ItemStack.EMPTY;
        } else if (firstInput.getCount() <= 1 && secondInput.getCount() <= 1) {
            boolean both = !firstInput.isEmpty() && !secondInput.isEmpty();
            if (!both) {
                ItemStack stack = !firstInput.isEmpty() ? firstInput : secondInput;
                return !EnchantmentHelper.hasEnchantments(stack) ? ItemStack.EMPTY : grind(stack.copy());
            } else {
                return combineItems(firstInput, secondInput);
            }
        } else {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack combineItems(ItemStack firstInput, ItemStack secondInput) {
        if (!firstInput.isOf(secondInput.getItem())) {
            return ItemStack.EMPTY;
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

    // -------------------------------------------------------------------------
    // Inventory implementation
    // -------------------------------------------------------------------------

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
        if (slot == SLOT_OUTPUT) {
            ItemStack full = items.get(SLOT_OUTPUT);
            if (full.isEmpty())
                return ItemStack.EMPTY;

            ItemStack result = full.copy();
            setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);

            if (world != null && !world.isClient()) {
                onOutputTaken(world);
            }

            onInventoryChanged();
            return result;
        }

        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
                setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);
                recomputeOutputVanilla();
            }
            onInventoryChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot == SLOT_OUTPUT) {
            ItemStack full = items.get(SLOT_OUTPUT);
            if (full.isEmpty())
                return ItemStack.EMPTY;

            ItemStack result = full.copy();
            setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);

            if (world != null && !world.isClient()) {
                onOutputTaken(world);
            }

            onInventoryChanged();
            return result;
        }

        ItemStack result = Inventories.removeStack(items, slot);
        if (!result.isEmpty()) {
            if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
                setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);
                recomputeOutputVanilla();
            }
            onInventoryChanged();
        }
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }

        if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
            setStackInternal(SLOT_OUTPUT, ItemStack.EMPTY);
            recomputeOutputVanilla();
        }

        onInventoryChanged();
    }

    private void setStackInternal(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) {
            stack.setCount(stack.getMaxCount());
        }
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

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Hopper rules
    // -------------------------------------------------------------------------

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
        if (slot == SLOT_INPUT_TOP || slot == SLOT_INPUT_SIDE) {
            if (!getStack(SLOT_OUTPUT).isEmpty())
                return false;
        }

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

    // -------------------------------------------------------------------------
    // Screen opening
    // -------------------------------------------------------------------------

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

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
