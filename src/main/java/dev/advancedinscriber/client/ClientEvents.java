/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.advancedinscriber.client;

import dev.advancedinscriber.AdvancedInscriberMod;
import dev.advancedinscriber.container.AdvancedInscriberContainer;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.StyleManager;

@Mod.EventBusSubscriber(modid = AdvancedInscriberMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ScreenManager.<AdvancedInscriberContainer, AdvancedInscriberScreen>registerFactory(
                AdvancedInscriberMod.ADVANCED_INSCRIBER_CONTAINER.get(),
                (container, playerInventory, title) -> {
                    try {
                        ScreenStyle style = StyleManager.loadStyleDoc("/screens/advanced_inscriber.json");
                        return new AdvancedInscriberScreen(container, playerInventory, title, style);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load the Advanced Inscriber screen style", e);
                    }
                }));
    }
}
