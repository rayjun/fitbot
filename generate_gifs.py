import os
import math
from PIL import Image, ImageDraw

OUTPUT_DIR = "app/src/main/assets/exercises"
os.makedirs(OUTPUT_DIR, exist_ok=True)

WIDTH, HEIGHT = 200, 200
BG_COLOR = (250, 250, 250)
LINE_COLOR = (50, 150, 250)
HEAD_COLOR = (50, 150, 250)
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

def benchpress():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Bench
        draw.line((40, 150, 160, 150), fill=EQUIP_COLOR, width=8)
        # Body (lying down)
        draw.line((60, 140, 140, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 50, 140)
        # Legs
        draw.line((140, 140, 160, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((160, 160, 160, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Arms and bar
        progress = math.sin(i * math.pi / 10) # 0 to 1 to 0
        bar_y = 110 - 40 * progress
        elbow_x = 80
        elbow_y = 140 - 20 * (1 - progress)
        
        draw.line((70, 140, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, 80, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((40, bar_y, 120, bar_y), fill=EQUIP_COLOR, width=6)
        
        frames.append(img)
    save_gif(frames, "benchpress.gif")

def pushup():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Floor
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        
        progress = math.sin(i * math.pi / 10)
        body_y_offset = 30 * (1 - progress)
        
        shoulder_x, shoulder_y = 60, 120 + body_y_offset
        feet_x, feet_y = 160, 175
        
        # Body
        draw.line((shoulder_x, shoulder_y, feet_x, feet_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x - 15, shoulder_y - 10)
        
        # Arms
        elbow_x = 60 + 15 * (1 - progress)
        elbow_y = 150 + 15 * (1 - progress)
        draw.line((shoulder_x, shoulder_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, 60, 175), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "pushup.gif")

def incline_press():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Bench at 45 deg
        draw.line((60, 160, 120, 100), fill=EQUIP_COLOR, width=8)
        draw.line((120, 100, 120, 160), fill=EQUIP_COLOR, width=8) # seat
        
        # Body
        draw.line((70, 145, 120, 95), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 60, 135)
        # Legs
        draw.line((120, 95, 150, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((150, 140, 150, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        # Push perpendicular to body
        # Body vector is (1, -1), normal is (1, 1) or (-1, -1) -> pushing up/left is (-1, -1)
        base_x, base_y = 95, 120
        push_dist = 40 * progress
        bar_x = base_x - push_dist * 0.7
        bar_y = base_y - push_dist * 0.7 - 20
        
        elbow_x = base_x + 10 * (1 - progress)
        elbow_y = base_y + 10 * (1 - progress)
        
        draw.line((base_x, base_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, bar_x, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Barbell
        draw.line((bar_x - 30, bar_y + 30, bar_x + 30, bar_y - 30), fill=EQUIP_COLOR, width=6)
        
        frames.append(img)
    save_gif(frames, "incline_press.gif")

def pullup():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Bar
        draw.line((60, 40, 140, 40), fill=EQUIP_COLOR, width=6)
        
        progress = math.sin(i * math.pi / 10)
        body_y = 110 - 40 * progress
        
        shoulder_x, shoulder_y = 100, body_y
        
        # Body
        draw.line((shoulder_x, shoulder_y, 100, body_y + 60), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, body_y - 15)
        # Legs (crossed)
        draw.line((100, body_y + 60, 90, body_y + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, body_y + 60, 110, body_y + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Arms
        elbow_x = 120 + 10 * progress
        elbow_y = body_y + 20 * (1 - progress)
        draw.line((shoulder_x, shoulder_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, 120, 40), fill=LINE_COLOR, width=LINE_WIDTH) # hand to bar
        
        elbow_x2 = 80 - 10 * progress
        draw.line((shoulder_x, shoulder_y, elbow_x2, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x2, elbow_y, 80, 40), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "pullup.gif")

def row():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Body bent over
        hip_x, hip_y = 120, 130
        shoulder_x, shoulder_y = 70, 90
        
        draw.line((hip_x, hip_y, shoulder_x, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x - 10, shoulder_y - 15)
        
        # Legs slightly bent
        knee_x, knee_y = 130, 160
        feet_x, feet_y = 120, 190
        draw.line((hip_x, hip_y, knee_x, knee_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((knee_x, knee_y, feet_x, feet_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        
        bar_x = 70 + 20 * progress
        bar_y = 140 - 30 * progress
        
        elbow_x = 90 + 20 * progress
        elbow_y = 90 - 20 * progress
        
        draw.line((shoulder_x, shoulder_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, bar_x, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Barbell
        draw.line((bar_x - 20, bar_y - 5, bar_x + 20, bar_y + 5), fill=EQUIP_COLOR, width=6)
        
        frames.append(img)
    save_gif(frames, "row.gif")

def squat():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        
        progress = math.sin(i * math.pi / 10)
        
        hip_x = 100
        hip_y = 120 + 30 * progress
        shoulder_x = 100 + 20 * progress
        shoulder_y = hip_y - 40
        knee_x = 120 + 20 * progress
        knee_y = 150 + 15 * progress
        feet_x, feet_y = 110, 190
        
        # Body
        draw.line((hip_x, hip_y, shoulder_x, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x, shoulder_y - 15)
        
        # Legs
        draw.line((hip_x, hip_y, knee_x, knee_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((knee_x, knee_y, feet_x, feet_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Arms (holding invisible bar on shoulders)
        draw.line((shoulder_x, shoulder_y, shoulder_x + 10, shoulder_y + 15), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Barbell
        draw.line((shoulder_x - 10, shoulder_y, shoulder_x + 30, shoulder_y), fill=EQUIP_COLOR, width=6)
        
        frames.append(img)
    save_gif(frames, "squat.gif")

def lunge():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        
        progress = math.sin(i * math.pi / 10)
        
        body_y = 90 + 30 * progress
        shoulder_x, shoulder_y = 100, body_y
        hip_x, hip_y = 100, body_y + 40
        
        # Body
        draw.line((shoulder_x, shoulder_y, hip_x, hip_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x, shoulder_y - 15)
        
        # Front leg
        front_knee_x = 130 + 10 * progress
        front_knee_y = hip_y + 20 + 20 * progress
        front_foot_x, front_foot_y = 140, 190
        draw.line((hip_x, hip_y, front_knee_x, front_knee_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((front_knee_x, front_knee_y, front_foot_x, front_foot_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Back leg
        back_knee_x = 70
        back_knee_y = hip_y + 20 + 30 * progress
        back_foot_x, back_foot_y = 50, 190
        draw.line((hip_x, hip_y, back_knee_x, back_knee_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((back_knee_x, back_knee_y, back_foot_x, back_foot_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "lunge.gif")

def plank():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Floor
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        
        # Subtle breathing
        progress = math.sin(i * math.pi / 5)
        hip_y = 160 - 2 * progress
        
        shoulder_x, shoulder_y = 60, 150
        feet_x, feet_y = 160, 175
        hip_x = 110
        
        # Body
        draw.line((shoulder_x, shoulder_y, hip_x, hip_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((hip_x, hip_y, feet_x, feet_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x - 15, shoulder_y - 5)
        
        # Arms (elbows on ground)
        elbow_x, elbow_y = 60, 175
        hand_x, hand_y = 80, 175
        draw.line((shoulder_x, shoulder_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, hand_x, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "plank.gif")

def burpee():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Floor
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        
        state = i / 20.0
        
        if state < 0.25: # squat down
            p = state / 0.25
            hip_y = 100 + 40 * p
            shoulder_y = hip_y - 40 + 20 * p
            draw_head(draw, 100, shoulder_y - 15)
            draw.line((100, hip_y, 100, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.line((100, hip_y, 120, 150 + 20*p), fill=LINE_COLOR, width=LINE_WIDTH) # leg
            draw.line((120, 150 + 20*p, 100, 180), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.line((100, shoulder_y, 120, 180), fill=LINE_COLOR, width=LINE_WIDTH) # arms to ground
            
        elif state < 0.5: # kick back to pushup
            p = (state - 0.25) / 0.25
            shoulder_x, shoulder_y = 100 - 20*p, 150
            feet_x, feet_y = 100 + 60*p, 175
            draw_head(draw, shoulder_x - 15, shoulder_y - 10)
            draw.line((shoulder_x, shoulder_y, feet_x, feet_y), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.line((shoulder_x, shoulder_y, shoulder_x, 180), fill=LINE_COLOR, width=LINE_WIDTH) # arm
            
        elif state < 0.75: # jump forward
            p = (state - 0.5) / 0.25
            shoulder_x, shoulder_y = 80 + 20*p, 150 - 30*p
            hip_x, hip_y = 100, 140
            draw_head(draw, shoulder_x, shoulder_y - 15)
            draw.line((shoulder_x, shoulder_y, hip_x, hip_y), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.line((hip_x, hip_y, 100, 180), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.line((shoulder_x, shoulder_y, 100, 180), fill=LINE_COLOR, width=LINE_WIDTH) # arms
            
        else: # jump up
            p = math.sin((state - 0.75) / 0.25 * math.pi)
            jump_h = 30 * p
            shoulder_y = 60 - jump_h
            hip_y = 100 - jump_h
            draw_head(draw, 100, shoulder_y - 15)
            draw.line((100, hip_y, 100, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.line((100, hip_y, 100, 180 - jump_h), fill=LINE_COLOR, width=LINE_WIDTH) # legs
            draw.line((100, shoulder_y, 100, shoulder_y - 30), fill=LINE_COLOR, width=LINE_WIDTH) # arms up
            
        frames.append(img)
    save_gif(frames, "burpee.gif")

if __name__ == "__main__":
    benchpress()
    pushup()
    incline_press()
    pullup()
    row()
    squat()
    lunge()
    plank()
    burpee()
    print("All GIFs generated successfully.")
