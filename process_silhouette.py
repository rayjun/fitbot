from PIL import Image, ImageDraw

def process_image(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    
    # 1. Flood fill the exterior white background with transparent
    # We use a trick: create a new image with a 1px border, floodfill from (0,0)
    width, height = img.size
    
    # Create a mask for floodfill
    # To do this reliably, let's threshold first
    # Convert to grayscale to find background
    gray = img.convert("L")
    pixels = gray.load()
    rgba_pixels = img.load()
    
    # Define what is "background" (almost white)
    bg_threshold = 240
    
    # Simple approach: find the bounding box, but the image might have white inside the body.
    # We need a proper flood fill. Pillow's ImageDraw.floodfill works on the image itself.
    # Let's flood fill the corners with a special color (e.g. (0,0,0,0)).
    ImageDraw.floodfill(img, (0, 0), (0, 0, 0, 0), thresh=15)
    ImageDraw.floodfill(img, (width-1, 0), (0, 0, 0, 0), thresh=15)
    ImageDraw.floodfill(img, (0, height-1), (0, 0, 0, 0), thresh=15)
    ImageDraw.floodfill(img, (width-1, height-1), (0, 0, 0, 0), thresh=15)
    
    # Now, all pixels that are NOT (0,0,0,0) belong to the body!
    # Let's turn them all into pure white, with 100% opacity.
    for y in range(height):
        for x in range(width):
            if rgba_pixels[x, y][3] != 0: # If not transparent
                rgba_pixels[x, y] = (255, 255, 255, 255)
            
    img.save(output_path)
    print(f"Saved {output_path}")

process_image("app/src/commonMain/composeResources/drawable/anatomy_front.png", "app/src/commonMain/composeResources/drawable/anatomy_front_mask.png")
process_image("app/src/commonMain/composeResources/drawable/anatomy_back.png", "app/src/commonMain/composeResources/drawable/anatomy_back_mask.png")
