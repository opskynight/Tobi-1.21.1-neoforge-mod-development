# ============================================
# PHASING DETECTION - FIXED
# ============================================
# Activate Phasing: Holding Defensive COAS + Sneaking + Abilities Enabled

# Activate Phasing: Defensive COAS + Sneaking + Abilities Enabled (tobi_enabled=1)
execute as @a[scores={tobi_defensive=1,tobi_enabled=1},predicate=tobi:is_sneaking,gamemode=survival] run scoreboard players set @s tobi_phase 1

# Reset if conditions aren't met
execute as @a[scores={tobi_phase=1}] unless score @s tobi_defensive matches 1 run scoreboard players set @s tobi_phase 0
execute as @a[scores={tobi_phase=1}] unless predicate tobi:is_sneaking run scoreboard players set @s tobi_phase 0
execute as @a[scores={tobi_phase=1}] unless score @s tobi_enabled matches 1 run scoreboard players set @s tobi_phase 0

# CRITICAL: If Kamui activates, turn off phasing
execute as @a[scores={tobi_phase=1,tobi_kamui_active=1..}] run scoreboard players set @s tobi_phase 0
