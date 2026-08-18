package com.tobi.tobimod.client.sound;

import com.tobi.tobimod.common.sound.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class KamuiSoundManager {
    private static SimpleSoundInstance activeSound;
    private KamuiSoundManager() {}
    public static void start() {
        // FIX: if leaked from natural finish, clear it so every channel plays
        if (activeSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeSound);
            activeSound = null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        activeSound = new SimpleSoundInstance(
                ModSoundEvents.KAMUI_CHANNEL.get().getLocation(),
                SoundSource.PLAYERS, 1.0F, 1.0F, mc.player.getRandom(),
                false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
        mc.getSoundManager().play(activeSound);
    }
    public static void startChannel() { start(); }
    public static void stop() {
        if (activeSound != null) { Minecraft.getInstance().getSoundManager().stop(activeSound); activeSound = null; }
    }
    public static boolean isPlaying() { return activeSound != null; }
}