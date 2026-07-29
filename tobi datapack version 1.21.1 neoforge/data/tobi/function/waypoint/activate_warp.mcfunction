# ============================================
# KAMUI WAYPOINT - ACTIVATE WARP (FIXED v2)
# ============================================
# Store current position in NBT and teleport to kamui:void

# ============================================
# Get dimension directly from entity NBT
# ============================================
data modify storage tobi:temp dimension set from entity @s Dimension

# ============================================
# Store coordinates
# ============================================
data modify storage tobi:temp x set from entity @s Pos[0]
data modify storage tobi:temp y set from entity @s Pos[1]
data modify storage tobi:temp z set from entity @s Pos[2]

# ============================================
# DEBUG OUTPUT
# ============================================
tellraw @s {"text":"========================================","color":"gold"}
tellraw @s {"text":"[WAYPOINT] Saving current location...","color":"aqua","bold":true}
tellraw @s [{"text":"Dimension: ","color":"yellow"},{"nbt":"Dimension","entity":"@s","color":"white"}]
tellraw @s [{"text":"Position: ","color":"yellow"},{"nbt":"Pos","entity":"@s","color":"white"}]

# ============================================
# Store in waypoint locations array
# ============================================
# Get player's UUID
data modify storage tobi:temp uuid set from entity @s UUID

# Initialize storage if it doesn't exist
execute unless data storage tobi:waypoint locations run data modify storage tobi:waypoint locations set value []

# Create new entry
data modify storage tobi:waypoint locations append value {}
data modify storage tobi:waypoint locations[-1].uuid set from storage tobi:temp uuid
data modify storage tobi:waypoint locations[-1].dimension set from storage tobi:temp dimension
data modify storage tobi:waypoint locations[-1].x set from storage tobi:temp x
data modify storage tobi:waypoint locations[-1].y set from storage tobi:temp y
data modify storage tobi:waypoint locations[-1].z set from storage tobi:temp z

# Show what was stored
tellraw @s [{"text":"Stored: ","color":"green"},{"nbt":"waypoint.locations[-1]","storage":"tobi:","color":"white"}]
tellraw @s {"text":"========================================","color":"gold"}

# ============================================
# Teleport to kamui:void at 0 45 0
# ============================================
execute in kamui:void run tp @s 0 45 0

# Reset charge
scoreboard players set @s tobi_charge 0

# Success message
tellraw @s {"text":"[Kamui Waypoint] Warped to pocket dimension!","color":"dark_purple","bold":true}
tellraw @s {"text":"→ Sneak for 3 seconds to return to your saved location","color":"light_purple"}

# Effects
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 0.5
execute at @s run particle minecraft:portal ~ ~1 ~ 1 1 1 1 100 force

# Update COAS to show "Return"
function tobi:waypoint/update_coas_return with storage tobi:temp
