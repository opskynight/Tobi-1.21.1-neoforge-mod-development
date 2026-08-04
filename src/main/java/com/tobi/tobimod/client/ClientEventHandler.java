package com.tobi.tobimod.client;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.client.keybinds.ModKeybindings;
import com.tobi.tobimod.client.screens.KamuiNavigationScreen;
import com.tobi.tobimod.network.payload.KamuiIntangibilityStatePayload;
import com.tobi.tobimod.network.payload.KamuiIntangibilityTogglePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side Kamui enforcement.
 *
 * <p>Mirrors the server's jitter-prevention: in Pre-tick, when the player
 * is on the virtual floor and not jumping, temporarily disable gravity and
 * zero Y velocity so the client entity tick (travel) doesn't apply the
 * -0.08 downward pull. In Post-tick, restore gravity.
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEventHandler {
    private static final double FLOOR_EPSILON = 0.05D;

    private ClientEventHandler() {}

    public static void beginEnterChannel() {
        // Channel sound removed per user request.
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        KamuiIntangibilityStatePayload.tickClientTimer();

        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientKamuiActive()) {
            Player player = minecraft.player;
            double floorY = KamuiIntangibilityStatePayload.getClientFloorY();
            double yVel = player.getDeltaMovement().y;

            // Client-side enforcement: noPhysics + jitter prevention
            player.noPhysics = true;
            player.resetFallDistance();

            if (player.getY() <= floorY + FLOOR_EPSILON && yVel <= 0.0D) {
                // At or near floor, not rising → stand on virtual floor
                player.setPos(player.getX(), floorY, player.getZ());
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
                player.setOnGround(true);
                player.setNoGravity(true); // prevent jitter in travel()
            }
        }

        while (ModKeybindings.KAMUI_INTANGIBILITY.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                PacketDistributor.sendToServer(new KamuiIntangibilityTogglePayload());
            }
        }

        while (ModKeybindings.KAMUI_NAVIGATION.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                minecraft.setScreen(new KamuiNavigationScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && KamuiIntangibilityStatePayload.isClientKamuiActive()) {
            // Restore gravity after the client entity tick (travel) has run.
            // This matches the server's Post-tick gravity restore.
            minecraft.player.setNoGravity(false);

            // Safety-net floor clamp (in case client prediction drifted)
            double floorY = KamuiIntangibilityStatePayload.getClientFloorY();
            if (minecraft.player.getY() < floorY) {
                minecraft.player.setPos(minecraft.player.getX(), floorY, minecraft.player.getZ());
                minecraft.player.setDeltaMovement(
                        minecraft.player.getDeltaMovement().x,
                        0.0D,
                        minecraft.player.getDeltaMovement().z
                );
                minecraft.player.setOnGround(true);
            }
        }
    }
}