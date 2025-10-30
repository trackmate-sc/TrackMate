from cellpose import models, io
import numpy as np
import appose

def process(img, axes):

    io.logger_setup()
    model = models.Cellpose(model_type='${--pretrained_model}', gpu=${--use_gpu})

    # Transpose so that T is first axis and C is the second axis.
    t_axis = axes['Time']
    c_axis = axes['Channel']
    if t_axis > c_axis:
        c_axis += 1
    # Move T to front
    image_reshaped = np.moveaxis(img, t_axis, 0)
    # Move C to second position
    image_reshaped = np.moveaxis(image_reshaped, c_axis, 1)

    task.update(f"Original image shape: {img.shape}")
    task.update(f"Axes mapping: " + str(axes))
    task.update(f"Reshaped image shape: {image_reshaped.shape}")

    # Run Cellpose on each timepoint
    out = model.eval(image_reshaped, diameter=${--diameter}, channels=[${--chan},${--chan2}], channel_axis=1, progress=True)
    masks = out[0]
    task.update(f"Mask shape: {masks.shape}")
    return masks


input = image.ndarray()
masks = process(input, axes)  

shared = appose.NDArray(str(masks.dtype), masks.shape)
shared.ndarray()[:] = masks[:]
task.outputs['masks'] = shared
