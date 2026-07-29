# ============================================
# HANDLE DEATH - REAPPLY EFFECTS
# ============================================
# Attributes persist through death, but effects don't
# This reapplies effects if player has abilities enabled

# Reapply effects if abilities are enabled and player just died
execute as @a[,scores={tobi_death=1..}] run effect give @s minecraft:night_vision infinite 0 true
execute as @a[,scores={tobi_death=1..}] run effect give @s minecraft:saturation infinite 0 true

# Reset death counter
scoreboard players set @a[scores={tobi_death=1..}] tobi_death 0