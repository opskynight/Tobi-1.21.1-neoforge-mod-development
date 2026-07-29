# ============================================
# GENJUTSU DETECT/CLEANUP - FIXED v3
# ============================================
# Cleanup when switching modes OR removing COAS

# ============================================
# TRIGGER 1: Mode Switch (defensive_mode changed away from 1)
# ============================================
execute as @a unless score @s tobi_defensive_mode matches 1 at @s as @e[tag=genjutsu_target,distance=..25] run data merge entity @s {NoAI:0b}
execute as @a unless score @s tobi_defensive_mode matches 1 at @s run tag @e[tag=genjutsu_target,distance=..25] remove genjutsu_target
execute as @a unless score @s tobi_defensive_mode matches 1 at @s run effect clear @e[distance=..25] minecraft:glowing

# ============================================
# TRIGGER 2: COAS Removed (tobi_defensive = 0)
# ============================================
execute as @a[scores={tobi_defensive=0}] at @s as @e[tag=genjutsu_target,distance=..25] run data merge entity @s {NoAI:0b}
execute as @a[scores={tobi_defensive=0}] at @s run tag @e[tag=genjutsu_target,distance=..25] remove genjutsu_target
execute as @a[scores={tobi_defensive=0}] at @s run effect clear @e[distance=..25] minecraft:glowing

# ============================================
# MARKER CLEANUP - NO DISTANCE LIMIT
# This kills ALL genjutsu markers globally when conditions met
# ============================================
# Trigger 1: Mode switched away
execute as @a unless score @s tobi_defensive_mode matches 1 run kill @e[type=armor_stand,tag=genjutsu_marker]

# Trigger 2: COAS removed
execute as @a[scores={tobi_defensive=0}] run kill @e[type=armor_stand,tag=genjutsu_marker]

# ============================================
# SNEAK FREEZE - INTENTIONALLY SEPARATE
# These targets persist even when switching items/modes
# ============================================
# (No changes here - sneak_target cleanup remains in sneak_freeze.mcfunction)

# ============================================
# DAMAGE COOLDOWN SYSTEM
# ============================================
execute as @e[tag=genjutsu_damaged,scores={tobi_genjutsu_timer=1..}] run scoreboard players remove @s tobi_genjutsu_timer 1
execute as @e[tag=genjutsu_damaged,scores={tobi_genjutsu_timer=0}] run tag @s remove genjutsu_damaged
execute as @e[tag=genjutsu_damaged] at @s unless entity @a[scores={tobi_defensive_mode=1},distance=..20] run tag @s remove genjutsu_damaged
execute as @e[tag=genjutsu_damaged] at @s unless entity @a[scores={tobi_defensive_mode=1},distance=..20] run scoreboard players set @s tobi_genjutsu_timer 0