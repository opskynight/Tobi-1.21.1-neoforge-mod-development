# ============================================
# DETECT HOLDING KAMUI DIMENSIONAL STYLE
# ============================================
# Detects when player is holding the Dimensional COAS
# Also reads which mode (Travel=0, Dimension=1, Genjutsu=2)

# Reset dimensional mode for all players first
scoreboard players set @a tobi_dimensional 0

# Check mainhand - set dimensional flag
execute as @a if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b}] run scoreboard players set @s tobi_dimensional 1

# Check offhand - set dimensional flag
execute as @a if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b}] run scoreboard players set @s tobi_dimensional 1

# If holding dimensional COAS, check which mode
# Check mainhand mode
execute as @a[scores={tobi_dimensional=1}] if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b,dimensional_mode:0}] run scoreboard players set @s tobi_dimensional_mode 0
execute as @a[scores={tobi_dimensional=1}] if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b,dimensional_mode:1}] run scoreboard players set @s tobi_dimensional_mode 1
execute as @a[scores={tobi_dimensional=1}] if items entity @s weapon.mainhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b,dimensional_mode:2}] run scoreboard players set @s tobi_dimensional_mode 2

# Check offhand mode
execute as @a[scores={tobi_dimensional=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b,dimensional_mode:0}] run scoreboard players set @s tobi_dimensional_mode 0
execute as @a[scores={tobi_dimensional=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b,dimensional_mode:1}] run scoreboard players set @s tobi_dimensional_mode 1
execute as @a[scores={tobi_dimensional=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_dimensional:1b,dimensional_mode:2}] run scoreboard players set @s tobi_dimensional_mode 2