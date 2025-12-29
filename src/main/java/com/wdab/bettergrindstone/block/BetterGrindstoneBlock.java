package com.wdab.bettergrindstone.block;

import com.mojang.serialization.MapCodec;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

public class BetterGrindstoneBlock extends BlockWithEntity {
    public static final MapCodec<BetterGrindstoneBlock> CODEC = createCodec(BetterGrindstoneBlock::new);

    public static final BooleanProperty POWERED = BooleanProperty.of("powered");

    public BetterGrindstoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BetterGrindstoneBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    // Open UI
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient())
            return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BetterGrindstoneBlockEntity factory) {
            player.openHandledScreen(factory);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    // Drop inventory on break
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
     * 1.21.11 signature uses WireOrientation (net.minecraft.world.block).
     * We detect rising edge and trigger one grind.
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
            boolean isPoweredNow = world.isReceivingRedstonePower(pos);
            boolean wasPowered = state.get(POWERED);

            // Rising edge: OFF -> ON
            if (!wasPowered && isPoweredNow) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof BetterGrindstoneBlockEntity grindstoneBe) {
                    grindstoneBe.tryGrindOnce();
                }
            }

            // Keep POWERED state synced so we can detect edges
            if (wasPowered != isPoweredNow) {
                world.setBlockState(pos, state.with(POWERED, isPoweredNow), Block.NOTIFY_ALL);
            }
        }

        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        boolean isPoweredNow = world.isReceivingRedstonePower(pos);
        boolean wasPowered = state.get(POWERED);

        if (!wasPowered && isPoweredNow) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BetterGrindstoneBlockEntity grindstoneBe) {
                grindstoneBe.tryGrindOnce();
            }
        }

        if (wasPowered != isPoweredNow) {
            world.setBlockState(pos, state.with(POWERED, isPoweredNow), Block.NOTIFY_ALL);
        }
    }
}
