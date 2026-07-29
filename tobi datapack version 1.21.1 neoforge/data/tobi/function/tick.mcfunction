# ============================================
# TOBI DATAPACK - TICK (WITH WAYPOINT CREATOR)
# ============================================

# --- GLOBAL CHECKS ---
# Handle death (reapply effects if abilities enabled)
function tobi:handle_death

# ============================================
# SLOT DETECTION (FOR SPIRAL ANIMATION)
# ============================================
function tobi:detect_slot

# ============================================
# COAS DETECTION
# ============================================
function tobi:coas/detect_defensive
function tobi:coas/detect_offensive
function tobi:coas/detect_dimensional
function tobi:coas/detect_waypoint

# ============================================
# DEFENSIVE STYLE (COAS #1)
# ============================================
execute as @a[scores={tobi_defensive=1}] run function tobi:defensive/main

# ============================================
# OFFENSIVE STYLE (COAS #2)
# ============================================
execute as @a[scores={tobi_offensive=1}] run function tobi:offensive/main

# ============================================
# KAMUI TRAVEL SAFETY NET
# ============================================
execute as @a[scores={tobi_kamui_active=1}] run function tobi:offensive/kamui_travel/detect_stillness

# ============================================
# DIMENSIONAL STYLE (COAS #3)
# ============================================
execute as @a[scores={tobi_dimensional=1}] run function tobi:dimensional/main

# ============================================
# WAYPOINT STYLE (COAS #4)
# ============================================
execute as @a[scores={tobi_waypoint=1}] run function tobi:waypoint/main

# ============================================
# WAYPOINT CREATOR SYSTEM (NEW!)
# ============================================
function tobi:waypoint_creator/main

# ============================================
# SPIRAL ANIMATION (SLOT 6)
# ============================================
function tobi:test_spiral/main
