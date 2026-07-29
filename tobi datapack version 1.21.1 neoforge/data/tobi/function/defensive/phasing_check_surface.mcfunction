# ============================================
# PHASING CHECK SURFACE
# ============================================
# Return to survival when:
# - In spectator mode
# - Phasing is OFF (stopped sneaking or removed COAS)
# - NOT in Kamui
# - Has enough air space above (3 blocks of air to safely stand)

execute as @a[gamemode=spectator,scores={tobi_phase=0,tobi_kamui_active=0}] at @s if block ~ ~ ~ #minecraft:air if block ~ ~1 ~ #minecraft:air if block ~ ~2 ~ #minecraft:air run gamemode survival @s
execute as @a[gamemode=spectator,scores={tobi_phase=0,tobi_kamui_active=0}] at @s if block ~ ~ ~ #minecraft:air if block ~ ~1 ~ #minecraft:air if block ~ ~2 ~ #minecraft:air run tellraw @s {"text":"[Phasing] Returned to surface.","color":"green"}
execute as @a[gamemode=spectator,scores={tobi_phase=0,tobi_kamui_active=0}] at @s if block ~ ~ ~ #minecraft:air if block ~ ~1 ~ #minecraft:air if block ~ ~2 ~ #minecraft:air run playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 0.5 2
execute as @a[gamemode=spectator,scores={tobi_phase=0,tobi_kamui_active=0}] at @s if block ~ ~ ~ #minecraft:air if block ~ ~1 ~ #minecraft:air if block ~ ~2 ~ #minecraft:air run particle minecraft:cloud ~ ~1 ~ 0.3 0.5 0.3 0.02 10 force
