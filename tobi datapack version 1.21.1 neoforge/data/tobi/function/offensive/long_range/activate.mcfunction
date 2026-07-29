# ============================================
# LONG RANGE ACTIVATE - FIXED AI RESTORATION
# ============================================
# Ensures AI is restored AFTER teleportation to Kamui void

# Count entities for feedback
execute store result score @s tobi_temp_x if entity @e[tag=kamui_target]

# Particle and sound at each entity's location BEFORE teleporting
execute as @e[tag=kamui_target] at @s run particle minecraft:explosion ~ ~1 ~ 0 0 0 0 1 force
execute as @e[tag=kamui_target] at @s run playsound minecraft:entity.generic.explode player @a ~ ~ ~ 1 1

# Tag them as kidnapped BEFORE teleporting
tag @e[tag=kamui_target] add tobi_kidnapped
execute as @e[tag=kamui_target] run data merge entity @s {PersistenceRequired:1b}

# Teleport all tagged entities to Kamui dimension at coordinates 0 45 0
execute as @e[tag=kamui_target] in kamui:void run tp @s 0 45 0

# CRITICAL FIX: Wait 1 tick, then restore AI in the void dimension
schedule function tobi:offensive/long_range/restore_ai_delayed 1t

# Clear glowing effect in the void dimension
execute in kamui:void run effect clear @e[tag=kamui_target] minecraft:glowing

# Remove temporary tag (but keep tobi_kidnapped)
execute in kamui:void run tag @e[tag=kamui_target] remove kamui_target

# Remove marker
kill @e[type=armor_stand,tag=kamui_marker]

# Success message and effects
execute if score @s tobi_temp_x matches 1.. run tellraw @s [{"text":"[Kamui] ","color":"dark_purple","bold":true},{"text":"Long-range absorbed ","color":"light_purple"},{"score":{"name":"@s","objective":"tobi_temp_x"},"color":"gold"},{"text":" entities into the void!","color":"light_purple"}]
execute unless score @s tobi_temp_x matches 1.. run tellraw @s {"text":"[Kamui] No entities at marker location!","color":"yellow"}

playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 0.5
execute at @s run particle minecraft:explosion ~ ~1 ~ 1 1 1 0 10 force

# Start cooldown
scoreboard players set @s tobi_cooldown 1

# Reset charge
scoreboard players set @s tobi_charge 0
