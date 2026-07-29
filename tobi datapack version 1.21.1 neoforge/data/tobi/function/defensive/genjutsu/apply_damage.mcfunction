# ============================================
# GENJUTSU - APPLY DAMAGE
# ============================================
# Deal 33% max HP damage with execution below 20%

# Tag entity to prevent repeated damage
tag @s add genjutsu_damaged

# Store MAX health in tobi_genjutsu_dmg
execute store result score @s tobi_genjutsu_dmg run attribute @s minecraft:generic.max_health get 100

# Calculate 1/3 of MAX health (this is the damage to deal)
scoreboard players operation @s tobi_genjutsu_dmg /= #3 tobi_genjutsu_dmg

# Get CURRENT health into tobi_temp_health
execute store result score @s tobi_temp_health run data get entity @s Health 100

# Subtract the damage (1/3 max) from current health
scoreboard players operation @s tobi_temp_health -= @s tobi_genjutsu_dmg

# EXECUTION: If health would go below 20% of max health, kill instantly
# Calculate 20% of max health for execution threshold
execute store result score @s tobi_genjutsu_dmg run attribute @s minecraft:generic.max_health get 100
scoreboard players operation @s tobi_genjutsu_dmg *= #20 tobi_genjutsu_dmg
scoreboard players operation @s tobi_genjutsu_dmg /= #100 tobi_genjutsu_dmg

# If new health is below execution threshold, KILL (drops loot properly)
execute if score @s tobi_temp_health <= @s tobi_genjutsu_dmg run tag @s add genjutsu_execute

# EXECUTION VISUAL EFFECTS (before kill)
execute if entity @s[tag=genjutsu_execute] run particle minecraft:explosion ~ ~1 ~ 0.5 0.5 0.5 0 5 force
execute if entity @s[tag=genjutsu_execute] run particle minecraft:soul ~ ~1 ~ 0.3 0.3 0.3 0.05 10 force
execute if entity @s[tag=genjutsu_execute] run playsound minecraft:entity.wither.death player @a ~ ~ ~ 0.5 2

# Kill if marked for execution (this properly drops loot)
execute if entity @s[tag=genjutsu_execute] run kill @s

# Otherwise, prevent going below 1 HP normally
execute if score @s tobi_temp_health matches 1..99 run scoreboard players set @s tobi_temp_health 100

# Apply the new health value (only if not executed)
execute unless entity @s[tag=genjutsu_execute] store result entity @s Health float 0.01 run scoreboard players get @s tobi_temp_health

# Start cooldown timer (40 ticks = 2 seconds)
scoreboard players set @s tobi_genjutsu_timer 40

# Normal visual feedback - REDUCED particles
particle minecraft:dust{color:[1.0,0.0,0.0],scale:1.5} ~ ~1 ~ 0.3 0.5 0.3 0 8 force
playsound minecraft:entity.player.hurt player @a ~ ~ ~ 0.5 0.5
