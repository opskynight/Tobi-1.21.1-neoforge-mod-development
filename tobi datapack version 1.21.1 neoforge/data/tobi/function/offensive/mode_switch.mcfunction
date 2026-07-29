# ============================================
# OFFENSIVE MODE SWITCHING - FIXED v2
# ============================================
# Detects when player swaps Offensive COAS to offhand
# Cycles through: Short (0) → Long (1) → Return (2) → Travel (3) → Short (0)

# ============================================
# BLOCK: Do NOT allow cycling while inside Kamui Travel (spectator)
# ============================================
execute as @a[scores={tobi_kamui_active=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b}] run title @s actionbar {"text":"◉ Cannot switch modes while in Kamui Travel ◉","color":"red","bold":true}
execute as @a[scores={tobi_kamui_active=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b}] run item replace entity @s weapon.offhand with air
execute as @a[scores={tobi_kamui_active=1}] if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b}] run return 0

# ============================================
# CRITICAL FIX: Detect offhand swap and trigger cycle
# ============================================
# When COAS is in offhand AND we haven't already processed this swap
# Also check tobi_offensive_mode to ensure it's set (prevents issues when tobi_offensive=0)
execute as @a[scores={tobi_offhand_swap=0}] if score @s tobi_offensive_mode matches 0..3 if items entity @s weapon.offhand carrot_on_a_stick[custom_data~{tobi_offensive:1b}] run function tobi:offensive/cycle_mode

# ============================================
# CLEAR OFFHAND after processing
# ============================================
# This prevents the item from staying in offhand and blocking future swaps
execute as @a[scores={tobi_offhand_swap=1}] run item replace entity @s weapon.offhand with air

# Reset swap detection after clearing offhand
scoreboard players set @a[scores={tobi_offhand_swap=1}] tobi_offhand_swap 0
