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

# --- Cardio ---
def create_running():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = i / 20.0
        cx, cy = 100, 100
        draw.line((cx, cy - 20, cx, cy + 30), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, cx, cy - 35)
        arm_swing = 20 * math.sin(progress * 2 * math.pi)
        draw.line((cx, cy - 10, cx - 20, cy + 10 + arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy - 10, cx + 20, cy + 10 - arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        leg_swing = 30 * math.sin(progress * 2 * math.pi)
        knee_lift = 15 * math.cos(progress * 2 * math.pi)
        draw.line((cx, cy + 30, cx + 15 - leg_swing, cy + 60 - knee_lift), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx + 15 - leg_swing, cy + 60 - knee_lift, cx + 20 - leg_swing*1.2, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy + 30, cx - 15 + leg_swing, cy + 60 + knee_lift), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx - 15 + leg_swing, cy + 60 + knee_lift, cx - 20 + leg_swing*1.2, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "running.gif", duration=50)

def create_walking():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = i / 20.0
        cx, cy = 100, 100
        draw.line((cx, cy - 20, cx, cy + 30), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, cx, cy - 35)
        arm_swing = 15 * math.sin(progress * 2 * math.pi)
        draw.line((cx, cy - 10, cx - 15, cy + 15 + arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy - 10, cx + 15, cy + 15 - arm_swing), fill=LINE_COLOR, width=LINE_WIDTH)
        leg_swing = 20 * math.sin(progress * 2 * math.pi)
        draw.line((cx, cy + 30, cx - 15 + leg_swing, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx, cy + 30, cx + 15 - leg_swing, cy + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "brisk_walking.gif", duration=60)

def create_cycling():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = i / 20.0
        cx, cy = 100, 100
        draw.line((cx-30, cy+40, cx+30, cy+40), fill=EQUIP_COLOR, width=4)
        draw.line((cx-30, cy+40, cx-15, cy-10), fill=EQUIP_COLOR, width=4)
        draw.line((cx+30, cy+40, cx+15, cy-10), fill=EQUIP_COLOR, width=4)
        draw.line((cx-15, cy-10, cx+15, cy-10), fill=EQUIP_COLOR, width=4)
        draw.line((cx+15, cy-10, cx+20, cy-20), fill=EQUIP_COLOR, width=4)
        draw.ellipse((cx-50, cy+20, cx-10, cy+60), outline=EQUIP_COLOR, width=4)
        draw.ellipse((cx+10, cy+20, cx+50, cy+60), outline=EQUIP_COLOR, width=4)
        pedal_x = 10 * math.cos(progress * 2 * math.pi)
        pedal_y = 15 * math.sin(progress * 2 * math.pi)
        draw.line((cx-15, cy-10, cx - pedal_x, cy + 40 - pedal_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx-15, cy-10, cx-5, cy-35), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, cx-2, cy-48)
        draw.line((cx-5, cy-30, cx+20, cy-20), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((cx-15, cy-10, cx + pedal_x, cy + 40 + pedal_y), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "cycling.gif", duration=50)

# --- New Gym Exercises ---

def create_dumbbell_fly():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((40, 150, 160, 150), fill=EQUIP_COLOR, width=8) # Bench
        draw.line((60, 140, 140, 140), fill=LINE_COLOR, width=LINE_WIDTH) # Body
        draw_head(draw, 50, 140)
        draw.line((140, 140, 160, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((160, 160, 160, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10) # 0 to 1 back to 0
        arm_spread = 60 * progress
        elbow_y = 140 + 20 * progress
        # Back arm
        draw.line((80, 140, 80 + arm_spread, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.ellipse((80 + arm_spread - 5, elbow_y - 10, 80 + arm_spread + 5, elbow_y + 10), fill=EQUIP_COLOR)
        # Front arm
        draw.line((80, 140, 80 - arm_spread, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.ellipse((80 - arm_spread - 5, elbow_y - 10, 80 - arm_spread + 5, elbow_y + 10), fill=EQUIP_COLOR)
        frames.append(img)
    save_gif(frames, "dumbbell_fly.gif")

def create_cable_crossover():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Machine frame
        draw.line((30, 20, 30, 180), fill=EQUIP_COLOR, width=6)
        draw.line((170, 20, 170, 180), fill=EQUIP_COLOR, width=6)
        draw.line((30, 20, 170, 20), fill=EQUIP_COLOR, width=6)
        
        # Body
        draw.line((100, 80, 100, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, 65)
        draw.line((100, 140, 90, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 140, 110, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        hand_dist = 60 * (1 - progress)
        hand_y = 120 + 20 * progress
        
        # Cables
        draw.line((30, 30, 100 - hand_dist, hand_y), fill=EQUIP_COLOR, width=2)
        draw.line((170, 30, 100 + hand_dist, hand_y), fill=EQUIP_COLOR, width=2)
        
        # Arms
        draw.line((100, 80, 100 - hand_dist, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 80, 100 + hand_dist, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        frames.append(img)
    save_gif(frames, "cable_crossover.gif")

def create_lat_pulldown():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((100, 20, 100, 180), fill=EQUIP_COLOR, width=6) # Frame
        draw.line((70, 140, 100, 140), fill=EQUIP_COLOR, width=6) # Seat
        
        draw.line((70, 90, 70, 140), fill=LINE_COLOR, width=LINE_WIDTH) # Body
        draw_head(draw, 70, 75)
        draw.line((70, 140, 100, 140), fill=LINE_COLOR, width=LINE_WIDTH) # Thigh
        draw.line((100, 140, 100, 180), fill=LINE_COLOR, width=LINE_WIDTH) # Calf
        
        progress = math.sin(i * math.pi / 10)
        bar_y = 40 + 40 * progress
        draw.line((100, 20, 100, bar_y), fill=EQUIP_COLOR, width=2) # Cable
        draw.line((60, bar_y, 140, bar_y), fill=EQUIP_COLOR, width=4) # Bar
        
        # Arms
        draw.line((70, 90, 80, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "lat_pulldown.gif")

def create_face_pull():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((160, 20, 160, 180), fill=EQUIP_COLOR, width=6) # Frame
        
        # Body (leaning back slightly)
        draw.line((70, 90, 90, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 65, 75)
        draw.line((90, 140, 80, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((90, 140, 110, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        rope_x = 160 - 70 * progress
        draw.line((160, 80, rope_x, 80), fill=EQUIP_COLOR, width=2) # Cable
        
        # Arms pulling to face
        draw.line((70, 90, rope_x, 80), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "face_pull.gif")

def create_front_raise():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Body
        draw.line((100, 80, 100, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, 65)
        draw.line((100, 140, 90, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 140, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        angle = math.pi/2 - progress * math.pi/2 # Down to Forward
        hand_x = 100 + 40 * math.sin(angle)
        hand_y = 80 + 40 * math.cos(angle)
        
        draw.line((100, 80, hand_x, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.ellipse((hand_x-5, hand_y-10, hand_x+5, hand_y+10), fill=EQUIP_COLOR)
        frames.append(img)
    save_gif(frames, "front_raise.gif")

def create_hammer_curl():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        # Body
        draw.line((100, 80, 100, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, 65)
        draw.line((100, 140, 90, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 140, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        angle = progress * math.pi/1.8 # Neutral grip visual trick: vertical dumbbell
        hand_y = 120 - 40 * math.sin(angle)
        hand_x = 100 + 40 * math.cos(angle)
        
        draw.line((100, 80, 100, 120), fill=LINE_COLOR, width=LINE_WIDTH) # upper arm
        draw.line((100, 120, hand_x, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
        
        # Dumbbell drawn vertically
        draw.line((hand_x, hand_y-15, hand_x, hand_y+15), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "hammer_curl.gif")

def create_triceps_pushdown():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((150, 20, 150, 180), fill=EQUIP_COLOR, width=6) # Frame
        
        # Body
        draw.line((90, 80, 90, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 90, 65)
        draw.line((90, 140, 80, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((90, 140, 100, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        
        progress = math.sin(i * math.pi / 10)
        hand_y = 100 + 40 * progress
        draw.line((150, 20, 110, hand_y), fill=EQUIP_COLOR, width=2) # Cable
        
        draw.line((90, 80, 110, 100), fill=LINE_COLOR, width=LINE_WIDTH) # upper arm static
        draw.line((110, 100, 110, hand_y), fill=LINE_COLOR, width=LINE_WIDTH) # forearm pushes down
        frames.append(img)
    save_gif(frames, "triceps_pushdown.gif")

def create_leg_press():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((40, 160, 100, 160), fill=EQUIP_COLOR, width=8) # Seat
        draw.line((40, 160, 60, 100), fill=EQUIP_COLOR, width=8) # Backrest
        
        draw.line((60, 110, 80, 150), fill=LINE_COLOR, width=LINE_WIDTH) # Torso
        draw_head(draw, 50, 90)
        
        progress = math.sin(i * math.pi / 10)
        knee_x, knee_y = 100 + 10 * progress, 120 + 10 * progress
        foot_x, foot_y = 140 + 40 * progress, 80 + 40 * progress
        
        draw.line((80, 150, knee_x, knee_y), fill=LINE_COLOR, width=LINE_WIDTH) # Thigh
        draw.line((knee_x, knee_y, foot_x, foot_y), fill=LINE_COLOR, width=LINE_WIDTH) # Calf
        
        # Plate
        draw.line((foot_x - 10, foot_y + 20, foot_x + 20, foot_y - 10), fill=EQUIP_COLOR, width=8)
        frames.append(img)
    save_gif(frames, "leg_press.gif")

def create_leg_extension():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((60, 140, 120, 140), fill=EQUIP_COLOR, width=8) # Seat
        draw.line((60, 140, 60, 80), fill=EQUIP_COLOR, width=8) # Backrest
        draw.line((120, 140, 120, 190), fill=EQUIP_COLOR, width=6) # Front leg
        
        draw.line((70, 90, 70, 130), fill=LINE_COLOR, width=LINE_WIDTH) # Torso
        draw_head(draw, 70, 75)
        draw.line((70, 130, 120, 130), fill=LINE_COLOR, width=LINE_WIDTH) # Thigh
        
        progress = math.sin(i * math.pi / 10)
        angle = math.pi/2 - progress * math.pi/2.5 # from down to horizontal
        foot_x = 120 + 50 * math.sin(angle)
        foot_y = 130 + 50 * math.cos(angle)
        
        draw.line((120, 130, foot_x, foot_y), fill=LINE_COLOR, width=LINE_WIDTH) # Calf
        frames.append(img)
    save_gif(frames, "leg_extension.gif")

def create_leg_curl():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((40, 140, 160, 140), fill=EQUIP_COLOR, width=8) # Bench
        
        draw.line((60, 130, 110, 130), fill=LINE_COLOR, width=LINE_WIDTH) # Torso lying down
        draw_head(draw, 50, 130)
        draw.line((110, 130, 150, 130), fill=LINE_COLOR, width=LINE_WIDTH) # Thigh
        
        progress = math.sin(i * math.pi / 10)
        angle = progress * math.pi/2 # from horizontal up to vertical
        foot_x = 150 - 40 * math.sin(angle)
        foot_y = 130 - 40 * math.cos(angle)
        
        draw.line((150, 130, foot_x, foot_y), fill=LINE_COLOR, width=LINE_WIDTH) # Calf curling up
        frames.append(img)
    save_gif(frames, "leg_curl.gif")

def create_romanian_deadlift():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = math.sin(i * math.pi / 10)
        hip_y = 120
        hip_x = 100 - 20 * progress
        shoulder_y = hip_y - 40 + 40 * progress
        shoulder_x = 100 + 20 * progress
        
        draw.line((hip_x, hip_y, shoulder_x, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x + 5, shoulder_y - 15)
        
        draw.line((hip_x, hip_y, 100, 190), fill=LINE_COLOR, width=LINE_WIDTH) # Straight legs
        
        bar_y = shoulder_y + 40
        draw.line((shoulder_x, shoulder_y, 100, bar_y), fill=LINE_COLOR, width=LINE_WIDTH) # Arms straight down
        draw.line((80, bar_y, 120, bar_y), fill=EQUIP_COLOR, width=6) # Barbell
        frames.append(img)
    save_gif(frames, "romanian_deadlift.gif")

def create_hanging_leg_raise():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((80, 40, 120, 40), fill=EQUIP_COLOR, width=6) # Bar
        
        draw.line((100, 40, 100, 80), fill=LINE_COLOR, width=LINE_WIDTH) # Arms
        draw_head(draw, 100, 70)
        draw.line((100, 80, 100, 130), fill=LINE_COLOR, width=LINE_WIDTH) # Torso
        
        progress = math.sin(i * math.pi / 10)
        angle = progress * math.pi/2 # legs swing up
        foot_x = 100 + 50 * math.sin(angle)
        foot_y = 130 + 50 * math.cos(angle)
        
        draw.line((100, 130, foot_x, foot_y), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "hanging_leg_raise.gif")

if __name__ == "__main__":
    create_running()
    create_walking()
    create_cycling()
    create_dumbbell_fly()
    create_cable_crossover()
    create_lat_pulldown()
    create_face_pull()
    create_front_raise()
    create_hammer_curl()
    create_triceps_pushdown()
    create_leg_press()
    create_leg_extension()
    create_leg_curl()
    create_romanian_deadlift()
    create_hanging_leg_raise()
    print("12 new gym exercise GIFs generated successfully.")