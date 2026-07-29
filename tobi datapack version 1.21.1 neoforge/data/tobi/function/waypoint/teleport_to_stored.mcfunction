# ============================================
# TELEPORT TO STORED LOCATION (MACRO)
# ============================================
# Called with: function tobi:waypoint/teleport_to_stored with storage tobi:temp return_data
# Expects: {dimension:"minecraft:overworld", x:100.5d, y:64.0d, z:-200.3d}

$execute in $(dimension) run tp @s $(x) $(y) $(z)
