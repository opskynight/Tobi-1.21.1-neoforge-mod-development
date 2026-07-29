# Smooth rotating spiral - 30 frames (12° each)
# Renders the appropriate frame based on current rotation angle
# NOW USING CLEAN BLADES!

# Frame 0: 0-11° (uses clean blades)
execute if score @s spiral_rotation matches 0..11 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_clean/all_blades

# Frame 1: 12-23°
execute if score @s spiral_rotation matches 12..23 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_1

# Frame 2: 24-35°
execute if score @s spiral_rotation matches 24..35 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_2

# Frame 3: 36-47°
execute if score @s spiral_rotation matches 36..47 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_3

# Frame 4: 48-59°
execute if score @s spiral_rotation matches 48..59 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_4

# Frame 5: 60-71°
execute if score @s spiral_rotation matches 60..71 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_5

# Frame 6: 72-83°
execute if score @s spiral_rotation matches 72..83 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_6

# Frame 7: 84-95°
execute if score @s spiral_rotation matches 84..95 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_7

# Frame 8: 96-107°
execute if score @s spiral_rotation matches 96..107 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_8

# Frame 9: 108-119°
execute if score @s spiral_rotation matches 108..119 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_9

# Frame 10: 120-131°
execute if score @s spiral_rotation matches 120..131 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_10

# Frame 11: 132-143°
execute if score @s spiral_rotation matches 132..143 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_11

# Frame 12: 144-155°
execute if score @s spiral_rotation matches 144..155 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_12

# Frame 13: 156-167°
execute if score @s spiral_rotation matches 156..167 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_13

# Frame 14: 168-179°
execute if score @s spiral_rotation matches 168..179 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_14

# Frame 15: 180-191°
execute if score @s spiral_rotation matches 180..191 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_15

# Frame 16: 192-203°
execute if score @s spiral_rotation matches 192..203 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_16

# Frame 17: 204-215°
execute if score @s spiral_rotation matches 204..215 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_17

# Frame 18: 216-227°
execute if score @s spiral_rotation matches 216..227 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_18

# Frame 19: 228-239°
execute if score @s spiral_rotation matches 228..239 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_19

# Frame 20: 240-251°
execute if score @s spiral_rotation matches 240..251 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_20

# Frame 21: 252-263°
execute if score @s spiral_rotation matches 252..263 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_21

# Frame 22: 264-275°
execute if score @s spiral_rotation matches 264..275 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_22

# Frame 23: 276-287°
execute if score @s spiral_rotation matches 276..287 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_23

# Frame 24: 288-299°
execute if score @s spiral_rotation matches 288..299 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_24

# Frame 25: 300-311°
execute if score @s spiral_rotation matches 300..311 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_25

# Frame 26: 312-323°
execute if score @s spiral_rotation matches 312..323 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_26

# Frame 27: 324-335°
execute if score @s spiral_rotation matches 324..335 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_27

# Frame 28: 336-347°
execute if score @s spiral_rotation matches 336..347 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_28

# Frame 29: 348-359°
execute if score @s spiral_rotation matches 348..359 if score @s spiral_scale matches 1.. run function tobi:test_spiral/blades_rotated/frame_29

# ======================================
# BLACK CENTER GLOW (KAMUI DIMENSION)
# ======================================

# Dark gray outer layer
execute if score @s spiral_state matches 1..3 if score @s spiral_scale matches 50.. at @s anchored eyes run particle minecraft:dust{color:[0.15,0.15,0.15],scale:1.5} ^ ^ ^0.5 0 0 0 0 1 force

# Darker gray mid-layer
execute if score @s spiral_state matches 1..3 if score @s spiral_scale matches 50.. at @s anchored eyes run particle minecraft:dust{color:[0.08,0.08,0.08],scale:1.2} ^ ^ ^0.5 0.05 0.05 0.05 0 2 force

# Pure black core (void of Kamui dimension)
execute if score @s spiral_state matches 1..3 if score @s spiral_scale matches 50.. at @s anchored eyes run particle minecraft:dust{color:[0.0,0.0,0.0],scale:0.9} ^ ^ ^0.5 0.02 0.02 0.02 0 1 force

# Subtle portal effect
execute if score @s spiral_state matches 1..3 if score @s spiral_scale matches 70.. at @s anchored eyes run particle minecraft:portal ^ ^ ^0.5 0.08 0.08 0.08 0.2 2 force