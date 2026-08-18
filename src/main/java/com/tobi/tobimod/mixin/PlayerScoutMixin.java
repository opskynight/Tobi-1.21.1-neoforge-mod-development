package com.tobi.tobimod.mixin;

import com.tobi.tobimod.common.abilities.KamuiScoutPerception;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scout vanish. Must target {@link ServerPlayer} — it overrides {@code Player#isSpectator()}
 * and never calls super. Does not change GameType. Client LocalPlayer is untouched.
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerScoutMixin {

    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void tobimod$scoutUnperceivable(CallbackInfoReturnable<Boolean> cir) {
        if (KamuiScoutPerception.isUndetectable((ServerPlayer) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}