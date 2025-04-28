package fiji.plugin.trackmate.detection.onestep;

import fiji.plugin.trackmate.detection.SpotDetectorFactoryBase;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 *
 * Interface for <b>one-step tracking</b> algorithm factories.
 * <p>
 * These algorithms perform tracking in one step: they take the image as input
 * and directly return the tracks as output. This is different from the two-step
 * detection / linking paradigm that has been in use in TrackMate until the
 * introduction of this factory.
 *
 * @author Jean-Yves Tinevez
 *
 * @param <T>
 *            the type of pixels in the source image.
 */
public interface SpotDetectorTrackerFactory< T extends RealType< T > & NativeType< T > > extends SpotDetectorFactoryBase< T >
{

	/**
	 * Returns a new {@link SpotDetectorTracker} configured to operate the
	 * source image and return the tracking results in one step. This factory
	 * must be first given the <code>ImgPlus</code> and the settings map,
	 * through the <code>#setTarget(ImgPlus, Map)</code> method.
	 *
	 * @param interval
	 *            the interval that determines the region in the source image to
	 *            operate on. This must <b>not</b> have a dimension for time
	 *            (<i>e.g.</i> if the source image is 2D+T (3D), then the
	 *            interval must be 2D; if the source image is 3D without time,
	 *            then the interval must be 3D).
	 */
	public SpotDetectorTracker< T > getDetectorTracker( final Interval interval );
}
