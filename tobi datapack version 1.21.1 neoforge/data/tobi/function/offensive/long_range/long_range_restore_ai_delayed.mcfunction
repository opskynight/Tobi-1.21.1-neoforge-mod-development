# ============================================
# RESTORE AI DELAYED - Long Range
# ============================================
# Called 1 tick after teleportation to ensure entities are in void dimension

execute in kamui:void as @e[tag=tobi_kidnapped,nbt={NoAI:1b}] run data merge entity @s {NoAI:0b}
