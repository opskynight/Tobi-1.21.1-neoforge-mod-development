package com.tobi.tobimod.mixin;

import com.tobi.tobimod.common.abilities.KamuiScoutPerception;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes WASD flight use the same scrolled scout speed as Space/Shift.
 * {@code LivingEntity#travel} reads this when airborne.
 */
@Mixin(Player.class)
public abstract class PlayerScoutTravelMixin {

    @Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
    private void tobimod$scoutFlySpeed(CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        if (!KamuiScoutPerception.isUndetectable(self)) {
            return;
        }
        float speed = KamuiScoutPerception.scoutFlySpeed(self);
        cir.setReturnValue(self.isSprinting() ? speed * 2.0F : speed);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void tobimod$scoutGhostSelf(Player viewer, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!KamuiScoutPerception.isUndetectable(self)) {
            return;
        }
        // Self F5 → not invisible-to-self so the renderer uses the translucent pass (ghost).
        // Everyone else → fully hidden.
        cir.setReturnValue(viewer != self);
    }
}
