package fiji.plugin.trackmate.detection;

import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface for detector factories that need to process all the time-points at
 * once. They return a detector that will be given all the time-points and will
 * have to manage multi-threading by itself.
 * 
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 */
public interface SpotGlobalDetectorFactory< T extends RealType< T > & NativeType< T > > extends SpotDetectorFactoryBase< T >
{

	/**
	 * Returns a new {@link SpotDetector} configured to operate on all the
	 * time-points. This factory must be first given the {@link ImgPlus} and the
	 * settings map, through the {@link #setTarget(ImgPlus, Map)} method.
	 *
	 * @param interval
	 *            the interval that determines the region in the source image to
	 *            operate on. This must <b>not</b> have a dimension for time
	 *            (<i>e.g.</i> if the source image is 2D+T (3D), then the
	 *            interval must be 2D; if the source image is 3D without time,
	 *            then the interval must be 3D).
	 */
	public SpotGlobalDetector< T > getDetector( final Interval interval );

}
