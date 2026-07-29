# ============================================
# DETECT DEFENSIVE MODE
# ============================================
# Reads which mode from COAS NBT
# Mode 0 = Invulnerability, Mode 1 = Genjutsu

# Reset defensive mode for all players holding defensive COAS
execute as @a[scores={tobi_defensive=1}] run scoreboard players set @s tobi_defensive_mode 0

# Check mainhand mode
execute as @a[scores={tobi_defensive=1}] if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_defensive:1b,defensive_mode:0}] run scoreboard players set @s tobi_defensive_mode 0
execute as @a[scores={tobi_defensive=1}] if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_defensive:1b,defensive_mode:1}] run scoreboard players set @s tobi_defensive_mode 1

# Check offhand mode
execute as @a[scores={tobi_defensive=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_defensive:1b,defensive_mode:0}] run scoreboard players set @s tobi_defensive_mode 0
execute as @a[scores={tobi_defensive=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_defensive:1b,defensive_mode:1}] run scoreboard players set @s tobi_defensive_mode 1
