package com.wdab.bettergrindstone.block;

import com.mojang.serialization.MapCodec;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BetterGrindstoneBlock extends BlockWithEntity {
    public static final MapCodec<BetterGrindstoneBlock> CODEC = createCodec(BetterGrindstoneBlock::new);

    public BetterGrindstoneBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
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
}
