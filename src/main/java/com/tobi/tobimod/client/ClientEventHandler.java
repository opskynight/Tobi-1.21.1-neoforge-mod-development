package com.tobi.tobimod.client;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.client.screens.KamuiNavigationScreen;
import com.tobi.tobimod.client.sound.KamuiSoundManager;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TobiMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEventHandler {
    private ClientEventHandler() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        KamuiIntangibilityStatePayload.tickClientTimer();

        // Safety net: if the player disconnects or the world unloads while the
        // channel sound is playing, stop it so it doesn't leak.
        if (KamuiSoundManager.isPlaying() && (minecraft.player == null || minecraft.level == null)) {
            KamuiSoundManager.stop();
        }

        // Only underground mode applies client no-clip. Surface mode is ordinary
        // vanilla collision/movement while the server still grants Kamui defense.
        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientUnderground()) {
            minecraft.player.noPhysics = true;
            minecraft.player.setNoGravity(true);
            minecraft.player.resetFallDistance();
        }

        while (ModKeybindings.KAMUI_INTANGIBILITY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new KamuiIntangibilityTogglePayload());
            }
        }

        while (ModKeybindings.KAMUI_NAVIGATION.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new KamuiNavigationScreen());
            }
        }
    }
}