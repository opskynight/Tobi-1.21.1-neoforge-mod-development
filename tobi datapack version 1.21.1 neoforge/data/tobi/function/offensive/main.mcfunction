# ============================================
# KAMUI OFFENSIVE STYLE - MAIN CONTROLLER - FIXED
# ============================================
# Routes to appropriate mode based on offensive_mode score

# ============================================
# CRITICAL FIX: Run kamui_travel based ONLY on mode, not tobi_offensive
# This is because after F swap, tobi_offensive briefly becomes 0 during item clear/give
# But tobi_offensive_mode persists!
# ============================================

# MODE 0: SHORT RANGE
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=0}] run function tobi:offensive/short_range/main

# MODE 1: LONG RANGE
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=1}] run function tobi:offensive/long_range/main

# MODE 2: RETURN
execute as @a[scores={tobi_offensive=1,tobi_offensive_mode=2}] run function tobi:offensive/return_mode/main

# MODE 3: KAMUI TRAVEL
# Changed: Check tobi_offensive_mode=3 even if tobi_offensive=0
execute as @a[scores={tobi_offensive_mode=3}] run function tobi:offensive/kamui_travel/main

# General display
execute as @a[scores={tobi_offensive=1}] run function tobi:offensive/display

# Mode switching
function tobi:offensive/mode_switch
