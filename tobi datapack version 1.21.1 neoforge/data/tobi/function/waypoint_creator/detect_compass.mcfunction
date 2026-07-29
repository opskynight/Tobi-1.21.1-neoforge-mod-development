# ============================================
# DETECT COMPASS TOKEN USAGE - 3 SECOND CHARGE
# ============================================
# Uses sneak detection - Hold compass + Sneak for 3 seconds (60 ticks)

# Method: Hold compass + Sneak for 3 seconds (60 ticks)
execute as @a[predicate=tobi:is_sneaking] if items entity @s weapon.mainhand minecraft:compass[custom_data~{waypoint_token:1b}] run scoreboard players add @s tobi_charge 1

execute as @a[predicate=tobi:is_sneaking] if items entity @s weapon.offhand minecraft:compass[custom_data~{waypoint_token:1b}] run scoreboard players add @s tobi_charge 1

# Reset charge if not sneaking
execute as @a unless predicate tobi:is_sneaking run scoreboard players set @s tobi_charge 0

# Teleport when charge reaches 60 (3 seconds of sneaking)
execute as @a[scores={tobi_charge=60..}] if items entity @s weapon.mainhand minecraft:compass[custom_data~{waypoint_token:1b}] run function tobi:waypoint_creator/teleport_from_compass

execute as @a[scores={tobi_charge=60..}] if items entity @s weapon.offhand minecraft:compass[custom_data~{waypoint_token:1b}] run function tobi:waypoint_creator/teleport_from_compass

# Reset charge after teleport
execute as @a[scores={tobi_charge=60..}] run scoreboard players set @s tobi_charge 0
