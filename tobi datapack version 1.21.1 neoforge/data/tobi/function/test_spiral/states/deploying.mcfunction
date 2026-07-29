# deploying.mcfunction - FIXED ROTATION SPEED

# Increase scale (spiral grows)
scoreboard players add @s spiral_scale 5
execute if score @s spiral_scale matches 100.. run scoreboard players set @s spiral_scale 100

# FIXED: 12° per tick to match the 30 frame system
# This shows ALL frames smoothly (0°, 12°, 24°, 36°...)
scoreboard players add @s spiral_rotation 12
execute if score @s spiral_rotation matches 360.. run scoreboard players remove @s spiral_rotation 360

# Timer
scoreboard players add @s spiral_timer 1

# After 20 ticks (1 second), switch to idle_spin state
execute if score @s spiral_timer matches 20.. run scoreboard players set @s spiral_state 3
execute if score @s spiral_timer matches 20.. run scoreboard players set @s spiral_timer 0
execute if score @s spiral_timer matches 20.. run tellraw @s {"text":"[Spiral] Fully deployed!","color":"dark_purple"}
