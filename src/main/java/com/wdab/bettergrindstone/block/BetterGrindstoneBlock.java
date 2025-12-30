package com.wdab.bettergrindstone.block;

import com.mojang.serialization.MapCodec;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

public class BetterGrindstoneBlock extends GrindstoneBlock implements BlockEntityProvider {
    public static final MapCodec<GrindstoneBlock> CODEC = createCodec(BetterGrindstoneBlock::new);

    public static final BooleanProperty POWERED = BooleanProperty.of("powered");

    public BetterGrindstoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    @Override
    public MapCodec<GrindstoneBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BetterGrindstoneBlockEntity(pos, state);
    }

    // Use YOUR UI, not vanilla grindstone UI.
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient())
            return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BetterGrindstoneBlockEntity factory) {
            player.openHandledScreen(factory);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    // Drop BE inventory when broken.
    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BetterGrindstoneBlockEntity invBe) {
            ItemScatterer.spawn(world, pos, invBe);
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    /**
     * 1.21.11 neighborUpdate signature uses WireOrientation.
     * Rising-edge pulse triggers exactly one grind.
     */
    @Override
    protected void neighborUpdate(
            BlockState state,
            World world,
            BlockPos pos,
            Block sourceBlock,
            WireOrientation wireOrientation,
            boolean notify) {

        if (!world.isClient()) {
            boolean now = world.isReceivingRedstonePower(pos);
            boolean was = state.get(POWERED);

            if (!was && now) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BetterGrindstoneBlockEntity grindstoneBe) {
                    grindstoneBe.tryGrindOnce();
                }
            }

            if (was != now) {
                world.setBlockState(pos, state.with(POWERED, now), Block.NOTIFY_ALL);
            }
        }

        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean now = world.isReceivingRedstonePower(pos);
        boolean was = state.get(POWERED);

        if (!was && now) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BetterGrindstoneBlockEntity grindstoneBe) {
                grindstoneBe.tryGrindOnce();
            }
        }

        if (was != now) {
            world.setBlockState(pos, state.with(POWERED, now), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BetterGrindstoneBlockEntity g))
            return 0;

        boolean top = !g.getStack(BetterGrindstoneBlockEntity.SLOT_INPUT_TOP).isEmpty();
        boolean side = !g.getStack(BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE).isEmpty();
        boolean out = !g.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty();

        boolean hasAnyInput = top || side;

        // ✅ If there are no inputs, it cannot be triggered -> no signal,
        // even if an output item is sitting there.
        if (!hasAnyInput)
            return 0;

        // ✅ Ready/triggerable when output exists while inputs are present
        if (out)
            return 15;

        // Loaded but not ready (e.g., only a damaged non-enchanted item)
        return 8;
    }
}
