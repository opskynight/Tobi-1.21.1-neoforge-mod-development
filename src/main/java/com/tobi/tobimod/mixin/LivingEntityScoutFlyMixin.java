package com.tobi.tobimod.mixin;

import com.tobi.tobimod.common.abilities.KamuiScoutPerception;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fallback if {@link Player} does not override {@code getFlyingSpeed} in this mapping.
 * If Player does override, {@link PlayerScoutTravelMixin} handles the player instance instead.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityScoutFlyMixin {

    @Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
    private void tobimod$scoutFlySpeed(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player) || !KamuiScoutPerception.isUndetectable(player)) {
            return;
        }
        float speed = KamuiScoutPerception.scoutFlySpeed(player);
        cir.setReturnValue(player.isSprinting() ? speed * 2.0F : speed);
    }
}