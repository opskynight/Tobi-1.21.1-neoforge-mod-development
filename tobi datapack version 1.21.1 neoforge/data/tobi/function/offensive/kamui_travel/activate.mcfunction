# ============================================
# KAMUI TRAVEL - ACTIVATE
# ============================================
# Switch to spectator mode

# Switch to spectator
gamemode spectator @s
scoreboard players set @s tobi_kamui_active 1
scoreboard players set @s tobi_kamui_charge 0

# Initialize position for stillness detection
execute store result score @s tobi_kamui_pos_x run data get entity @s Pos[0] 100
execute store result score @s tobi_kamui_pos_y run data get entity @s Pos[1] 100
execute store result score @s tobi_kamui_pos_z run data get entity @s Pos[2] 100
scoreboard players set @s tobi_kamui_stillness 0

# Messages and effects
tellraw @s {"text":"[Kamui Travel] Spectator mode activated.","color":"aqua","bold":true}
tellraw @s {"text":"→ Keep moving to explore. Stand still for 3 seconds to return.","color":"dark_aqua"}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 0.5
execute at @s run particle minecraft:portal ~ ~1 ~ 0.5 0.5 0.5 1 50 force
