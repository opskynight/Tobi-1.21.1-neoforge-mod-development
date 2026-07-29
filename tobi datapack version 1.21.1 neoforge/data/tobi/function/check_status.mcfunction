# ============================================
# CHECK ABILITIES STATUS
# ============================================
# Shows the current state of all ability-related scores and tags
# Usage: /function tobi:check_status

tellraw @s {"text":"========================================","color":"gold","bold":true}
tellraw @s {"text":"       TOBI ABILITIES STATUS","color":"gold","bold":true}
tellraw @s {"text":"========================================","color":"gold","bold":true}

# Show score
tellraw @s [{"text":"tobi_enabled: ","color":"white"},{"score":{"name":"@s","objective":"tobi_enabled"},"color":"gold","bold":true}]

# Show tag status
execute if entity @s[] run tellraw @s {"text":"tobi_abilities_enabled: ✓ PRESENT","color":"green","bold":true}
execute unless entity @s[] run tellraw @s {"text":"tobi_abilities_enabled: ✗ MISSING","color":"red","bold":true}

# Show what this means
tellraw @s {"text":"","color":"white"}
execute if score @s tobi_enabled matches 1 run tellraw @s {"text":"Status: ABILITIES SHOULD BE WORKING","color":"green","bold":true}
execute unless score @s tobi_enabled matches 1 run tellraw @s {"text":"Status: ABILITIES ARE DISABLED","color":"red","bold":true}

# Check for mismatches
execute if score @s tobi_enabled matches 1 unless entity @s[] run tellraw @s {"text":"⚠ WARNING: Score is 1 but tag is missing!","color":"yellow","bold":true}
execute unless score @s tobi_enabled matches 1 if entity @s[] run tellraw @s {"text":"⚠ WARNING: Score is 0 but tag is present!","color":"yellow","bold":true}

# Show COAS detection
tellraw @s {"text":"","color":"white"}
tellraw @s {"text":"COAS Detection:","color":"aqua","bold":true}
execute if score @s tobi_defensive matches 1 run tellraw @s {"text":"  • Defensive COAS: Equipped","color":"green"}
execute unless score @s tobi_defensive matches 1 run tellraw @s {"text":"  • Defensive COAS: Not equipped","color":"gray"}
execute if score @s tobi_offensive matches 1 run tellraw @s {"text":"  • Offensive COAS: Equipped","color":"green"}
execute unless score @s tobi_offensive matches 1 run tellraw @s {"text":"  • Offensive COAS: Not equipped","color":"gray"}
execute if score @s tobi_dimensional matches 1 run tellraw @s {"text":"  • Dimensional COAS: Equipped","color":"green"}
execute unless score @s tobi_dimensional matches 1 run tellraw @s {"text":"  • Dimensional COAS: Not equipped","color":"gray"}

tellraw @s {"text":"========================================","color":"gold","bold":true}
