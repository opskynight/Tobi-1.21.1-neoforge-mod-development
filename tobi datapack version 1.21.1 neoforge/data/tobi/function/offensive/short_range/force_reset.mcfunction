# ============================================
# FORCE RESET SHORT RANGE - Emergency fix
# ============================================
# Run this if your ability is stuck

scoreboard players set @s tobi_cooldown 0
scoreboard players set @s tobi_charge 0

# Clean up any tagged entities
execute at @s run tag @e[tag=short_range_target,distance=..20] remove short_range_target
execute at @s as @e[distance=..20,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}

tellraw @s {"text":"[SHORT RANGE] Force reset complete!","color":"green","bold":true}
tellraw @s {"text":"Try sneaking again now.","color":"yellow"}
