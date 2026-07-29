# ============================================
# GENJUTSU RED EYE EFFECT
# ============================================
# Display a single large red dust particle at player's right eye
# Simulates the Sharingan/Mangekyou Sharingan glow

# Red eye particle at right eye position
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_enabled=1}] at @s anchored eyes run particle minecraft:dust{color:[1.0,0.0,0.0],scale:0.9} ^-0.15 ^0 ^0.5 0.02 0.02 0.02 0 1 force
