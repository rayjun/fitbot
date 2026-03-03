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

def benchpress():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((40, 150, 160, 150), fill=EQUIP_COLOR, width=8)
        draw.line((60, 140, 140, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 50, 140)
        draw.line((140, 140, 160, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((160, 160, 160, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        progress = math.sin(i * math.pi / 10)
        bar_y = 110 - 40 * progress
        elbow_x, elbow_y = 80, 140 - 20 * (1 - progress)
        draw.line((70, 140, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, 80, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((40, bar_y, 120, bar_y), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "benchpress.gif")

def pushup():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        progress = math.sin(i * math.pi / 10)
        body_y_offset = 30 * (1 - progress)
        shoulder_x, shoulder_y = 60, 120 + body_y_offset
        draw.line((shoulder_x, shoulder_y, 160, 175), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, shoulder_x - 15, shoulder_y - 10)
        elbow_x, elbow_y = 60 + 15 * (1 - progress), 150 + 15 * (1 - progress)
        draw.line((shoulder_x, shoulder_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, 60, 175), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "pushup.gif")

def incline_press():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((60, 160, 120, 100), fill=EQUIP_COLOR, width=8)
        draw.line((120, 100, 120, 160), fill=EQUIP_COLOR, width=8)
        draw.line((70, 145, 120, 95), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 60, 135)
        draw.line((120, 95, 150, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((150, 140, 150, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        progress = math.sin(i * math.pi / 10)
        base_x, base_y = 95, 120
        push_dist = 40 * progress
        bar_x, bar_y = base_x - push_dist * 0.7, base_y - push_dist * 0.7 - 20
        elbow_x, elbow_y = base_x + 10 * (1 - progress), base_y + 10 * (1 - progress)
        draw.line((base_x, base_y, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, bar_x, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((bar_x - 30, bar_y + 30, bar_x + 30, bar_y - 30), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "incline_press.gif")

def pullup():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((60, 40, 140, 40), fill=EQUIP_COLOR, width=6)
        progress = math.sin(i * math.pi / 10)
        body_y = 110 - 40 * progress
        draw.line((100, body_y, 100, body_y + 60), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, body_y - 15)
        draw.line((100, body_y + 60, 90, body_y + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, body_y + 60, 110, body_y + 90), fill=LINE_COLOR, width=LINE_WIDTH)
        elbow_y = body_y + 20 * (1 - progress)
        draw.line((100, body_y, 120 + 10 * progress, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((120 + 10 * progress, elbow_y, 120, 40), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, body_y, 80 - 10 * progress, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((80 - 10 * progress, elbow_y, 80, 40), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "pullup.gif")

def row():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((120, 130, 70, 90), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 60, 75)
        draw.line((120, 130, 130, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((130, 160, 120, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        progress = math.sin(i * math.pi / 10)
        bar_x, bar_y = 70 + 20 * progress, 140 - 30 * progress
        elbow_x, elbow_y = 90 + 20 * progress, 90 - 20 * progress
        draw.line((70, 90, elbow_x, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((elbow_x, elbow_y, bar_x, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((bar_x - 20, bar_y - 5, bar_x + 20, bar_y + 5), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "row.gif")

def deadlift():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = math.sin(i * math.pi / 10)
        hip_y = 140 - 40 * progress
        shoulder_y = hip_y - 40
        draw.line((100, hip_y, 100, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, shoulder_y - 15)
        draw.line((100, hip_y, 120, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((120, 160, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        bar_y = 180 - 80 * progress
        draw.line((100, shoulder_y, 100, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((70, bar_y, 130, bar_y), fill=EQUIP_COLOR, width=8)
        frames.append(img)
    save_gif(frames, "deadlift.gif")

def overhead_press():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((100, 100, 100, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, 85)
        draw.line((100, 160, 90, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 160, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        progress = math.sin(i * math.pi / 10)
        bar_y = 80 - 60 * progress
        draw.line((100, 100, 120, bar_y + 20), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((120, bar_y + 20, 120, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 100, 80, bar_y + 20), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((80, bar_y + 20, 80, bar_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((60, bar_y, 140, bar_y), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "overhead_press.gif")

def lateral_raise():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((100, 100, 100, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, 85)
        draw.line((100, 160, 90, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 160, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        progress = math.sin(i * math.pi / 10)
        angle = progress * math.pi / 2.2
        for side in [-1, 1]:
            hand_x = 100 + side * 70 * math.sin(angle)
            hand_y = 100 - 70 * math.cos(angle + math.pi/2)
            draw.line((100, 100, hand_x, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
            draw.ellipse((hand_x-5, hand_y-5, hand_x+5, hand_y+5), fill=EQUIP_COLOR)
        frames.append(img)
    save_gif(frames, "lateral_raise.gif")

def bicep_curl():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((100, 80, 100, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, 65)
        draw.line((100, 140, 90, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, 140, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        progress = math.sin(i * math.pi / 10)
        angle = math.pi/2 + progress * math.pi/1.5
        hand_x = 100 + 40 * math.cos(angle)
        hand_y = 80 + 40 * math.sin(angle)
        draw.line((100, 80, 100, 120), fill=LINE_COLOR, width=LINE_WIDTH) # upper arm
        draw.line((100, 120, hand_x, hand_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((hand_x-15, hand_y, hand_x+15, hand_y), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "bicep_curl.gif")

def tricep_dips():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((70, 120, 70, 180), fill=EQUIP_COLOR, width=8)
        draw.line((130, 120, 130, 180), fill=EQUIP_COLOR, width=8)
        progress = math.sin(i * math.pi / 10)
        body_y = 80 + 30 * progress
        draw.line((100, body_y, 100, body_y + 60), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, body_y - 15)
        draw.line((100, body_y + 60, 110, body_y + 100), fill=LINE_COLOR, width=LINE_WIDTH)
        elbow_y = body_y + 20
        draw.line((100, body_y, 120, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((120, elbow_y, 130, 120), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, body_y, 80, elbow_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((80, elbow_y, 70, 120), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "tricep_dips.gif")

def squat():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = math.sin(i * math.pi / 10)
        hip_y = 120 + 30 * progress
        shoulder_y = hip_y - 40
        draw.line((100, hip_y, 100, shoulder_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, shoulder_y - 15)
        knee_x, knee_y = 120 + 20 * progress, 150 + 15 * progress
        draw.line((100, hip_y, knee_x, knee_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((knee_x, knee_y, 110, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((shoulder_y - 10, shoulder_y, shoulder_y + 30, shoulder_y), fill=EQUIP_COLOR, width=6)
        frames.append(img)
    save_gif(frames, "squat.gif")

def lunge():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = math.sin(i * math.pi / 10)
        body_y = 90 + 30 * progress
        draw.line((100, body_y, 100, body_y + 40), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, body_y - 15)
        knee_y = body_y + 60 + 20 * progress
        draw.line((100, body_y + 40, 130 + 10 * progress, knee_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((130 + 10 * progress, knee_y, 140, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((100, body_y + 40, 70, body_y + 60 + 30 * progress), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((70, body_y + 60 + 30 * progress, 50, 190), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "lunge.gif")

def calf_raise():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = math.sin(i * math.pi / 10)
        body_y = 100 - 20 * progress
        draw.line((100, body_y, 100, body_y + 60), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 100, body_y - 15)
        draw.line((100, body_y + 60, 100, 190 - 10 * progress), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((90, 190 - 10 * progress, 110, 190 - 10 * progress), fill=EQUIP_COLOR, width=4)
        frames.append(img)
    save_gif(frames, "calf_raise.gif")

def situp():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        progress = math.sin(i * math.pi / 10)
        angle = progress * math.pi / 2.5
        torso_x = 100 - 60 * math.cos(angle)
        torso_y = 180 - 60 * math.sin(angle)
        draw.line((100, 180, torso_x, torso_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, torso_x, torso_y - 15)
        draw.line((100, 180, 140, 160), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((140, 160, 160, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "situp.gif")

def crunches():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        progress = math.sin(i * math.pi / 10)
        angle = progress * math.pi / 6
        torso_x = 100 - 60 * math.cos(angle)
        torso_y = 180 - 60 * math.sin(angle)
        draw.line((100, 180, torso_x, torso_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, torso_x, torso_y - 15)
        draw.line((100, 180, 140, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((140, 140, 160, 180), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "crunches.gif")

def russian_twist():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        progress = math.sin(i * math.pi / 10)
        draw.line((100, 160, 60, 120), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 60, 105)
        draw.line((100, 160, 140, 140), fill=LINE_COLOR, width=LINE_WIDTH)
        ball_x = 100 + 40 * progress
        draw.ellipse((ball_x-10, 140, ball_x+10, 160), fill=EQUIP_COLOR)
        frames.append(img)
    save_gif(frames, "russian_twist.gif")

def plank():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        progress = math.sin(i * math.pi / 5)
        hip_y = 160 - 2 * progress
        draw.line((60, 150, 110, hip_y), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((110, hip_y, 160, 175), fill=LINE_COLOR, width=LINE_WIDTH)
        draw_head(draw, 45, 145)
        draw.line((60, 150, 60, 175), fill=LINE_COLOR, width=LINE_WIDTH)
        draw.line((60, 175, 80, 175), fill=LINE_COLOR, width=LINE_WIDTH)
        frames.append(img)
    save_gif(frames, "plank.gif")

def burpee():
    frames = []
    for i in range(20):
        img, draw = create_frame()
        draw.line((20, 180, 180, 180), fill=EQUIP_COLOR, width=4)
        state = i / 20.0
        if state < 0.25:
            p = state / 0.25
            hip_y = 100 + 40 * p
            draw.line((100, hip_y, 100, hip_y - 40), fill=LINE_COLOR, width=LINE_WIDTH)
            draw_head(draw, 100, hip_y - 55)
        elif state < 0.5:
            p = (state - 0.25) / 0.25
            draw.line((80, 150, 160, 175), fill=LINE_COLOR, width=LINE_WIDTH)
            draw_head(draw, 65, 140)
        elif state < 0.75:
            p = (state - 0.5) / 0.25
            draw.line((100, 140, 100, 180), fill=LINE_COLOR, width=LINE_WIDTH)
            draw_head(draw, 100, 125)
        else:
            p = math.sin((state - 0.75) / 0.25 * math.pi)
            jump_h = 30 * p
            draw.line((100, 100-jump_h, 100, 160-jump_h), fill=LINE_COLOR, width=LINE_WIDTH)
            draw_head(draw, 100, 85-jump_h)
        frames.append(img)
    save_gif(frames, "burpee.gif")

if __name__ == "__main__":
    benchpress(); pushup(); incline_press(); pullup(); row(); deadlift(); overhead_press()
    lateral_raise(); bicep_curl(); tricep_dips(); squat(); lunge(); calf_raise()
    situp(); crunches(); russian_twist(); plank(); burpee()
    print("All 18 GIFs generated successfully.")
