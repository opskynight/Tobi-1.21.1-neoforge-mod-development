# ============================================
# DETECT HOLDING KAMUI WAYPOINT
# ============================================
# Detects when player is holding the Waypoint COAS

# Reset waypoint flag for all players
scoreboard players set @a tobi_waypoint 0

# Check mainhand
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_waypoint:1b}] run scoreboard players set @s tobi_waypoint 1

# Check offhand
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_waypoint:1b}] run scoreboard players set @s tobi_waypoint 1
