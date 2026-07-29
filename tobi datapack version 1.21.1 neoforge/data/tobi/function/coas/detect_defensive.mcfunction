# ============================================
# DETECT HOLDING KAMUI DEFENSIVE STYLE
# ============================================
# Detects when player is holding the Defensive COAS in either hand

# Reset defensive mode for all players first
scoreboard players set @a tobi_defensive 0

# Check mainhand
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_defensive:1b}] run scoreboard players set @s tobi_defensive 1

# Check offhand
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_defensive:1b}] run scoreboard players set @s tobi_defensive 1
