package com.wdab.bettergrindstone.screen;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BetterGrindstoneScreen extends HandledScreen<BetterGrindstoneScreenHandler> {
    // Texture for the better grindstone GUI
    private static final Identifier TEXTURE = Identifier.of(WDABBetterGrindstone.MOD_ID,
            "textures/gui/better_grindstone.png");

    // Texture dimensions (same as vanilla grindstone)
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    // Constructor
    public BetterGrindstoneScreen(BetterGrindstoneScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        // Set GUI dimensions and title positions
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;

        // Title and inventory label positions (same as vanilla grindstone)
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    // Draw the background of the GUI
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {

        // Calculate top-left corner of the GUI
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw the GUI background
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                0.0f,
                0.0f,
                this.backgroundWidth,
                this.backgroundHeight,
                TEX_W,
                TEX_H);
    }

    // Render the entire screen
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
