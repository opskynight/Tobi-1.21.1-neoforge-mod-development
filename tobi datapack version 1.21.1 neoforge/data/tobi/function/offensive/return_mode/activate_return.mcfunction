# ============================================
# RETURN MODE - ACTIVATE RETURN
# ============================================
# Teleport all kidnapped entities back to player

# Step 1: Reset their scores and remove tag
scoreboard players set @e[tag=tobi_kidnapped] tobi_entity_marked 0
scoreboard players set @e[tag=tobi_kidnapped] tobi_maintain_timer 0

# Step 2: Teleport them to player
execute at @s run tp @e[tag=tobi_kidnapped] @s

# Step 3: Clear effects and tags
effect clear @e[tag=tobi_kidnapped] minecraft:blindness
tag @e[tag=tobi_kidnapped] remove tobi_kidnapped

# Step 4: Remove persistence requirement
execute as @e[type=!player,distance=..5] run data merge entity @s {PersistenceRequired:0b}

# Message to player
tellraw @s {"text":"[Kamui] Entities returned from the void dimension!","color":"aqua","bold":true}

# Sound and particle effects
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 0.8
execute at @s run particle minecraft:portal ~ ~1 ~ 1 1 1 0.5 50 force

# Start cooldown for the player
scoreboard players set @s tobi_cooldown 1

# Reset charge after activation
scoreboard players set @s tobi_charge 0
