# ============================================
# LONG RANGE DETECT
# ============================================
# Cleanup when switching modes or losing armor

# Restore AI when player stops sneaking (but still in mode 1)
execute as @a[scores={tobi_offensive_mode=1}] unless predicate tobi:is_sneaking at @s as @e[tag=kamui_target,distance=..30,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}
execute as @a[scores={tobi_offensive_mode=1}] unless predicate tobi:is_sneaking at @s run tag @e[tag=kamui_target,distance=..30] remove kamui_target

# Restore AI when player switches away from mode 1
execute as @a unless score @s tobi_offensive_mode matches 1 at @s as @e[tag=kamui_target,distance=..30,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}

# Kill ALL markers if player switches away from mode 1
execute as @a unless score @s tobi_offensive_mode matches 1 at @s run kill @e[type=armor_stand,tag=kamui_marker,distance=..30]

# Kill ALL markers if player loses armor
execute as @a[tag=!tobi_abilities_enabled] at @s run kill @e[type=armor_stand,tag=kamui_marker,distance=..30]

# Kill ALL markers if player is on cooldown
execute as @a[scores={tobi_cooldown=1..}] at @s run kill @e[type=armor_stand,tag=kamui_marker,distance=..30]

# Clear kamui_target tags when marker is removed or conditions not met
execute as @e[tag=kamui_target] at @s unless entity @e[type=armor_stand,tag=kamui_marker,distance=..30] run tag @s remove kamui_target
execute as @e[tag=kamui_target] at @s unless entity @e[type=armor_stand,tag=kamui_marker,distance=..30] run effect clear @s minecraft:glowing
execute as @e[tag=kamui_target] at @s unless entity @e[type=armor_stand,tag=kamui_marker,distance=..30] run data merge entity @s {NoAI:0b}

execute as @a unless score @s tobi_offensive_mode matches 1 at @s run tag @e[tag=kamui_target,distance=..30] remove kamui_target
execute as @a unless score @s tobi_offensive_mode matches 1 at @s run effect clear @e[tag=kamui_target,distance=..30] minecraft:glowing

# Reset charge if player switches away from mode 1
execute as @a unless score @s tobi_offensive_mode matches 1 run scoreboard players set @s tobi_charge 0

# Reset cooldown if not on mode 1
execute as @a unless score @s tobi_offensive_mode matches 1 run scoreboard players set @s tobi_cooldown 0
