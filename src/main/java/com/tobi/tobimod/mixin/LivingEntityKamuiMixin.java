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
 * <p>When Kamui is active:
 * <ul>
 *   <li>Sets {@code noPhysics = true} and resets fall distance every tick.</li>
 *   <li>Forces {@code onGround = true} when on the virtual floor, so
 *       vanilla's jump check in aiStep() fires and the player can jump.</li>
 * </ul>
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

        // Force onGround=true when on the virtual floor.
        // This runs after Entity.tick() (which may reset onGround)
        // but before aiStep() (which checks onGround for jump).
        if (!player.level().isClientSide()) {
            KamuiIntangibilityState state = player.getExistingDataOrNull(TobiMod.KAMUI_INTANGIBILITY_STATE);
            if (state != null) {
                double floorY = state.floorY();
                double yVel = player.getDeltaMovement().y;
                boolean nearFloor = Math.abs(player.getY() - floorY) < 0.1;
                if (nearFloor && yVel <= 0.0) {
                    player.setOnGround(true);
                }
            }
        }
    }
}