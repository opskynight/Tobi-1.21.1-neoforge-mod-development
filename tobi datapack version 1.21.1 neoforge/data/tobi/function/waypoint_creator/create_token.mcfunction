# ============================================
# CREATE WAYPOINT TOKEN (COMPASS)
# ============================================
# Stores current position into NBT and creates compass

# Get current coordinates as integers
execute store result storage tobi:temp token_x int 1 run data get entity @s Pos[0]
execute store result storage tobi:temp token_y int 1 run data get entity @s Pos[1]
execute store result storage tobi:temp token_z int 1 run data get entity @s Pos[2]

# Get dimension
data modify storage tobi:temp token_dim set from entity @s Dimension

# Debug output
tellraw @s [{"text":"[DEBUG] Creating token at: ","color":"yellow"}]
tellraw @s [{"text":"X: ","color":"gold"},{"nbt":"token_x","storage":"tobi:temp"}]
tellraw @s [{"text":"Y: ","color":"gold"},{"nbt":"token_y","storage":"tobi:temp"}]
tellraw @s [{"text":"Z: ","color":"gold"},{"nbt":"token_z","storage":"tobi:temp"}]
tellraw @s [{"text":"Dim: ","color":"gold"},{"nbt":"token_dim","storage":"tobi:temp"}]

# Create the compass using macro
function tobi:waypoint_creator/give_compass_token with storage tobi:temp

# Effects
playsound minecraft:block.enchantment_table.use player @s ~ ~ ~ 1 1.5
particle minecraft:enchant ~ ~1 ~ 0.3 0.5 0.3 1 20 force

# Success message
tellraw @s {"text":"[Waypoint Creator] Token created!","color":"gold","bold":true}