# ============================================
# PHASING SINK
# ============================================
# Sink downward ONLY while in SURVIVAL mode and phasing is active
# Once in spectator, this stops running automatically

# Sink downward while phasing
execute as @a[gamemode=survival,scores={tobi_phase=1,tobi_kamui_active=0}] at @s run tp @s ~ ~-0.15 ~

# Subtle ground-entry particles (only in survival)
execute as @a[gamemode=survival,scores={tobi_phase=1,tobi_kamui_active=0}] at @s run particle minecraft:smoke ~ ~0.1 ~ 0.2 0.1 0.2 0.01 3 force
