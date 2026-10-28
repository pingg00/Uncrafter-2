package org.pingg.uncrafter_2;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(UnCrafterMod.MOD_ID)
public class UnCrafterMod {
    public static final String MOD_ID = "uncrafter_2";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    private static final SoundType METAL_ANVIL = new SoundType(1.0f, 1.2f,
            SoundEvents.ANVIL_BREAK, SoundEvents.METAL_STEP, SoundEvents.ANVIL_PLACE,
            SoundEvents.METAL_HIT, SoundEvents.METAL_FALL);
    private static final SoundType METAL_ANVIL_HEAVY = new SoundType(1.0f, 1.0f,
            SoundEvents.ANVIL_BREAK, SoundEvents.METAL_STEP, SoundEvents.ANVIL_PLACE,
            SoundEvents.METAL_HIT, SoundEvents.METAL_FALL);

    public static final DeferredBlock<Block> UNCRAFTER_BLOCK = BLOCKS.registerBlock("uncrafter",
            UnCrafterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5f, 6.0f).sound(METAL_ANVIL));

    public static final DeferredItem<UnCrafterBlockItem> UNCRAFTER_ITEM =
            ITEMS.registerItem("uncrafter",
                    props -> new UnCrafterBlockItem(UNCRAFTER_BLOCK.get(), props, 1),
                    new Item.Properties());

    public static final DeferredHolder<MenuType<?>, MenuType<UnCrafterMenu>> UNCRAFTER_MENU =
            MENUS.register("uncrafter", () -> IMenuTypeExtension.create((id, inv, buf) ->
                    new UnCrafterMenu(UnCrafterMod.UNCRAFTER_MENU.get(), id, inv, ContainerLevelAccess.NULL, 1)));

    public static final DeferredBlock<Block> UNCRAFTER_UPGRADED_BLOCK = BLOCKS.registerBlock("uncrafter_upgraded",
            UpgradedUnCrafterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(5.0f, 6.0f).sound(METAL_ANVIL_HEAVY));

    public static final DeferredItem<UnCrafterBlockItem> UNCRAFTER_UPGRADED_ITEM =
            ITEMS.registerItem("uncrafter_upgraded",
                    props -> new UnCrafterBlockItem(UNCRAFTER_UPGRADED_BLOCK.get(), props, 3),
                    new Item.Properties());

    public static final DeferredHolder<MenuType<?>, MenuType<UnCrafterMenu>> UNCRAFTER_UPGRADED_MENU =
            MENUS.register("uncrafter_upgraded", () -> IMenuTypeExtension.create((id, inv, buf) ->
                    new UnCrafterMenu(UnCrafterMod.UNCRAFTER_UPGRADED_MENU.get(), id, inv, ContainerLevelAccess.NULL, 3)));

    public UnCrafterMod(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(UNCRAFTER_ITEM);
            event.accept(UNCRAFTER_UPGRADED_ITEM);
        }
    }
}
