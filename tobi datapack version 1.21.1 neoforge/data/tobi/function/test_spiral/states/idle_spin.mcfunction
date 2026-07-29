# Keep scale at maximum
scoreboard players set @s spiral_scale 100

# FIXED: 12° per tick to match the 30 frame system
# Shows all frames smoothly for clean rotation
scoreboard players add @s spiral_rotation 12
execute if score @s spiral_rotation matches 360.. run scoreboard players remove @s spiral_rotation 360
