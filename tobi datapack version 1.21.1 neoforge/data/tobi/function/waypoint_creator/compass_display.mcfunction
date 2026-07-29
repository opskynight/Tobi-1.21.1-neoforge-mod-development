# ============================================
# COMPASS TELEPORT - VISUAL FEEDBACK (3 SECOND)
# ============================================
# Shows progress bar while sneaking with compass

# Check if holding waypoint compass
execute as @a if items entity @s weapon.mainhand minecraft:compass[custom_data~{waypoint_token:1b}] run tag @s add holding_waypoint
execute as @a if items entity @s weapon.offhand minecraft:compass[custom_data~{waypoint_token:1b}] run tag @s add holding_waypoint

# Show action bar for players holding waypoint compass
execute as @a[tag=holding_waypoint,scores={tobi_charge=0}] unless predicate tobi:is_sneaking run title @s actionbar {"text":"◈ Waypoint Compass - SNEAK 3s to teleport ◈","color":"light_purple","bold":true}

# 0-12 ticks (0-0.6s)
execute as @a[tag=holding_waypoint,scores={tobi_charge=1..12},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▱▱▱▱▱▱▱▱▱ Charging...","color":"aqua","bold":true}

# 13-24 ticks (0.6-1.2s)
execute as @a[tag=holding_waypoint,scores={tobi_charge=13..24},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰▰▱▱▱▱▱▱▱ Charging...","color":"aqua","bold":true}

# 25-36 ticks (1.2-1.8s)
execute as @a[tag=holding_waypoint,scores={tobi_charge=25..36},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰▰▰▰▱▱▱▱▱ Charging...","color":"dark_aqua","bold":true}

# 37-48 ticks (1.8-2.4s)
execute as @a[tag=holding_waypoint,scores={tobi_charge=37..48},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰▰▰▰▰▰▱▱▱ Almost ready!","color":"dark_aqua","bold":true}

# 49-59 ticks (2.4-3s)
execute as @a[tag=holding_waypoint,scores={tobi_charge=49..59},predicate=tobi:is_sneaking] run title @s actionbar {"text":"▰▰▰▰▰▰▰▰▰▱ Get ready!","color":"gold","bold":true}

# Clear tag
tag @a remove holding_waypoint
