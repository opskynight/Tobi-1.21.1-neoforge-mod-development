# ============================================
# GIVE ALL TOBI ITEMS (ARMOR + ALL 3 COAS)
# ============================================
# Usage: /function tobi:give_all

# Give Defensive COAS
function tobi:coas/give_defensive

# Give Offensive COAS
function tobi:coas/give_offensive

# Give Dimensional COAS
function tobi:coas/give_dimensional

tellraw @p {"text":"[Tobi] Complete equipment received!","color":"gold","bold":true}
tellraw @p {"text":"→ Defensive + Offensive + Dimensional COAS","color":"yellow"}
tellraw @p {"text":"→ Swap to offhand (F key) to change modes","color":"green"}
tellraw @p {"text":"→ Run /function tobi:toggle to enable powers","color":"aqua"}