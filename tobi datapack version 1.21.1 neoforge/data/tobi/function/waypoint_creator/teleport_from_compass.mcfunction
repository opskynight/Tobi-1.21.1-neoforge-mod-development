# ============================================
# TELEPORT FROM COMPASS TOKEN - WITH PORTAL SOUNDS
# ============================================

# Clear any old data first
data remove storage tobi:temp compass_data

# Try mainhand
execute if items entity @s weapon.mainhand minecraft:compass[custom_data~{waypoint_token:1b}] run data modify storage tobi:temp compass_data set from entity @s SelectedItem.components."minecraft:custom_data"

# Try offhand if mainhand failed
execute unless data storage tobi:temp compass_data if items entity @s weapon.offhand minecraft:compass[custom_data~{waypoint_token:1b}] run data modify storage tobi:temp compass_data set from entity @s Inventory[{Slot:-106b}].components."minecraft:custom_data"

# Check if we got data
execute unless data storage tobi:temp compass_data run tellraw @s {"text":"[ERROR] Could not read compass data!","color":"red"}
execute unless data storage tobi:temp compass_data run return 0

# ============================================
# ENHANCED PORTAL SOUNDS & EFFECTS
# ============================================

# Portal trigger sound (initial activation)
playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 1

# Portal travel sound (the whoosh)
playsound minecraft:block.portal.travel player @s ~ ~ ~ 1 1

# Ambient portal sound (the continuous hum)
playsound minecraft:block.portal.ambient player @s ~ ~ ~ 0.5 1

# Portal particles BEFORE teleport (at departure)
particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 2 100 force

# Reverse portal particles (spiral upward)
particle minecraft:reverse_portal ~ ~0.5 ~ 0.3 0.5 0.3 0.1 50 force

# End gateway beam particles (dramatic effect)
particle minecraft:end_rod ~ ~1 ~ 0.2 0.5 0.2 0.1 20 force

# Do the teleport
function tobi:waypoint_creator/do_teleport with storage tobi:temp compass_data

# ============================================
# ARRIVAL EFFECTS (after teleport)
# ============================================

# Portal trigger at arrival
playsound minecraft:block.portal.trigger player @s ~ ~ ~ 1 1.5

# Enderman teleport sound
playsound minecraft:entity.enderman.teleport player @s ~ ~ ~ 1 1

# Explosion particles (arrival impact)
particle minecraft:explosion ~ ~1 ~ 0.5 0.5 0.5 0 5 force

# Portal particles at arrival
particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 2 100 force

# Dragon breath particles (mystical arrival)
particle minecraft:dragon_breath ~ ~0.5 ~ 0.3 0.5 0.3 0.05 30 force

# Compass stays in inventory (infinite use)
# Clear storage
data remove storage tobi:temp compass_data
