# ============================================
# DEFENSIVE STYLE STATUS DISPLAY (UPDATED)
# ============================================
# Shows different messages based on mode

# No armor warning (overrides everything)
execute as @a[scores={tobi_defensive=1,tobi_enabled=0}] run title @s actionbar {"text":"⚠ ABILITIES DISABLED ⚠","color":"red","bold":true}

# ============================================
# MODE 0: INVULNERABILITY
# ============================================
# Phasing active (sneaking underground)
execute as @a[gamemode=spectator,scores={tobi_defensive=1,tobi_defensive_mode=0,tobi_phase=1}] run title @s actionbar {"text":"◉ INVULNERABILITY - Underground Phasing ◉","color":"dark_gray","bold":true}

# Phasing sinking (still in survival, sinking down)
execute as @a[gamemode=survival,scores={tobi_defensive=1,tobi_defensive_mode=0,tobi_phase=1}] run title @s actionbar {"text":"◉ INVULNERABILITY - Sinking... ◉","color":"gray","bold":true}

# Passive mode (just holding, not phasing)
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=0,tobi_phase=0,tobi_enabled=1}] run title @s actionbar {"text":"▰▰▰ INVULNERABILITY ACTIVE ▰▰▰","color":"dark_red","bold":true}

# ============================================
# MODE 1: GENJUTSU
# ============================================
# Sneak freeze active - shows remaining time in ticks
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_genjutsu_sneak_freeze=2,tobi_genjutsu_sneak_timer=1..,tobi_enabled=1}] run title @s actionbar [{"text":"◉ AREA FREEZE: ","color":"red","bold":true},{"score":{"name":"@s","objective":"tobi_genjutsu_sneak_timer"},"color":"gold"},{"text":"t remaining ◉","color":"red"}]

# Normal genjutsu mode
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_genjutsu_sneak_freeze=..1,tobi_enabled=1}] run title @s actionbar {"text":"◉ GENJUTSU - AIM/SNEAK/CLICK ◉","color":"red","bold":true}
execute as @a[scores={tobi_defensive=1,tobi_defensive_mode=1,tobi_genjutsu_sneak_freeze=2,tobi_genjutsu_sneak_timer=..0,tobi_enabled=1}] run title @s actionbar {"text":"◉ GENJUTSU - AIM/SNEAK/CLICK ◉","color":"red","bold":true}
