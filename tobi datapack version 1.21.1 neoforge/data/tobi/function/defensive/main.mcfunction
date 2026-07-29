# ============================================
# KAMUI DEFENSIVE STYLE - MAIN CONTROLLER (UPDATED)
# ============================================
# Routes to Invulnerability (Mode 0) or Genjutsu (Mode 1)

# Detect which mode player is using
function tobi:defensive/detect_mode

# ============================================
# MODE 0: INVULNERABILITY (Passive + Phasing)
# ============================================
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=0}] run function tobi:defensive/passive_invulnerability
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=0}] run function tobi:defensive/phasing_detect
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=0}] run function tobi:defensive/phasing_sink
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=0}] run function tobi:defensive/phasing_check_underground
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=0}] run function tobi:defensive/phasing_check_surface

# ============================================
# MODE 1: GENJUTSU (Raycast + Freeze + Torture)
# ============================================
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1}] run function tobi:defensive/genjutsu/main

# ============================================
# STATUS DISPLAY (for both modes)
# ============================================
function tobi:defensive/display

# ============================================
# MODE SWITCHING (offhand swap detection)
# ============================================
# Check if player just swapped the Defensive COAS to offhand
execute as @a[scores={tobi_defensive=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_defensive:1b}] unless score @s tobi_offhand_swap matches 1 run function tobi:defensive/cycle_mode

# Reset swap detection after processing
scoreboard players set @a[scores={tobi_offhand_swap=1}] tobi_offhand_swap 0
