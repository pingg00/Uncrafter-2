package org.pingg.uncrafter_2.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.pingg.uncrafter_2.UnCrafterMod;
import org.pingg.uncrafter_2.UncraftLogic;

@JeiPlugin
public class AddicationJEI implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(UnCrafterMod.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AddicationJEI_Category(registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(AddicationJEI_Category.TYPE, UncraftLogic.entries(level));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(UnCrafterMod.UNCRAFTER_ITEM.get()), AddicationJEI_Category.TYPE);
        registration.addRecipeCatalyst(new ItemStack(UnCrafterMod.UNCRAFTER_UPGRADED_ITEM.get()), AddicationJEI_Category.TYPE);
    }
}
