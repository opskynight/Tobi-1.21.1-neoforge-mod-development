package com.tobi.tobimod.mixin;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityState;
import com.tobi.tobimod.common.abilities.KamuiScoutState;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents vanilla wall-suffocation ({@code isInWall}) while Kamui
 * intangibility is active.  Without this the player would take
 * in-wall damage every tick when standing inside blocks.
 */
@Mixin(Entity.class)
public abstract class EntityKamuiMixin {

    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void tobimod$preventSuffocation(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }

        boolean active;
        boolean scoutActive;
        if (player.level().isClientSide()) {
            active = KamuiIntangibilityStatePayload.isClientKamuiActive();
            scoutActive = KamuiScoutStatePayload.isClientActive();
        } else {
            KamuiIntangibilityState state = player.getExistingDataOrNull(TobiMod.KAMUI_INTANGIBILITY_STATE);
            active = state != null && state.isActive();
            KamuiScoutState scout = player.getExistingDataOrNull(TobiMod.KAMUI_SCOUT_STATE);
            scoutActive = scout != null && scout.isActive();
        }

        if (active || scoutActive) {
            cir.setReturnValue(false);
        }
    }
}