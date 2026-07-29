# ============================================
# TOGGLE TOBI ABILITIES ON/OFF - FIXED
# ============================================
# Usage: /function tobi:toggle_abilities
# Toggles between enabled and disabled states

# Check if abilities are currently ENABLED (score = 1)
execute if score @s tobi_enabled matches 1 run function tobi:disable_abilities

# If disabled (score = 0 or not set), enable them
execute unless score @s tobi_enabled matches 1 run function tobi:enable_abilities
