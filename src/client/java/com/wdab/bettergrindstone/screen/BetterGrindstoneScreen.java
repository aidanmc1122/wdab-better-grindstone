package com.wdab.bettergrindstone.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class BetterGrindstoneScreen extends HandledScreen<BetterGrindstoneScreenHandler> {

    public BetterGrindstoneScreen(BetterGrindstoneScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        // A reasonable size so slots and player inventory fit.
        // (We aren't drawing a custom background in Step 1.)
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Intentionally blank for Step 1 to avoid 1.21.11 drawTexture API differences.
        // Slots will still render and be interactive.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
