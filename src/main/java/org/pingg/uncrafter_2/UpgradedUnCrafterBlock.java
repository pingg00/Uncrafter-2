package org.pingg.uncrafter_2;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;

public class UpgradedUnCrafterBlock extends UnCrafterBlock {
    public static final MapCodec<UpgradedUnCrafterBlock> UPGRADED_CODEC = simpleCodec(UpgradedUnCrafterBlock::new);

    public UpgradedUnCrafterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return UPGRADED_CODEC;
    }

    @Override
    protected int maxTakes() {
        return 3;
    }

    @Override
    protected MenuType<UnCrafterMenu> menuType() {
        return UnCrafterMod.UNCRAFTER_UPGRADED_MENU.get();
    }

    @Override
    protected Component title() {
        return Component.translatable("container.uncrafter_2.uncrafter_upgraded");
    }
}
