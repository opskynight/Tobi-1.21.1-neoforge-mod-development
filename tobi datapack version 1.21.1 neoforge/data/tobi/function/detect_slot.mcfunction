# ============================================
# DETECT SELECTED HOTBAR SLOT
# ============================================
# Detects which hotbar slot (0-8) the player has selected

# Reset all players' slot to 0 first
scoreboard players set @a tobi_slot 0

# Check each slot (0-8)
execute as @a store result score @s tobi_slot run data get entity @s SelectedItemSlot

# The score will now be:
# 0 = slot 1 (leftmost)
# 1 = slot 2
# 2 = slot 3
# 3 = slot 4
# 4 = slot 5
# 5 = slot 6 ← This is what we want for spiral!
# 6 = slot 7
# 7 = slot 8
# 8 = slot 9 (rightmost)
