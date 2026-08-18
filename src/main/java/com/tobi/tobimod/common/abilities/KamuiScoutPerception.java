package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Shared scout checks. Cheap: one attachment / payload read, no world scans.
 */
public final class KamuiScoutPerception {
    private KamuiScoutPerception() {}

    public static boolean isUndetectable(@Nullable Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (player.level().isClientSide()) {
            if (player.isLocalPlayer()) {
                return KamuiScoutStatePayload.isClientActive();
            }
            KamuiScoutState remote = player.getExistingDataOrNull(TobiMod.KAMUI_SCOUT_STATE);
            return remote != null && remote.isActive();
        }
        KamuiScoutState state = player.getExistingDataOrNull(TobiMod.KAMUI_SCOUT_STATE);
        return state != null && state.isActive();
    }

    public static float scoutFlySpeed(Player player) {
        if (player.level().isClientSide() && player.isLocalPlayer()) {
            return KamuiScoutStatePayload.getClientFlySpeed();
        }
        KamuiScoutState state = player.getExistingDataOrNull(TobiMod.KAMUI_SCOUT_STATE);
        return state != null && state.isActive() ? state.scoutSpeed() : player.getAbilities().getFlyingSpeed();
    }

    /** One-shot: drop combat chase + leftover path. Does not touch villager look memories. */
    public static void dropCombatAggro(ServerPlayer player, double radius) {
        for (Mob mob : player.level().getEntitiesOfClass(
                Mob.class,
                player.getBoundingBox().inflate(radius),
                mob -> mob.getTarget() == player || mob.getLastHurtByMob() == player
        )) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            if (mob.getLastHurtByMob() == player) {
                mob.setLastHurtByMob(null);
            }
            mob.getNavigation().stop();
        }
    }
}
