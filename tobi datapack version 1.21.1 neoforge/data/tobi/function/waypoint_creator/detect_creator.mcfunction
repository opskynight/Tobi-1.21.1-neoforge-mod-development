# ============================================
# DETECT WAYPOINT CREATOR USAGE
# ============================================

# When player right-clicks the creator
execute as @a[scores={tobi_used_coas=1..}] if items entity @s weapon.mainhand minecraft:carrot_on_a_stick[custom_data~{waypoint_creator:1b}] run function tobi:waypoint_creator/create_token

execute as @a[scores={tobi_used_coas=1..}] if items entity @s weapon.offhand minecraft:carrot_on_a_stick[custom_data~{waypoint_creator:1b}] run function tobi:waypoint_creator/create_token

# Reset
scoreboard players set @a[scores={tobi_used_coas=1..}] tobi_used_coas 0