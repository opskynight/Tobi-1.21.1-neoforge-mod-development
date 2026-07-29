# ============================================
# GENJUTSU TORTURE (RIGHT CLICK)
# ============================================
# Detects right click on carrot_on_a_stick and tortures all frozen mobs

# Detect right click (carrot_on_a_stick used_carrot_on_a_stick stat)
scoreboard players set @a[scores={tobi_defensive=1,tobi_defensive_mode=1}] tobi_genjutsu_click 0
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1}] store result score @s tobi_genjutsu_click run clear @s carrot_on_a_stick 0

# Alternative: Use minecraft.used:minecraft.carrot_on_a_stick stat
# This is more reliable for detecting right clicks
scoreboard objectives add tobi_used_coas minecraft.used:minecraft.carrot_on_a_stick

# When player right clicks while in genjutsu mode
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_used_coas=1..}] run function tobi:defensive/genjutsu/torture_all

# Reset stat
scoreboard players set @a tobi_used_coas 0
