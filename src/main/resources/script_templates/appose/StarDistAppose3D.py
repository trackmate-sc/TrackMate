from stardist.models import StarDist3D
from csbdeep.utils import normalize
import numpy as np
import appose
from pathlib import Path
from scipy.ndimage import zoom

# Model expected nucleus sizes (in pixels)
MODEL_SIZES = {
    'confocal': {'xy': 39, 'z': 7},
    'sospim': {'xy': 27.5, 'z': 10},  # avg of 27,28
    'spinning': {'xy': 39, 'z': 7}
}

def process(img, axes, model_name, prob_thresh, nms_thresh, normalize_input, target_channel,
            expected_diameter_xy, expected_diameter_z, pixel_size_xy, pixel_size_z):
    """
    Segment 3D nuclei using StarDist with automatic volume scaling.

    Parameters
    ----------
    img : ndarray
        Input image array
    axes : dict
        Mapping of axis names to indices
    model_name : str
        Name of the pretrained model to use
    prob_thresh : float
        Probability threshold for object detection
    nms_thresh : float
        Non-maximum suppression threshold
    normalize_input : bool
        Whether to normalize the input image
    target_channel : int
        Channel index to process (1-based)
    expected_diameter_xy : float
        Expected nucleus diameter in XY (in pixels)
    expected_diameter_z : float
        Expected nucleus diameter in Z (in pixels)
    pixel_size_xy : float
        Pixel size in XY (physical units)
    pixel_size_z : float
        Pixel size in Z (physical units)

    Returns
    -------
    labels : ndarray
        Integer array where each nucleus has a unique ID
    """

    task.update(f"Original image shape: {img.shape}")
    task.update(f"Axes mapping: " + str(axes))

    # Calculate scaling factors
    model_size = MODEL_SIZES.get(model_name, {'xy': 35, 'z': 8})
    scale_xy = model_size['xy'] / expected_diameter_xy
    scale_z = model_size['z'] / expected_diameter_z

    task.update(f"Model expects nucleus size: XY={model_size['xy']}px, Z={model_size['z']}px")
    task.update(f"User expects nucleus size: XY={expected_diameter_xy}px, Z={expected_diameter_z}px")
    task.update(f"Scaling factors: XY={scale_xy:.3f}, Z={scale_z:.3f}")

    # Handle multi-channel images
    if 'Channel' in axes:
        c_axis = axes['Channel']
        # Convert from 1-based to 0-based indexing
        channel_idx = target_channel - 1
        # Select the target channel
        img = np.take(img, channel_idx, axis=c_axis)
        task.update(f"Selected channel {target_channel}, new shape: {img.shape}")

    # Handle time series - process each timepoint separately
    if 'Time' in axes:
        t_axis = axes['Time']
        num_timepoints = img.shape[t_axis]
        task.update(f"Processing {num_timepoints} timepoints")

        # Load the model once
        task.update(f"Loading StarDist model: {model_name}")
        # Models are in the starfun3d/models directory
        model_path = Path.home() / "code" / "trackmate" / "starfun3d" / "models" / model_name
        if not model_path.exists():
            # Fallback to models in current directory
            model_path = Path("models") / model_name
        model = StarDist3D(None, name=model_name, basedir=model_path.parent)

        # Process each timepoint
        all_labels = []
        for t in range(num_timepoints):
            task.update(f"Processing timepoint {t+1}/{num_timepoints}")

            # Extract single timepoint
            frame = np.take(img, t, axis=t_axis)

            # Scale the volume to match model expectations
            # Determine which axes are Z, Y, X
            z_axis, y_axis, x_axis = None, None, None
            for name, idx in axes.items():
                if name == 'Z': z_axis = idx
                elif name == 'Y': y_axis = idx
                elif name == 'X': x_axis = idx
                elif name == 'Time' and idx <= t_axis:
                    # Adjust for removed time dimension
                    if z_axis is not None and z_axis > idx: z_axis -= 1
                    if y_axis is not None and y_axis > idx: y_axis -= 1
                    if x_axis is not None and x_axis > idx: x_axis -= 1

            # Build zoom factors for each dimension
            zoom_factors = [1.0] * frame.ndim
            if z_axis is not None: zoom_factors[z_axis] = scale_z
            if y_axis is not None: zoom_factors[y_axis] = scale_xy
            if x_axis is not None: zoom_factors[x_axis] = scale_xy

            task.update(f"Scaling volume with factors: {zoom_factors}")
            frame_scaled = zoom(frame, zoom_factors, order=1)  # Linear interpolation
            task.update(f"Scaled shape: {frame.shape} -> {frame_scaled.shape}")

            # Normalize if requested
            if normalize_input:
                frame_scaled = normalize(frame_scaled, 1, 99.8)

            # Predict
            labels, details = model.predict_instances(
                frame_scaled,
                prob_thresh=prob_thresh,
                nms_thresh=nms_thresh
            )

            # Scale labels back to original size
            inverse_zoom = [1.0/z for z in zoom_factors]
            labels_original = zoom(labels.astype(np.float32), inverse_zoom, order=0)  # Nearest neighbor
            labels_original = labels_original.astype(labels.dtype)

            all_labels.append(labels_original)
            task.update(f"Timepoint {t+1}: detected {labels_original.max()} objects")

        # Stack all timepoints back together
        masks = np.stack(all_labels, axis=t_axis)
        task.update(f"Final mask shape: {masks.shape}")

    else:
        # Single timepoint - process directly
        task.update(f"Processing single 3D volume")
        task.update(f"Loading StarDist model: {model_name}")

        # Models are in the starfun3d/models directory
        model_path = Path.home() / "code" / "trackmate" / "starfun3d" / "models" / model_name
        if not model_path.exists():
            # Fallback to models in current directory
            model_path = Path("models") / model_name
        model = StarDist3D(None, name=model_name, basedir=model_path.parent)

        # Scale the volume to match model expectations
        # Determine which axes are Z, Y, X
        z_axis, y_axis, x_axis = None, None, None
        for name, idx in axes.items():
            if name == 'Z': z_axis = idx
            elif name == 'Y': y_axis = idx
            elif name == 'X': x_axis = idx

        # Build zoom factors for each dimension
        zoom_factors = [1.0] * img.ndim
        if z_axis is not None: zoom_factors[z_axis] = scale_z
        if y_axis is not None: zoom_factors[y_axis] = scale_xy
        if x_axis is not None: zoom_factors[x_axis] = scale_xy

        task.update(f"Scaling volume with factors: {zoom_factors}")
        img_scaled = zoom(img, zoom_factors, order=1)  # Linear interpolation
        task.update(f"Scaled shape: {img.shape} -> {img_scaled.shape}")

        # Normalize if requested
        if normalize_input:
            img_scaled = normalize(img_scaled, 1, 99.8)

        # Predict
        labels, details = model.predict_instances(
            img_scaled,
            prob_thresh=prob_thresh,
            nms_thresh=nms_thresh
        )

        # Scale labels back to original size
        inverse_zoom = [1.0/z for z in zoom_factors]
        masks = zoom(labels.astype(np.float32), inverse_zoom, order=0)  # Nearest neighbor
        masks = masks.astype(labels.dtype)

        task.update(f"Detected {masks.max()} objects")
        task.update(f"Mask shape: {masks.shape}")

    return masks


# Parse arguments
model_name = "${--model}"
custom_model = "${--custom_model}"

# Use custom model if provided, otherwise use pretrained
if custom_model and custom_model.strip():
    model_path = Path(custom_model)
    if not model_path.exists():
        raise FileNotFoundError(f"Custom model path does not exist: {custom_model}")
    # Load custom model
    model_name = model_path.name
    task.update(f"Using custom model from: {custom_model}")
else:
    task.update(f"Using pretrained model: {model_name}")

prob_thresh = float("${--prob_thresh}")
nms_thresh = float("${--nms_thresh}")
normalize_input = "${--normalize}" == "True"
target_channel = int("${TARGET_CHANNEL}")
expected_diameter_xy = float("${--diameter_xy}")
expected_diameter_z = float("${--diameter_z}")
pixel_size_xy = float("${PIXEL_SIZE_XY}")
pixel_size_z = float("${PIXEL_SIZE_Z}")

# Get input image
input_img = image.ndarray()

# Process
masks = process(
    input_img,
    axes,
    model_name,
    prob_thresh,
    nms_thresh,
    normalize_input,
    target_channel,
    expected_diameter_xy,
    expected_diameter_z,
    pixel_size_xy,
    pixel_size_z
)

# Create shared memory output
shared = appose.NDArray(str(masks.dtype), masks.shape)
shared.ndarray()[:] = masks[:]
task.outputs['masks'] = shared
