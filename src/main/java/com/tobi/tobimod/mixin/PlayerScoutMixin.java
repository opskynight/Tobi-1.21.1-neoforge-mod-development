package com.tobi.tobimod.mixin;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiScoutState;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes Kamui Scout appear as spectator to vanilla AI.
 * Villagers, golems and all LookAt / NearestAttackable goals check
 * player.isSpectator() — returning true here gives 100% stealth
 * without actually changing gamemode (anti-cheat sees SURVIVAL).
 * Target Entity.isSpectator (base) so injection always finds method,
 * then check if self is Player in scout.
 */
@Mixin(Entity.class)
public abstract class PlayerScoutMixin {

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void tobimod$scoutIsSpectator(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;
        boolean scout;
        if (player.level().isClientSide()) {
            scout = KamuiScoutStatePayload.isClientActive();
        } else {
            KamuiScoutState s = player.getExistingDataOrNull(TobiMod.KAMUI_SCOUT_STATE);
            scout = s != null && s.isActive();
        }
        if (scout) cir.setReturnValue(true);
    }
}