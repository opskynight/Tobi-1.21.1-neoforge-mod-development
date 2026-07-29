# ============================================
# GENJUTSU SNEAK FREEZE - FINAL FIX
# ============================================
# Sneak once to freeze all mobs within 10 blocks for 8 seconds
# Uses DEDICATED TIMER to avoid conflicts with waypoint/offensive systems

# Detect sneak press (using predicate)
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_enabled=1,tobi_genjutsu_sneak_freeze=0},predicate=tobi:is_sneaking] run scoreboard players set @s tobi_genjutsu_sneak_freeze 1

# On sneak press, tag and freeze all nearby mobs
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] at @s run tag @e[type=!player,type=!armor_stand,type=!item,type=!experience_orb,distance=..10] add genjutsu_sneak_target
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] at @s as @e[tag=genjutsu_sneak_target,distance=..10] run data merge entity @s {NoAI:1b}
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] at @s run effect give @e[tag=genjutsu_sneak_target,distance=..10] minecraft:glowing 9 0 true
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] run tellraw @s {"text":"[Genjutsu] Area freeze activated! (8 seconds)","color":"red","bold":true}
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] at @s run playsound minecraft:entity.wither.ambient player @s ~ ~ ~ 1 2
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] at @s run particle minecraft:dust{color:[0.5,0.0,0.0],scale:2.0} ~ ~1 ~ 10 10 10 0 100 force

# Start DEDICATED timer (160 ticks = 8 seconds)
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] run scoreboard players set @s tobi_genjutsu_sneak_timer 160

# Set flag to 2 (processing)
execute as @a[scores={tobi_genjutsu_sneak_freeze=1}] run scoreboard players set @s tobi_genjutsu_sneak_freeze 2

# Count down DEDICATED timer
execute as @a[scores={tobi_genjutsu_sneak_timer=1..}] run scoreboard players remove @s tobi_genjutsu_sneak_timer 1

# Unfreeze when timer expires
execute as @a[scores={tobi_genjutsu_sneak_timer=0,tobi_genjutsu_sneak_freeze=2}] at @s as @e[tag=genjutsu_sneak_target,distance=..15] run data merge entity @s {NoAI:0b}
execute as @a[scores={tobi_genjutsu_sneak_timer=0,tobi_genjutsu_sneak_freeze=2}] at @s run tag @e[tag=genjutsu_sneak_target,distance=..15] remove genjutsu_sneak_target
execute as @a[scores={tobi_genjutsu_sneak_timer=0,tobi_genjutsu_sneak_freeze=2}] run scoreboard players set @s tobi_genjutsu_sneak_freeze 0

# Reset when player stops sneaking (for next use)
execute as @a[scores={tobi_genjutsu_sneak_freeze=2}] unless predicate tobi:is_sneaking if score @s tobi_genjutsu_sneak_timer matches ..0 run scoreboard players set @s tobi_genjutsu_sneak_freeze 0
