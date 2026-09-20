package com.coderdiaz.enjoynights.client.gui;

import com.coderdiaz.enjoynights.EnjoyNights;
import com.coderdiaz.enjoynights.menu.WardingTotemMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class WardingTotemScreen extends AbstractContainerScreen<WardingTotemMenu> {
    private static final Identifier CONTAINER_TEXTURE =
            Identifier.fromNamespaceAndPath(EnjoyNights.MOD_ID, "textures/gui/warding_totem.png");

    private Button toggleButton;

    public WardingTotemScreen(WardingTotemMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        int btnWidth = 76;
        int btnHeight = 20;
        int btnX = this.leftPos + (this.imageWidth - btnWidth) / 2;
        int btnY = this.topPos + 55;

        this.toggleButton = Button.builder(
                this.menu.isActive() ?
                        Component.translatable("enjoy_nights.ward.button.deactivate") :
                        Component.translatable("enjoy_nights.ward.button.activate"),
                btn -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                }
        ).bounds(btnX, btnY, btnWidth, btnHeight).build();

        this.addRenderableWidget(this.toggleButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.toggleButton != null) {
            this.toggleButton.setMessage(
                    this.menu.isActive() ?
                            Component.translatable("enjoy_nights.ward.button.deactivate") :
                            Component.translatable("enjoy_nights.ward.button.activate")
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        if (this.menu.isActive()) {
            Component activeText = Component.translatable("enjoy_nights.ward.status.active");
            int textWidth = this.font.width(activeText);
            guiGraphics.drawString(this.font, activeText, (this.imageWidth - textWidth) / 2, 20, 0x00AAFF, false);
        } else {
            Component inactiveText = Component.translatable("enjoy_nights.ward.status.inactive", this.menu.getRequiredAmethysts());
            int textWidth = this.font.width(inactiveText);
            guiGraphics.drawString(this.font, inactiveText, (this.imageWidth - textWidth) / 2, 20, 0x555555, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CONTAINER_TEXTURE,
                this.leftPos, this.topPos,
                0.0F, 0.0F,
                this.imageWidth, this.imageHeight,
                256, 256
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
