import os
from PIL import Image, ImageDraw

OUTPUT_PATH = "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)

WIDTH, HEIGHT = 512, 512
# Background for the icon - clean white or slightly off-white
BG_COLOR = (255, 255, 255)
# Using the same Sporty Orange from the app theme
LINE_COLOR = (255, 87, 34)
LINE_WIDTH = 20
HEAD_RAD = 40

def generate_logo():
    # Create image with alpha channel for transparency if needed, 
    # but for a standard launcher icon, a solid background is often better.
    img = Image.new("RGBA", (WIDTH, HEIGHT), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    # Draw a rounded background circle or square? Let's do a simple stick figure on transparent first.
    # Actually, let's draw a nice rounded rectangle background to make it look like a real app icon.
    # draw.rounded_rectangle((20, 20, 492, 492), radius=100, fill=(255, 255, 255, 255))

    cx, cy = WIDTH // 2, HEIGHT // 2
    
    # Body (Torso)
    draw.line((cx, cy - 50, cx, cy + 100), fill=LINE_COLOR, width=LINE_WIDTH)
    
    # Head
    draw.ellipse((cx - HEAD_RAD, cy - 50 - HEAD_RAD*2, cx + HEAD_RAD, cy - 50), fill=LINE_COLOR)
    
    # Arms - Flexing pose
    # Left arm
    draw.line((cx, cy - 30, cx - 80, cy - 30), fill=LINE_COLOR, width=LINE_WIDTH) # upper
    draw.line((cx - 80, cy - 30, cx - 100, cy - 100), fill=LINE_COLOR, width=LINE_WIDTH) # lower
    # Right arm
    draw.line((cx, cy - 30, cx + 80, cy - 30), fill=LINE_COLOR, width=LINE_WIDTH) # upper
    draw.line((cx + 80, cy - 30, cx + 100, cy - 100), fill=LINE_COLOR, width=LINE_WIDTH) # lower
    
    # Legs - Stable stance
    # Left leg
    draw.line((cx, cy + 100, cx - 60, cy + 180), fill=LINE_COLOR, width=LINE_WIDTH) # upper
    draw.line((cx - 60, cy + 180, cx - 80, cy + 250), fill=LINE_COLOR, width=LINE_WIDTH) # lower
    # Right leg
    draw.line((cx, cy + 100, cx + 60, cy + 180), fill=LINE_COLOR, width=LINE_WIDTH) # upper
    draw.line((cx + 60, cy + 180, cx + 80, cy + 250), fill=LINE_COLOR, width=LINE_WIDTH) # lower

    # Save the image
    img.save(OUTPUT_PATH)
    print(f"Logo generated at {OUTPUT_PATH}")

if __name__ == "__main__":
    generate_logo()
