from PIL import Image

def process_image(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    data = img.getdata()
    
    new_data = []
    for item in data:
        # It's grayscale, so r=g=b
        # If it's white (255), we want alpha=0.
        # If it's black (0), we want alpha=255.
        # So alpha = 255 - r
        # We set color to pure white (255, 255, 255) so Compose can tint it easily.
        r = item[0]
        alpha = 255 - r
        
        # To remove noise, if alpha is very low, just make it 0
        if alpha < 20:
            alpha = 0
            
        new_data.append((255, 255, 255, alpha))
        
    img.putdata(new_data)
    img.save(output_path)
    print(f"Saved {output_path}")

process_image("app/src/commonMain/composeResources/drawable/anatomy_front.png", "app/src/commonMain/composeResources/drawable/anatomy_front_lines.png")
process_image("app/src/commonMain/composeResources/drawable/anatomy_back.png", "app/src/commonMain/composeResources/drawable/anatomy_back_lines.png")
