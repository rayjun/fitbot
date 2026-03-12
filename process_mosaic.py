import os
from PIL import Image

def pixelate(img, block_size):
    small = img.resize(
        (img.width // block_size, img.height // block_size),
        resample=Image.Resampling.NEAREST
    )
    result = small.resize(
        img.size,
        resample=Image.Resampling.NEAREST
    )
    return result

def process_image(input_path, output_path, block_size=18):
    input_path = os.path.expanduser(input_path)
    img = Image.open(input_path).convert("RGBA")
    
    # Extract lines and make background transparent
    # We'll make the lines white so they can be tinted
    data = img.getdata()
    new_data = []
    for item in data:
        r = item[0]
        alpha = 255 - r
        if alpha < 30:
            alpha = 0
        new_data.append((255, 255, 255, alpha))
        
    img.putdata(new_data)
    
    # Apply mosaic effect
    mosaic_img = pixelate(img, block_size)
    
    mosaic_img.save(output_path)
    print(f"Saved {output_path}")

process_image("~/Desktop/2.png", "app/src/commonMain/composeResources/drawable/anatomy_front_mosaic.png")
process_image("~/Desktop/1.png", "app/src/commonMain/composeResources/drawable/anatomy_back_mosaic.png")
