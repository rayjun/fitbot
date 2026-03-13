import os
import math
from PIL import Image, ImageDraw

OUTPUT_DIR = "app/src/main/assets/exercises"
os.makedirs(OUTPUT_DIR, exist_ok=True)

WIDTH, HEIGHT = 200, 200
BG_COLOR = (250, 250, 250)
LINE_COLOR = (255, 87, 34)
HEAD_COLOR = (255, 87, 34)
EQUIP_COLOR = (150, 150, 150)
LINE_WIDTH = 6
HEAD_RAD = 12

def create_frame():
    img = Image.new("RGB", (WIDTH, HEIGHT), BG_COLOR)
    draw = ImageDraw.Draw(img)
    return img, draw

def save_gif(frames, filename, duration=100):
    frames[0].save(
        os.path.join(OUTPUT_DIR, filename),
        save_all=True,
        append_images=frames[1:],
        duration=duration,
        loop=0
    )

def draw_head(draw, x, y):
    draw.ellipse((x - HEAD_RAD, y - HEAD_RAD, x + HEAD_RAD, y + HEAD_RAD), fill=HEAD_COLOR)

def create_running():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = i / 20.0
        phase = progress
        
        cx, cy = 100, 100
        
        # Body
        draw.line((cx, cy - 20, cx, cy + 30), fill=LINE_COLOR, width=LINE_WIDTH)
        # Head
        draw_head(draw, cx, cy - 35)
        
        # Arms
        arm_swing = 20 * math.sin(phase * 2 * math.pi)
        draw.line((cx, cy - 10, cx - 20, cy + 10 + arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy - 10, cx + 20, cy + 10 - arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Legs
        leg_swing = 30 * math.sin(phase * 2 * math.pi)
        knee_lift = 15 * math.cos(phase * 2 * math.pi)
        
        # Back leg (drawn first so it's behind)
        draw.line((cx, cy + 30, cx + 15 - leg_swing, cy + 60 - knee_lift), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx + 15 - leg_swing, cy + 60 - knee_lift, cx + 20 - leg_swing*1.2, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Front leg
        draw.line((cx, cy + 30, cx - 15 + leg_swing, cy + 60 + knee_lift), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx - 15 + leg_swing, cy + 60 + knee_lift, cx - 20 + leg_swing*1.2, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "running.gif", duration=50)

def create_walking():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = i / 20.0
        phase = progress
        
        cx, cy = 100, 100
        
        # Body
        draw.line((cx, cy - 20, cx, cy + 30), fill=LINE_COLOR, width=LINE_WIDTH)
        # Head
        draw_head(draw, cx, cy - 35)
        
        # Arms
        arm_swing = 15 * math.sin(phase * 2 * math.pi)
        draw.line((cx, cy - 10, cx - 15, cy + 15 + arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy - 10, cx + 15, cy + 15 - arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Legs
        leg_swing = 20 * math.sin(phase * 2 * math.pi)
        
        draw.line((cx, cy + 30, cx - 15 + leg_swing, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy + 30, cx + 15 - leg_swing, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "brisk_walking.gif", duration=60)

def create_cycling():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = i / 20.0
        phase = progress
        
        cx, cy = 100, 100
        
        # Bike frame
        draw.line((cx-30, cy+40, cx+30, cy+40), fill=EQUIP_COLOR, width=4) # bottom
        draw.line((cx-30, cy+40, cx-15, cy-10), fill=EQUIP_COLOR, width=4) # seat
        draw.line((cx+30, cy+40, cx+15, cy-10), fill=EQUIP_COLOR, width=4) # front
        draw.line((cx-15, cy-10, cx+15, cy-10), fill=EQUIP_COLOR, width=4) # top
        
        # Handlebar
        draw.line((cx+15, cy-10, cx+20, cy-20), fill=EQUIP_COLOR, width=4)
        
        # Wheels
        draw.ellipse((cx-50, cy+20, cx-10, cy+60), outline=EQUIP_COLOR, width=4)
        draw.ellipse((cx+10, cy+20, cx+50, cy+60), outline=EQUIP_COLOR, width=4)
        
        # Left leg (back)
        pedal_x = 10 * math.cos(phase * 2 * math.pi)
        pedal_y = 15 * math.sin(phase * 2 * math.pi)
        draw.line((cx-15, cy-10, cx - pedal_x, cy + 40 - pedal_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Body (leaning forward)
        draw.line((cx-15, cy-10, cx-5, cy-35), fill=LINE_COLOR, width=LINE_WIDTH)
        # Head
        draw_head(draw, cx-2, cy-48)
        
        # Arm
        draw.line((cx-5, cy-30, cx+20, cy-20), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Right leg (front)
        draw.line((cx-15, cy-10, cx + pedal_x, cy + 40 + pedal_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "cycling.gif", duration=50)

if __name__ == "__main__":
    create_running()
    create_walking()
    create_cycling()
    print("New cardio GIFs generated in original style.")
