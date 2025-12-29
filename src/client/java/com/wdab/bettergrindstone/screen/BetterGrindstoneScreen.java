package com.wdab.bettergrindstone.screen;

import com.wdab.bettergrindstone.WDABBetterGrindstone;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BetterGrindstoneScreen extends HandledScreen<BetterGrindstoneScreenHandler> {
    // Resolves to: assets/wdab-better-grindstone/textures/gui/better_grindstone.png
    private static final Identifier TEXTURE = Identifier.of(WDABBetterGrindstone.MOD_ID,
            "textures/gui/better_grindstone.png");

    // Vanilla UI atlas size
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public BetterGrindstoneScreen(BetterGrindstoneScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        // Vanilla grindstone screen size
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;

        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
