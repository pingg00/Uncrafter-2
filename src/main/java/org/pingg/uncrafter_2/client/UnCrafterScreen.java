package org.pingg.uncrafter_2.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.pingg.uncrafter_2.UnCrafterMenu;
import org.pingg.uncrafter_2.UnCrafterMod;
import org.pingg.uncrafter_2.UncraftLogic;

import java.util.Locale;

public class UnCrafterScreen extends AbstractContainerScreen<UnCrafterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(UnCrafterMod.MOD_ID, "textures/gui/uncrafter.png");

    private Button takeButton;

    public UnCrafterScreen(UnCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 176;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        takeButton = addRenderableWidget(Button.builder(Component.empty(), b -> takeAll())
                .bounds(leftPos + 104, topPos + 77, 44, 12).build());
    }

    private void takeAll() {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
    }

    private void updateButton() {
        int shown = menu.ingredientCount();
        int takes = menu.isLocked() ? menu.getTakesLeft() : menu.getMaxTakes();
        takeButton.setMessage(Component.translatable("gui.uncrafter_2.take", shown == 0 ? takes : Math.min(takes, shown)));
        takeButton.active = shown > 0;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);

        UncraftLogic.Reason reason = menu.getFailReason();
        if (reason != UncraftLogic.Reason.NONE && !menu.isLocked()) {
            Component msg = Component.translatable(
                    "gui.uncrafter_2.reason." + reason.name().toLowerCase(Locale.ROOT), menu.getFailArg());
            g.drawWordWrap(font, msg, 8, 63, 86, 0x8B2020);
        } else {
            int xp = menu.getXpCost();
            if (xp > 0) {
                var player = minecraft.player;
                boolean enough = player.getAbilities().instabuild || player.experienceLevel >= xp;
                g.drawString(font, Component.translatable("gui.uncrafter_2.xp_cost", xp), 8, 63, enough ? 0x404040 : 0xA02020, false);
            }
            if (menu.getLossPercent() > 0) {
                g.drawString(font, Component.translatable("gui.uncrafter_2.loss", menu.getLossPercent() + "%"), 8, 72, 0x404040, false);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateButton();
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
