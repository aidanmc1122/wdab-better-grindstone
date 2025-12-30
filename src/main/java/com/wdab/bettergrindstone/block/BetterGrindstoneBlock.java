package com.wdab.bettergrindstone.block;

import com.mojang.serialization.MapCodec;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.GrindstoneBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

public class BetterGrindstoneBlock extends GrindstoneBlock implements BlockEntityProvider {
    // Codec for serialization/deserialization
    public static final MapCodec<GrindstoneBlock> CODEC = createCodec(BetterGrindstoneBlock::new);

    // Property to track powered state
    public static final BooleanProperty POWERED = BooleanProperty.of("powered");

    // Constructor
    public BetterGrindstoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    // Get the codec for this block
    @Override
    public MapCodec<GrindstoneBlock> getCodec() {
        return CODEC;
    }

    // Append properties to the block state
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    // Create the block entity for this block
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BetterGrindstoneBlockEntity(pos, state);
    }

    // Handle block interaction
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

    // Handle block state replacement
    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BetterGrindstoneBlockEntity g) {
            g.suppressPreviewOutputDropOnce();
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    // Handle block breaking
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BetterGrindstoneBlockEntity g) {
                g.suppressPreviewOutputDropOnce();
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    // Handle neighbor updates (redstone power changes)
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

    // Scheduled tick to check redstone power changes
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

    // Determine if the block has comparator output
    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    // Get the comparator output level based on the block entity's inventory
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BetterGrindstoneBlockEntity g))
            return 0;

        boolean top = !g.getStack(BetterGrindstoneBlockEntity.SLOT_INPUT_TOP).isEmpty();
        boolean side = !g.getStack(BetterGrindstoneBlockEntity.SLOT_INPUT_SIDE).isEmpty();
        boolean out = !g.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty();

        boolean hasAnyInput = top || side;

        // No input items
        if (!hasAnyInput)
            return 0;

        // Output item present and at least one input item
        if (out)
            return 15;

        // One input item present
        return 8;
    }
}
