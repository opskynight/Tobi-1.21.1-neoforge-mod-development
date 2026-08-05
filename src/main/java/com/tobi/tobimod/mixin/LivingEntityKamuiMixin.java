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
 * Enforces Kamui virtual-floor physics at the living-entity tick level.
 *
 * <p>When Kamui is active this sets {@code noPhysics = true} and resets fall
 * distance every tick so the player never accumulates fall damage.
 * Gravity and onGround are handled by
 * {@link com.tobi.tobimod.common.abilities.KamuiIntangibilityHandler}
 * in {@code PlayerTickEvent.Post} so that they run after travel().
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
        if (player.level().isClientSide()) {
            active = KamuiIntangibilityStatePayload.isClientKamuiActive();
        } else {
            KamuiIntangibilityState state = player.getExistingDataOrNull(TobiMod.KAMUI_INTANGIBILITY_STATE);
            active = state != null && state.isActive();
        }

        if (!active) {
            return;
        }

        player.noPhysics = true;
        player.resetFallDistance();
    }
}
