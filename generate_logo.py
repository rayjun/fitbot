import os
from PIL import Image, ImageDraw

OUTPUT_PATH = "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)

WIDTH, HEIGHT = 512, 512
LINE_COLOR = (255, 87, 34) # Sporty Orange
LINE_WIDTH = 24
HEAD_RAD = 45

def generate_pushup_logo():
    # Create image with transparent background
    img = Image.new("RGBA", (WIDTH, HEIGHT), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    # Ground line
    draw.line((50, 400, 462, 400), fill=(200, 200, 200), width=8)

    # Push-up pose (Side View)
    # Pivot at feet (bottom right), Head at top left
    feet_x, feet_y = 400, 380
    shoulder_x, shoulder_y = 120, 250
    
    # Body
    draw.line((shoulder_x, shoulder_y, feet_x, feet_y), fill=LINE_COLOR, width=LINE_WIDTH)
    
    # Head
    draw.ellipse((shoulder_x - 60 - HEAD_RAD, shoulder_y - 20 - HEAD_RAD, 
                  shoulder_x - 60 + HEAD_RAD, shoulder_y - 20 + HEAD_RAD), fill=LINE_COLOR)
    
    # Neck/Connector
    draw.line((shoulder_x, shoulder_y, shoulder_x - 60, shoulder_y - 20), fill=LINE_COLOR, width=LINE_WIDTH)

    # Arm 1 (Front)
    elbow_x, elbow_y = 140, 320
    draw.line((shoulder_x, shoulder_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
    draw.line((elbow_x, elbow_y, 140, 395), fill=LINE_COLOR, width=LINE_WIDTH)
    
    # Arm 2 (Back/Offset slightly)
    # shoulder2_x, shoulder2_y = 140, 260
    # draw.line((shoulder2_x, shoulder2_y, 160, 395), fill=LINE_COLOR, width=LINE_WIDTH)

    # Save the image
    img.save(OUTPUT_PATH)
    print(f"Push-up Logo generated at {OUTPUT_PATH}")

if __name__ == "__main__":
    generate_pushup_logo()
