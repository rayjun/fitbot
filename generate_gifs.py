from PIL import Image, ImageDraw
import os

def create_stickman_frame(draw, action, phase, cx=150, cy=150):
    # Action defines the exercise, phase defines the animation step (0 to 1)
    
    # Head
    draw.ellipse((cx-20, cy-70, cx+20, cy-30), fill="black")
    # Body
    draw.line((cx, cy-30, cx, cy+30), fill="black", width=5)
    
    if action == "running":
        # Arms
        arm_swing = 30 * (-1 if phase < 0.5 else 1)
        draw.line((cx, cy-10, cx-30, cy+10+arm_swing), fill="black", width=4)
        draw.line((cx, cy-10, cx+30, cy+10-arm_swing), fill="black", width=4)
        # Legs
        leg_swing = 40 * (-1 if phase < 0.5 else 1)
        draw.line((cx, cy+30, cx-20, cy+80+leg_swing), fill="black", width=5)
        draw.line((cx, cy+30, cx+20, cy+80-leg_swing), fill="black", width=5)

    elif action == "walking":
        # Arms
        arm_swing = 15 * (-1 if phase < 0.5 else 1)
        draw.line((cx, cy-10, cx-20, cy+10+arm_swing), fill="black", width=4)
        draw.line((cx, cy-10, cx+20, cy+10-arm_swing), fill="black", width=4)
        # Legs
        leg_swing = 20 * (-1 if phase < 0.5 else 1)
        draw.line((cx, cy+30, cx-15, cy+80+leg_swing), fill="black", width=5)
        draw.line((cx, cy+30, cx+15, cy+80-leg_swing), fill="black", width=5)

    elif action == "cycling":
        # Bike frame
        draw.line((cx-40, cy+50, cx+40, cy+50), fill="gray", width=3) # bottom tube
        draw.line((cx-40, cy+50, cx-20, cy-10), fill="gray", width=3) # seat tube
        draw.line((cx+40, cy+50, cx+20, cy-10), fill="gray", width=3) # front tube
        draw.line((cx-20, cy-10, cx+20, cy-10), fill="gray", width=3) # top tube
        
        # Wheels
        draw.ellipse((cx-70, cy+20, cx-10, cy+80), outline="gray", width=3)
        draw.ellipse((cx+10, cy+20, cx+70, cy+80), outline="gray", width=3)
        
        # Stickman
        draw.ellipse((cx-30, cy-80, cx+10, cy-40), fill="black") # head leaning forward
        draw.line((cx-10, cy-40, cx-20, cy-10), fill="black", width=5) # body leaning
        draw.line((cx-10, cy-40, cx+20, cy-20), fill="black", width=4) # arm to handlebar
        
        # Pedaling legs
        pedal_y = 20 * (-1 if phase < 0.5 else 1)
        draw.line((cx-20, cy-10, cx, cy+50+pedal_y), fill="black", width=5)
        draw.line((cx-20, cy-10, cx, cy+50-pedal_y), fill="black", width=5)

def create_exercise_gif(filename, action, num_frames=10, duration=100):
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    frames = []
    for i in range(num_frames):
        img = Image.new("RGB", (300, 300), "white")
        draw = ImageDraw.Draw(img)
        phase = i / num_frames
        create_stickman_frame(draw, action, phase)
        frames.append(img)
    
    frames[0].save(
        filename,
        save_all=True,
        append_images=frames[1:],
        duration=duration,
        loop=0
    )

create_exercise_gif('app/src/main/assets/exercises/running.gif', 'running')
create_exercise_gif('app/src/main/assets/exercises/brisk_walking.gif', 'walking')
create_exercise_gif('app/src/main/assets/exercises/cycling.gif', 'cycling')

