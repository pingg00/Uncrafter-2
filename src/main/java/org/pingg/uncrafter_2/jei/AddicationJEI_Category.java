package org.pingg.uncrafter_2.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.pingg.uncrafter_2.UnCrafterMod;
import org.pingg.uncrafter_2.UncraftLogic;

public class AddicationJEI_Category implements IRecipeCategory<UncraftLogic.Entry> {
    public static final RecipeType<UncraftLogic.Entry> TYPE =
            RecipeType.create(UnCrafterMod.MOD_ID, "uncrafting", UncraftLogic.Entry.class);

    private static final ResourceLocation GUI =
            ResourceLocation.fromNamespaceAndPath(UnCrafterMod.MOD_ID, "textures/gui/uncrafter.png");
    private static final int WIDTH = 128, HEIGHT = 60;

    private final IDrawable icon;
    private final IDrawable arrow;

    public AddicationJEI_Category(IJeiHelpers helpers) {
        IGuiHelper gui = helpers.getGuiHelper();
        icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(UnCrafterMod.UNCRAFTER_ITEM.get()));
        arrow = gui.createDrawable(GUI, 56, 40, 28, 12);
    }

    @Override
    public RecipeType<UncraftLogic.Entry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.uncrafter_2.category");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, UncraftLogic.Entry recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 22).addItemStack(recipe.input());
        for (int i = 0; i < 9; i++) {
            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 70 + (i % 3) * 18, 3 + (i / 3) * 18);
            ItemStack s = recipe.grid().get(i);
            if (!s.isEmpty()) slot.addItemStack(s);
        }
    }

    public void draw(UncraftLogic.Entry recipe, IRecipeSlotsView slots, GuiGraphics g, double mouseX, double mouseY) {
        arrow.draw(g, 30, 24);
    }
}
