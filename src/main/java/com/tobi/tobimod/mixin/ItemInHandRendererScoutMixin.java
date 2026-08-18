package com.tobi.tobimod.mixin;

import com.tobi.tobimod.network.payload.KamuiScoutStatePayload;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * First-person arms stay visible while scout (ghost self, not empty camera).
 * {@code require = 0} because bytecode may invoke Entity, LivingEntity, or Player.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererScoutMixin {
    private static final int INVISIBLE_FLAG = 5;

    @Redirect(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInvisible()Z"),
            require = 0
    )
    private boolean tobimod$scoutHandsLiving(LivingEntity entity) {
        return hideForScoutHands(entity);
    }

    @Redirect(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"),
            require = 0
    )
    private boolean tobimod$scoutHandsEntity(Entity entity) {
        return hideForScoutHands(entity);
    }

    @Redirect(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInvisible()Z"),
            require = 0
    )
    private boolean tobimod$scoutHandsPlayer(Player entity) {
        return hideForScoutHands(entity);
    }

    private static boolean hideForScoutHands(Entity entity) {
        if (entity instanceof Player && KamuiScoutStatePayload.isClientActive()) {
            return false;
        }
        return ((EntityFlagAccessor) entity).tobimod$invokeGetSharedFlag(INVISIBLE_FLAG);
    }
}
