import math
import os
import shutil

# ============================================
# CONFIGURATION - USING INSTANT-DESPAWN PARTICLES
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
    """Generates blade using smoke particles (faster despawn) + dust for thickness"""
    content = f"# Blade {blade_index} - Hybrid Particle Arm (Smoke + Dust)\n"
    
    num_particles = 15  # Middle ground
    max_distance = 2.4
    max_curve = 165
    
    print(f"✨ Generating Arm {blade_index} (Starting Angle: {angle_offset}°)")

    for i in range(num_particles):
        t = i / (num_particles - 1)
        distance = 0.3 + (t * (max_distance - 0.3))
        current_curve = t * max_curve
        
        rad = math.radians(angle_offset + current_curve)
        x = distance * math.cos(rad)
        y = distance * math.sin(rad)
        
        p_scale = 0.85 - (t * 0.45)
        threshold = int(t * 100)
        
        if i % 5 == 0:
            print(f"   [P{i}] Dist: {distance:.2f}m | Curve: +{current_curve:.0f}° | Pos: ({x:.2f}, {y:.2f})")
        
        # HYBRID APPROACH: 
        # 1. Smoke particle (despawns faster, creates motion blur effect)
        content += f"execute if score @s spiral_scale matches {threshold}.. at @s anchored eyes run particle minecraft:smoke ^{x:.2f} ^{y:.2f} ^0.5 0 0 0 0 1 force\n"
        
        # 2. Dust particle (for solid black core of blade)
        content += f"execute if score @s spiral_scale matches {threshold}.. at @s anchored eyes run particle minecraft:dust{{color:[0,0,0],scale:{p_scale:.2f}}} ^{x:.2f} ^{y:.2f} ^0.5 0 0 0 0 1 force\n"
    
    return content

# --- MAIN EXECUTION ---
setup_folders()

blade_angles = [0, 45, 90, 135, 180, 225, 270, 315]

for i, angle in enumerate(blade_angles):
    file_content = generate_visible_blade(i, angle)
    with open(os.path.join(CLEAN_FOLDER, f"blade_{i}.mcfunction"), 'w') as f:
        f.write(file_content)

with open(os.path.join(CLEAN_FOLDER, "all_blades.mcfunction"), 'w') as f:
    f.write("# Render 8 Hybrid Arms (Smoke + Dust for cleaner spinning)\n")
    for i in range(8):
        f.write(f"function tobi:test_spiral/blades_clean/blade_{i}\n")

print("\n🎯 HYBRID GEOMETRY COMPLETE!")
print("Using smoke particles for faster despawn + dust for solidity")
print("Next: Run 'generate_rotating_blades.py' to update the rotation frames.")
