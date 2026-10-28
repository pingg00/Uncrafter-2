package org.pingg.uncrafter_2;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class UnCrafterBlockItem extends BlockItem {
    private final int takes;

    public UnCrafterBlockItem(Block block, Properties properties, int takes) {
        super(block, properties);
        this.takes = takes;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return takes > 1 ? name.copy().withStyle(ChatFormatting.GOLD) : name;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((takes > 1
                ? Component.translatable("tooltip.uncrafter_2.take_up_to", takes)
                : Component.translatable("tooltip.uncrafter_2.take_one")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.uncrafter_2.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
