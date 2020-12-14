package fiji.plugin.trackmate.detection;

import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * For detectors that process one time-point at a time, independently, and for
 * which we can therefore propose multithreading. *
 * <p>
 * These classes are able to configure a {@link SpotDetector} to operate on a
 * single time-point of the target {@link ImgPlus}.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 */
public interface SpotDetectorFactory< T extends RealType< T > & NativeType< T > > extends SpotDetectorFactoryBase< T >
{

	/**
	 * Returns a new {@link SpotDetector} configured to operate on the given
	 * target frame. This factory must be first given the {@link ImgPlus} and
	 * the settings map, through the {@link #setTarget(ImgPlus, Map)} method.
	 *
	 * @param interval
	 *            the interval that determines the region in the source image to
	 *            operate on. This must <b>not</b> have a dimension for time
	 *            (<i>e.g.</i> if the source image is 2D+T (3D), then the
	 *            interval must be 2D; if the source image is 3D without time,
	 *            then the interval must be 3D).
	 * @param frame
	 *            the frame index in the source image to operate on
	 */
	public SpotDetector< T > getDetector( final Interval interval, int frame );
}
