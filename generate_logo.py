import os
from PIL import Image, ImageDraw

OUTPUT_PATH = "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)

WIDTH, HEIGHT = 512, 512
LINE_COLOR = (255, 87, 34) # Sporty Orange
LINE_WIDTH = 24
HEAD_RAD = 40
BARBELL_WIDTH = 320

def generate_harmonious_logo():
    # Create image with transparent background
    img = Image.new("RGBA", (WIDTH, HEIGHT), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    cx, cy = WIDTH // 2, HEIGHT // 2
    
    # 1. Barbell (Top horizontal balance)
    bar_y = cy - 80
    draw.line((cx - BARBELL_WIDTH//2, bar_y, cx + BARBELL_WIDTH//2, bar_y), fill=(100, 100, 100), width=12)
    # Barbell Weights (Sides)
    weight_w, weight_h = 20, 80
    draw.rounded_rectangle((cx - BARBELL_WIDTH//2 - weight_w, bar_y - weight_h//2, 
                            cx - BARBELL_WIDTH//2, bar_y + weight_h//2), radius=5, fill=(80, 80, 80))
    draw.rounded_rectangle((cx + BARBELL_WIDTH//2, bar_y - weight_h//2, 
                            cx + BARBELL_WIDTH//2 + weight_w, bar_y + weight_h//2), radius=5, fill=(80, 80, 80))

    # 2. Robot Body (Centered)
    # Torso
    draw.line((cx, cy - 20, cx, cy + 120), fill=LINE_COLOR, width=LINE_WIDTH)
    
    # 3. Head (Bot Style)
    head_y = cy - 20 - HEAD_RAD
    draw.ellipse((cx - HEAD_RAD, head_y - HEAD_RAD, cx + HEAD_RAD, head_y + HEAD_RAD), fill=LINE_COLOR)
    # Eyes (The 'Bot' touch)
    eye_rad = 6
    draw.ellipse((cx - 15 - eye_rad, head_y - eye_rad, cx - 15 + eye_rad, head_y + eye_rad), fill=(255, 255, 255))
    draw.ellipse((cx + 15 - eye_rad, head_y - eye_rad, cx + 15 + eye_rad, head_y + eye_rad), fill=(255, 255, 255))

    # 4. Arms (Holding Barbell - Symmetrical)
    arm_shoulder_y = cy - 10
    # Left Arm
    draw.line((cx, arm_shoulder_y, cx - 100, arm_shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH) # upper
    draw.line((cx - 100, arm_shoulder_y, cx - 100, bar_y + 10), fill=LINE_COLOR, width=LINE_WIDTH) # lower
    # Right Arm
    draw.line((cx, arm_shoulder_y, cx + 100, arm_shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH) # upper
    draw.line((cx + 100, arm_shoulder_y, cx + 100, bar_y + 10), fill=LINE_COLOR, width=LINE_WIDTH) # lower
    
    # 5. Legs (Stable Base)
    leg_start_y = cy + 120
    draw.line((cx, leg_start_y, cx - 60, leg_start_y + 100), fill=LINE_COLOR, width=LINE_WIDTH)
    draw.line((cx, leg_start_y, cx + 60, leg_start_y + 100), fill=LINE_COLOR, width=LINE_WIDTH)

    # Save the image
    img.save(OUTPUT_PATH)
    print(f"Harmonious Logo generated at {OUTPUT_PATH}")

if __name__ == "__main__":
    generate_harmonious_logo()
