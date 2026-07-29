import math
import os
import shutil

# ============================================
# CONFIGURATION - REDUCED PARTICLE COUNT
# ============================================
BASE_PATH = r"C:\Users\nthan\curseforge\minecraft\Instances\1.21.1 tobi\saves\H2\datapacks\Tobi 1.21.1 New COAS system\data\tobi\function\test_spiral"
CLEAN_FOLDER = os.path.join(BASE_PATH, "blades_clean")
ROTATED_FOLDER = os.path.join(BASE_PATH, "blades_rotated")

def setup_folders():
    """Wipes old frames to prevent 'ghost' particles from old versions"""
    print("🧹 Cleaning up old animation data...")
    if os.path.exists(ROTATED_FOLDER):
        shutil.rmtree(ROTATED_FOLDER)
    if os.path.exists(CLEAN_FOLDER):
        shutil.rmtree(CLEAN_FOLDER)
        
    os.makedirs(CLEAN_FOLDER, exist_ok=True)
    os.makedirs(ROTATED_FOLDER, exist_ok=True)
    print("✅ Folders refreshed.")

def generate_visible_blade(blade_index, angle_offset):
    """Generates a cleaner blade with FEWER particles for distinct spinning"""
    content = f"# Blade {blade_index} - Clean Spinning Arm (Reduced Particles)\n"
    
    # REDUCED from 25 to 12 particles per blade
    # This gives 96 total particles (8 blades × 12) instead of 200
    # Much cleaner for spinning animations!
    num_particles = 12
    max_distance = 2.4  # How far the arm reaches
    max_curve = 165     # How much the arm twists (165 degrees)
    
    print(f"✨ Generating Arm {blade_index} (Starting Angle: {angle_offset}°)")

    for i in range(num_particles):
        # Progress (t) goes from 0.0 at center to 1.0 at the tip
        t = i / (num_particles - 1)
        
        # MATH: Distance starts at 0.3 blocks out to avoid center-blobbing
        distance = 0.3 + (t * (max_distance - 0.3))
        
        # MATH: Curve twist (The "Pinwheel" logic)
        current_curve = t * max_curve
        
        # Convert to polar coordinates
        rad = math.radians(angle_offset + current_curve)
        x = distance * math.cos(rad)
        y = distance * math.sin(rad)
        
        # PARTICLE SIZE: Larger particles (0.9 to 0.5) since we have fewer
        p_scale = 0.9 - (t * 0.4)
        
        # VISIBILITY: Links the arm growth to your 'spiral_scale' score (0-100)
        threshold = int(t * 100)
        
        # Debug only every 3rd particle to keep console clean
        if i % 3 == 0:
            print(f"   [P{i}] Dist: {distance:.2f}m | Curve: +{current_curve:.0f}° | Pos: ({x:.2f}, {y:.2f})")
        
        # The command: Pure black dust, forced visibility
        content += f"execute if score @s spiral_scale matches {threshold}.. at @s anchored eyes run particle minecraft:dust{{color:[0,0,0],scale:{p_scale:.2f}}} ^{x:.2f} ^{y:.2f} ^0.5 0 0 0 0 1 force\n"
    
    return content

# --- MAIN EXECUTION ---
setup_folders()

# 8 Arms (45 degrees apart)
blade_angles = [0, 45, 90, 135, 180, 225, 270, 315]

for i, angle in enumerate(blade_angles):
    file_content = generate_visible_blade(i, angle)
    with open(os.path.join(CLEAN_FOLDER, f"blade_{i}.mcfunction"), 'w') as f:
        f.write(file_content)

# Create the master file that calls all 8 blades
with open(os.path.join(CLEAN_FOLDER, "all_blades.mcfunction"), 'w') as f:
    f.write("# Render 8 Clean Arms (96 particles total - reduced for spinning)\n")
    for i in range(8):
        f.write(f"function tobi:test_spiral/blades_clean/blade_{i}\n")

print("\n🎯 CLEAN GEOMETRY COMPLETE!")
print(f"Total particles: {8 * 12} (reduced from 200)")
print("Next: Run 'generate_rotating_blades.py' to update the rotation frames.")
