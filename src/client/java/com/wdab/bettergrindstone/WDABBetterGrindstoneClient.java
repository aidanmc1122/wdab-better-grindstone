package com.wdab.bettergrindstone;

import com.wdab.bettergrindstone.screen.BetterGrindstoneScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class WDABBetterGrindstoneClient implements ClientModInitializer {
    // Register the Better Grindstone screen
    @Override
    public void onInitializeClient() {
        HandledScreens.register(
                WDABBetterGrindstone.BETTER_GRINDSTONE_SCREEN_HANDLER,
                BetterGrindstoneScreen::new);
    }
}
