# ============================================
# DETECT HOLDING KAMUI OFFENSIVE STYLE (FIXED v4 - WITH TRAVEL MODE)
# ============================================
# Detects when player is holding the Offensive COAS
# Also reads which mode (Short=0, Long=1, Return=2, TRAVEL=3)

# Reset offensive flag for all players
scoreboard players set @a tobi_offensive 0

# ============================================
# CRITICAL FIX: ADD MODE 3 (TRAVEL) DETECTION!
# The original detect_offensive only checked modes 0, 1, 2
# This is why Travel mode wasn't being detected!
# ============================================

# Check mainhand - set offensive flag AND mode
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:0}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:0}] run scoreboard players set @s tobi_offensive_mode 0

execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:1}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:1}] run scoreboard players set @s tobi_offensive_mode 1

execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:2}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:2}] run scoreboard players set @s tobi_offensive_mode 2

# MODE 3 - TRAVEL (THIS WAS MISSING!)
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:3}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:3}] run scoreboard players set @s tobi_offensive_mode 3

# Check offhand - set offensive flag AND mode
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:0}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:0}] run scoreboard players set @s tobi_offensive_mode 0

execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:1}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:1}] run scoreboard players set @s tobi_offensive_mode 1

execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:2}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:2}] run scoreboard players set @s tobi_offensive_mode 2

# MODE 3 - TRAVEL (THIS WAS MISSING!)
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:3}] run scoreboard players set @s tobi_offensive 1
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b,offensive_mode:3}] run scoreboard players set @s tobi_offensive_mode 3
