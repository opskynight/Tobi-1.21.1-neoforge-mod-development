package com.tobi.tobimod.mixin;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.abilities.KamuiIntangibilityState;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies Underground Kamui physics at the living-entity effect lifecycle point.
 *
 * This replaces the hidden MobEffect. The server reads the player Attachment;
 * the client reads the compact state packet sent when the mode changes.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKamuiMixin {
    @Inject(method = "tickEffects", at = @At("TAIL"))
    private void tobimod$applyUndergroundKamuiPhysics(CallbackInfo callbackInfo) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!(livingEntity instanceof Player player)) {
            return;
        }

        boolean underground;
        if (player.level().isClientSide()) {
            underground = KamuiIntangibilityStatePayload.isClientUnderground();
        } else {
            KamuiIntangibilityState state = player.getExistingDataOrNull(TobiMod.KAMUI_INTANGIBILITY_STATE);
            underground = state != null && state.isActive() && state.isUnderground();
        }

        if (!underground) {
            return;
        }

        player.noPhysics = true;
        player.setNoGravity(true);
        player.setOnGround(false);
        player.resetFallDistance();
    }
}