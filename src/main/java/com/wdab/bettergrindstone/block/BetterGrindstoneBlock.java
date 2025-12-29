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
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BetterGrindstoneBlock extends BlockWithEntity {
    public static final MapCodec<BetterGrindstoneBlock> CODEC = createCodec(BetterGrindstoneBlock::new);

    public static final BooleanProperty POWERED = BooleanProperty.of("powered");

    public BetterGrindstoneBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(POWERED, false));
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

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BetterGrindstoneBlockEntity invBe) {
            ItemScatterer.spawn(world, pos, invBe);
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    // ---- Comparator output (your 1.21.11 signature) ----
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof BetterGrindstoneBlockEntity grindstoneBe) {
            return grindstoneBe.getStack(BetterGrindstoneBlockEntity.SLOT_OUTPUT).isEmpty() ? 0 : 15;
        }
        return 0;
    }

    // ---- Redstone pulse activation ----
    // NOTE: In your mappings, the super method name/signature differs, so we do NOT
    // @Override.
    // Minecraft will still call this method if it matches the actual runtime
    // signature.
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos,
            boolean notify) {
        if (world.isClient())
            return;

        boolean nowPowered = world.isReceivingRedstonePower(pos);
        boolean wasPowered = state.get(POWERED);

        if (nowPowered && !wasPowered) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof BetterGrindstoneBlockEntity grindstoneBe) {
                grindstoneBe.tryGrindOnce();
            }
        }

        if (nowPowered != wasPowered) {
            world.setBlockState(pos, state.with(POWERED, nowPowered), 3);
        }
    }
}
