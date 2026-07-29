package com.tobi.tobimod.client.sound;

import com.tobi.tobimod.common.sound.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Client-side manager for the Kamui channel sound.
 *
 * <p>Plays a single looping sound instance while Kamui is active and stops it
 * the moment the ability deactivates or is interrupted. Because the sound is
 * tracked by reference, calling {@link #stop()} always kills the exact instance
 * that {@link #start()} created — no orphaned sounds, no overlaps.
 *
 * <p>This class is safe to call from any client thread. All methods are
 * idempotent: calling {@link #start()} twice simply ignores the second call;
 * calling {@link #stop()} when nothing is playing is a no-op.
 */
public final class KamuiSoundManager {
    /** The currently playing channel sound, or null if Kamui is inactive. */
    private static SimpleSoundInstance activeSound;

    private KamuiSoundManager() {}

    /**
     * Starts the Kamui channel sound. If a sound is already playing this is a
     * no-op so rapid state-change packets cannot stack sounds.
     */
    public static void start() {
        if (activeSound != null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Relative + no attenuation = the sound always plays at the listener's
        // position and follows the player, exactly what a channel cue needs.
        activeSound = new SimpleSoundInstance(
                ModSoundEvents.KAMUI_CHANNEL.get().getLocation(),
                SoundSource.PLAYERS,
                1.0F,                          // volume
                1.0F,                          // pitch
                mc.player.getRandom(),
                true,                          // looping
                0,                             // delay (ticks)
                SoundInstance.Attenuation.NONE, // no distance falloff
                0.0D, 0.0D, 0.0D,             // position (relative to listener)
                true                           // relative to listener
        );

        mc.getSoundManager().play(activeSound);
    }

    /**
     * Stops the Kamui channel sound immediately. Safe to call even when no
     * sound is playing.
     */
    public static void stop() {
        if (activeSound != null) {
            Minecraft.getInstance().getSoundManager().stop(activeSound);
            activeSound = null;
        }
    }

    /** Returns whether the channel sound is currently playing. */
    public static boolean isPlaying() {
        return activeSound != null;
    }
}
