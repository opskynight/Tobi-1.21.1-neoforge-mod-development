package com.tobi.tobimod.client.sound;

import com.tobi.tobimod.common.sound.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * Client-side manager for the Kamui CHANNEL sound (kamui_channel.ogg, 3 seconds).
 *
 * <p>This sound is ONLY for the 3-second Kamui travel channel (enter/leave/waypoint/manual).
 * It is non-looping, relative to the player, with no attenuation — it follows the listener.
 * It is played when a channel starts and forcibly stopped if the channel is interrupted
 * (damage, movement, R-cancel) before the 3s elapse. Natural completion needs no stop —
 * the clip ends on its own after 3s.
 *
 * <p>Tracking by reference ensures {@link #stop()} kills the exact instance that
 * {@link #start()} created — no orphaned sounds, no overlaps. All methods are
 * idempotent: double-start is ignored, stop-when-idle is a no-op.
 */
public final class KamuiSoundManager {
    /** The currently playing channel sound, or null if no channel is active. */
    private static SimpleSoundInstance activeSound;

    private KamuiSoundManager() {}

    /**
     * Starts the 3-second Kamui channel sound (non-looping). If a sound is already
     * playing this is a no-op so rapid packets cannot stack sounds.
     */
    public static void start() {
        if (activeSound != null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Relative + no attenuation = always at listener, follows player.
        // Non-looping because the ogg itself is exactly 3s (CHANNEL_TICKS).
        activeSound = new SimpleSoundInstance(
                ModSoundEvents.KAMUI_CHANNEL.get().getLocation(),
                SoundSource.PLAYERS,
                1.0F,                          // volume
                1.0F,                          // pitch
                mc.player.getRandom(),
                false,                         // NOT looping — 3s one-shot
                0,                             // delay (ticks)
                SoundInstance.Attenuation.NONE, // no distance falloff
                0.0D, 0.0D, 0.0D,             // position (relative to listener)
                true                           // relative to listener
        );

        mc.getSoundManager().play(activeSound);
    }

    /** Alias for {@link #start()} — clearer at call sites that start a channel. */
    public static void startChannel() {
        start();
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
