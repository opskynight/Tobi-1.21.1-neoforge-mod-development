# ============================================
# DO RETURN - Execute the actual return process
# ============================================
# This runs when player completes the 2-second charge

# Step 1: RESTORE AI FIRST (while still in void dimension)
execute in kamui:void as @e[tag=tobi_kidnapped] run data merge entity @s {NoAI:0b}

# Step 2: Clear blindness (while still in void)
execute in kamui:void run effect clear @e[tag=tobi_kidnapped] minecraft:blindness

# Step 3: Teleport them to player
execute at @s run tp @e[tag=tobi_kidnapped] @s

# Step 4: Remove persistence requirement
execute as @e[tag=tobi_kidnapped] run data merge entity @s {PersistenceRequired:0b}

# Step 5: Reset scores
scoreboard players set @e[tag=tobi_kidnapped] tobi_entity_marked 0
scoreboard players set @e[tag=tobi_kidnapped] tobi_maintain_timer 0

# Step 6: Remove tag
tag @e[tag=tobi_kidnapped] remove tobi_kidnapped

# Success message and effects
tellraw @s {"text":"[Kamui] Entities returned from the void dimension!","color":"aqua","bold":true}
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 1.5
execute at @s run particle minecraft:explosion ~ ~1 ~ 1 1 1 0 10 force

# Start cooldown
scoreboard players set @s tobi_cooldown 1
