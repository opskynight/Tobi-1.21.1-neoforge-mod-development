package com.tobi.tobimod.mixin;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityState;
import com.tobi.tobimod.common.abilities.KamuiScoutState;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enforces Kamui physics at the living-entity tick level.
 *
 * <p>When Kamui is active: sets noPhysics = true and resets fall distance.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKamuiMixin {

    @Inject(method = "tickEffects", at = @At("TAIL"))
    private void tobimod$applyKamuiPhysics(CallbackInfo callbackInfo) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!(livingEntity instanceof Player player)) {
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

        if (!active && !scoutActive) {
            return;
        }

        player.noPhysics = true;
        player.resetFallDistance();
    }
}