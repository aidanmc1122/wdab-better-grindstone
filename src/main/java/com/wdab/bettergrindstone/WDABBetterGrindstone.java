package com.wdab.bettergrindstone;

import com.wdab.bettergrindstone.block.BetterGrindstoneBlock;
import com.wdab.bettergrindstone.block.entity.BetterGrindstoneBlockEntity;
import com.wdab.bettergrindstone.screen.BetterGrindstoneScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class WDABBetterGrindstone implements ModInitializer {
    public static final String MOD_ID = "wdab-better-grindstone";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static final Identifier BETTER_GRINDSTONE_ID = id("better_grindstone");
    public static final RegistryKey<net.minecraft.block.Block> BETTER_GRINDSTONE_BLOCK_KEY = RegistryKey
            .of(RegistryKeys.BLOCK, BETTER_GRINDSTONE_ID);
    public static final RegistryKey<Item> BETTER_GRINDSTONE_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM,
            BETTER_GRINDSTONE_ID);

    public static Block BETTER_GRINDSTONE;
    public static BlockEntityType<BetterGrindstoneBlockEntity> BETTER_GRINDSTONE_BE;
    public static ScreenHandlerType<BetterGrindstoneScreenHandler> BETTER_GRINDSTONE_SCREEN_HANDLER;

    @Override
    public void onInitialize() {
        // IMPORTANT: settings must carry the BLOCK registry key before Block
        // construction (1.21.11)
        var settings = Block.Settings
                .create()
                .registryKey(BETTER_GRINDSTONE_BLOCK_KEY)
                .strength(2.0F, 6.0F)
                .sounds(BlockSoundGroup.STONE);

        BETTER_GRINDSTONE = new BetterGrindstoneBlock(settings);

        Registry.register(Registries.BLOCK, BETTER_GRINDSTONE_ID, BETTER_GRINDSTONE);

        Registry.register(
                Registries.ITEM,
                BETTER_GRINDSTONE_ID,
                new BlockItem(
                        BETTER_GRINDSTONE,
                        new Item.Settings().registryKey(BETTER_GRINDSTONE_ITEM_KEY)));

        BETTER_GRINDSTONE_BE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                BETTER_GRINDSTONE_ID,
                FabricBlockEntityTypeBuilder.create(BetterGrindstoneBlockEntity::new, BETTER_GRINDSTONE).build()
        );

        BETTER_GRINDSTONE_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                id("better_grindstone"),
                new ExtendedScreenHandlerType<>(BetterGrindstoneScreenHandler::new, BlockPos.PACKET_CODEC)
        );
    }
}
