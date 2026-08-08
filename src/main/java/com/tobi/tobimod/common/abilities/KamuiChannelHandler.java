package com.tobi.tobimod.common.abilities;

import com.tobi.tobimod.TobiMod;
import com.tobi.tobimod.common.world.KamuiTravel;
import com.tobi.tobimod.network.payload.KamuiChannelSyncPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side 3-second channel system for ALL Kamui travel abilities.
 */
@EventBusSubscriber(modid = TobiMod.MOD_ID)
public final class KamuiChannelHandler {
    public static final int CHANNEL_TICKS = 60; // 3 seconds
    private static final double MOVEMENT_CANCEL_SQR = 0.09; // 0.3 blocks
    private static final Map<UUID, ChannelData> ACTIVE_CHANNELS = new HashMap<>();
    private KamuiChannelHandler() {}
    public enum Action { ENTER_KAMUI, LEAVE_KAMUI, TRAVEL_TO_WAYPOINT, TRAVEL_TO_COORDS }
    private record ChannelData(Action action,int waypointSlot,double x,double y,double z,long startTime,double startX,double startY,double startZ){
        boolean isComplete(long now){return now-startTime>=CHANNEL_TICKS;}
        int remainingTicks(long now){return (int)(CHANNEL_TICKS-(now-startTime));}
    }
    public static void startEnterChannel(ServerPlayer player){
        long now=player.level().getGameTime();deactivateKamui(player);
        ChannelData d=new ChannelData(Action.ENTER_KAMUI,-1,0,0,0,now,player.getX(),player.getY(),player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(),d);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"),true);
        PacketDistributor.sendToPlayer(player,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }
    public static void startLeaveChannel(ServerPlayer player){
        long now=player.level().getGameTime();deactivateKamui(player);
        ChannelData d=new ChannelData(Action.LEAVE_KAMUI,-1,0,0,0,now,player.getX(),player.getY(),player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(),d);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"),true);
        PacketDistributor.sendToPlayer(player,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }
    public static void startTravelToWaypoint(ServerPlayer player,int slot){
        long now=player.level().getGameTime();deactivateKamui(player);
        ChannelData d=new ChannelData(Action.TRAVEL_TO_WAYPOINT,slot,0,0,0,now,player.getX(),player.getY(),player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(),d);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"),true);
        PacketDistributor.sendToPlayer(player,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }
    public static void startTravelToCoords(ServerPlayer player,double x,double y,double z){
        long now=player.level().getGameTime();deactivateKamui(player);
        ChannelData d=new ChannelData(Action.TRAVEL_TO_COORDS,-1,x,y,z,now,player.getX(),player.getY(),player.getZ());
        ACTIVE_CHANNELS.put(player.getUUID(),d);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_start"),true);
        PacketDistributor.sendToPlayer(player,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.START));
    }
    public static boolean isChanneling(Player player){return ACTIVE_CHANNELS.containsKey(player.getUUID());}
    private static void deactivateKamui(ServerPlayer player){
        KamuiIntangibilityState s=player.getData(TobiMod.KAMUI_INTANGIBILITY_STATE);
        if(s.isActive()) KamuiIntangibilityHandler.deactivate(player,s,player.level().getGameTime(),true);
    }
    private static void cancelChannel(ServerPlayer player,Component r){
        if(ACTIVE_CHANNELS.remove(player.getUUID())!=null){
            player.displayClientMessage(r,true);
            PacketDistributor.sendToPlayer(player,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.CANCEL));
        }
    }
    private static void cancelChannelSilent(ServerPlayer player){
        if(ACTIVE_CHANNELS.remove(player.getUUID())!=null) PacketDistributor.sendToPlayer(player,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.CANCEL));
    }
    public static void handleClientCancel(ServerPlayer player){
        if(isChanneling(player)) cancelChannel(player,Component.translatable("message.tobimod.kamui_channel_interrupted"));
    }
    public static boolean cancelChannelAndActivateKamui(ServerPlayer player){
        if(!isChanneling(player)) return false;
        cancelChannelSilent(player);
        KamuiIntangibilityHandler.forceActivate(player);
        player.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_interrupted"),true);
        return true;
    }
    private static void executeChannel(ServerPlayer player,ChannelData d){
        switch(d.action){
            case ENTER_KAMUI -> KamuiTravel.enter(player);
            case LEAVE_KAMUI -> KamuiTravel.leave(player);
            case TRAVEL_TO_WAYPOINT -> KamuiTravel.travelToWaypoint(player,d.waypointSlot);
            case TRAVEL_TO_COORDS -> KamuiTravel.travelToCoords(player,d.x,d.y,d.z);
        }
    }
    @SubscribeEvent public static void onPlayerTick(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p)) return;
        ChannelData d=ACTIVE_CHANNELS.get(p.getUUID()); if(d==null) return;
        long now=p.level().getGameTime();
        double dx=p.getX()-d.startX,dy=p.getY()-d.startY,dz=p.getZ()-d.startZ;
        if(dx*dx+dy*dy+dz*dz> MOVEMENT_CANCEL_SQR){cancelChannel(p,Component.translatable("message.tobimod.kamui_channel_interrupted"));return;}
        int rem=d.remainingTicks(now);
        if(rem<=0){ACTIVE_CHANNELS.remove(p.getUUID());executeChannel(p,d);}
        else if(rem==CHANNEL_TICKS-1||rem%20==0){int s=(rem+19)/20;p.displayClientMessage(Component.translatable("message.tobimod.kamui_channel_progress",s),true);}
    }
    @SubscribeEvent public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e){ if(e.getEntity() instanceof ServerPlayer p) ACTIVE_CHANNELS.remove(p.getUUID());}
    @SubscribeEvent public static void onLivingHurt(LivingIncomingDamageEvent e){ if(e.getEntity() instanceof ServerPlayer p && ACTIVE_CHANNELS.containsKey(p.getUUID())) cancelChannel(p,Component.translatable("message.tobimod.kamui_channel_interrupted"));}
    @SubscribeEvent public static void onAttackEntity(AttackEntityEvent e){ if(e.getEntity() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e){ if(e.getEntity() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e){ if(e.getEntity() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onRightClickItem(PlayerInteractEvent.RightClickItem e){ if(e.getEntity() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onEntityInteract(PlayerInteractEvent.EntityInteract e){ if(e.getEntity() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific e){ if(e.getEntity() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onBlockBreak(BlockEvent.BreakEvent e){ if(e.getPlayer() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onUseItemOnBlock(UseItemOnBlockEvent e){ if(e.getPlayer() instanceof ServerPlayer p && isChanneling(p)) e.setCanceled(true);}
    @SubscribeEvent public static void onLivingDeath(LivingDeathEvent e){ if(e.getEntity() instanceof ServerPlayer p && ACTIVE_CHANNELS.remove(p.getUUID())!=null) PacketDistributor.sendToPlayer(p,new KamuiChannelSyncPayload(KamuiChannelSyncPayload.Action.CANCEL));}
}