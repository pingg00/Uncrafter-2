package org.pingg.uncrafter_2.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.pingg.uncrafter_2.UnCrafterMod;

@EventBusSubscriber(modid = UnCrafterMod.MOD_ID, value = Dist.CLIENT)
public class UnCrafterClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(UnCrafterMod.UNCRAFTER_MENU.get(), UnCrafterScreen::new);
        event.register(UnCrafterMod.UNCRAFTER_UPGRADED_MENU.get(), UnCrafterScreen::new);
    }
}
