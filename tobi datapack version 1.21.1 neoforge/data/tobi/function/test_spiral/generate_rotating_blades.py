import os
import math
import re

# Your actual path to test_spiral folder
BASE_PATH = r"C:\Users\nthan\curseforge\minecraft\Instances\1.21.1 tobi\saves\H2\datapacks\Tobi 1.21.1 New COAS system\data\tobi\function\test_spiral"

# Path to clean blades folder
BLADES_FOLDER = os.path.join(BASE_PATH, "blades_clean")
OUTPUT_FOLDER = os.path.join(BASE_PATH, "blades_rotated")

# Create output folder
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

def rotate_point(x, y, angle_degrees):
    """Rotate a point (x, y) by angle_degrees clockwise"""
    angle_rad = math.radians(-angle_degrees)  # Negative for clockwise
    cos_a = math.cos(angle_rad)
    sin_a = math.sin(angle_rad)
    
    new_x = x * cos_a - y * sin_a
    new_y = x * sin_a + y * cos_a
    
    return new_x, new_y

# Generate 30 frames (12° apart for smooth rotation)
num_frames = 30
angle_step = 360 / num_frames

for frame_idx in range(1, num_frames + 1):
    rotation = angle_step * frame_idx
    print(f"\n🔄 Generating frame_{frame_idx} (rotated {rotation:.1f}°)...")
    
    frame_content = f"# Rotated frame {frame_idx} - {rotation:.1f}° clockwise\n"
    particle_count = 0
    
    # Read all 8 clean blade files and combine them
    for blade_num in range(8):
        blade_file = os.path.join(BLADES_FOLDER, f"blade_{blade_num}.mcfunction")
        
        if not os.path.exists(blade_file):
            print(f"   ⚠️  blade_{blade_num}.mcfunction not found!")
            continue
            
        with open(blade_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Find all coordinate pairs and full execute lines
        for line in content.split('\n'):
            if 'execute' in line and 'particle' in line:
                # Find coordinates: ^NUMBER ^NUMBER
                match = re.search(r'(\^-?\d+\.\d+) (\^-?\d+\.\d+)', line)
                if match:
                    x_str = match.group(1).replace('^', '')
                    y_str = match.group(2).replace('^', '')
                    x = float(x_str)
                    y = float(y_str)
                    
                    # Rotate the coordinates
                    new_x, new_y = rotate_point(x, y, rotation)
                    
                    # Replace coordinates in line
                    new_line = line.replace(f"{match.group(1)} {match.group(2)}", f"^{new_x:.2f} ^{new_y:.2f}")
                    frame_content += new_line + "\n"
                    particle_count += 1
    
    # Write the combined rotated frame
    output_file = os.path.join(OUTPUT_FOLDER, f"frame_{frame_idx}.mcfunction")
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(frame_content)
    
    print(f"   ✔️ Created frame_{frame_idx}.mcfunction ({particle_count} particles)")

print(f"\n🎯 Done! Created {num_frames} rotation frames")
print(f"📁 Location: {OUTPUT_FOLDER}")
print("\n📋 Final step:")
print("Update render_spiral.mcfunction to use blades_clean/all_blades for frame 0!")