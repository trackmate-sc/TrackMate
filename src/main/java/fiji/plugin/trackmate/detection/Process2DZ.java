package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.LabelImgExporter;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.mesh.alg.TaubinSmoothing;
import net.imagej.mesh.nio.BufferMesh;
import net.imagej.mesh.obj.transform.TranslateMesh;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * A {@link SpotDetector} for 3D images that work by running a spot segmentation
 * algorithm on 2D slices, and merging results using a tracker. This yield a
 * label image that is then converted to 3D meshes using the
 * {@link LabelImageDetector}.
 * <p>
 * This is a convenience class, made to be used in specialized
 * {@link SpotDetectorFactory} with specific choices of detector and merging
 * strategy.
 * 
 * @author Jean-Yves Tinevez, 2023
 *
 * @param <T>
 */
public class Process2DZ< T extends RealType< T > & NativeType< T > >
		extends MultiThreadedBenchmarkAlgorithm
		implements SpotDetector< T >
{

	private static final String BASE_ERROR_MESSAGE = "[Process2DZ] ";

	private final RandomAccessible< T > img;

	private final Interval interval;

	private final double[] calibration;

	private final Settings settings;

	private final boolean simplify;

	private List< Spot > spots;

	/**
	 * Creates a new {@link Process2DZ} detector.
	 * 
	 * @param img
	 *            the input data. Must be 3D and the 3 dimensions must be X, Y
	 *            and Z.
	 * @param interval
	 *            the interval in the input data to process. Must have the same
	 *            number of dimensions that the input data.
	 * @param calibration
	 *            the pixel size array.
	 * @param settings
	 *            a TrackMate settings object, configured to operate on the
	 *            (cropped) input data as if it was a 2D+T image.
	 * @param simplifyMeshes
	 *            whether or not to smooth and simplify meshes resulting from
	 *            merging the 2D contours.
	 */
	public Process2DZ(
			final RandomAccessible< T > img,
			final Interval interval,
			final double[] calibration,
			final Settings settings,
			final boolean simplifyMeshes )
	{
		this.img = img;
		this.interval = interval;
		this.calibration = calibration;
		this.settings = settings;
		this.simplify = simplifyMeshes;
	}

	@Override
	public boolean checkInput()
	{
		if ( img.numDimensions() != 3 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source image is not 3D.\n";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		spots = null;

		// Make the final single T 3D image, a 2D + T image final by making Z->T
		final IntervalView< T > cropped = Views.interval( img, interval );
		final ImagePlus imp = ImageJFunctions.wrap( cropped, null );
		final int nFrames = ( int ) interval.dimension( 2 );
		final int nChannels = ( interval.numDimensions() > 3 ) ? ( int ) interval.dimension( 3 ) : 1;
		imp.setDimensions( nChannels, 1, nFrames );
		imp.getCalibration().pixelWidth = calibration[ 0 ];
		imp.getCalibration().pixelHeight = calibration[ 1 ];
		imp.getCalibration().pixelDepth = calibration[ 2 ];

		// Execute segmentation and tracking.
		final Settings settingsFrame = settings.copyOn( imp );
		final TrackMate trackmate = new TrackMate( settingsFrame );
		trackmate.setNumThreads( numThreads );
		trackmate.getModel().setLogger( Logger.VOID_LOGGER );
		if ( !trackmate.checkInput() || !trackmate.process() )
		{
			errorMessage = BASE_ERROR_MESSAGE + trackmate.getErrorMessage();
			return false;
		}

		// Get 2D+T masks
		final ImagePlus lblImp = LabelImgExporter.createLabelImagePlus( trackmate, false, true, false );

		// Back to a 3D single time-point image.
		lblImp.setDimensions( lblImp.getNChannels(), lblImp.getNFrames(), lblImp.getNSlices() );

		// Convert labels to 3D meshes.
		final ImgPlus< T > lblImg = TMUtils.rawWraps( lblImp );
		final LabelImageDetector< T > detector = new LabelImageDetector<>( lblImg, lblImg, calibration, simplify );
		if ( !detector.checkInput() || !detector.process() )
		{
			errorMessage = BASE_ERROR_MESSAGE + detector.getErrorMessage();
			return false;
		}

		final List< Spot > results = detector.getResult();
		spots = new ArrayList<>( results.size() );

		final RandomAccess< T > ra = lblImg.randomAccess();
		final TrackModel tm = trackmate.getModel().getTrackModel();

		for ( final Spot spot : results )
		{
			if ( !spot.getClass().isAssignableFrom( SpotMesh.class ) )
				continue;

			/*
			 * Smooth spot?
			 */

			final Spot newSpot;
			if ( simplify )
			{

				final SpotMesh sm = ( SpotMesh ) spot;
				final BufferMesh out = TaubinSmoothing.smooth( TranslateMesh.translate( sm.getMesh(), sm ) );
				newSpot = SpotMeshUtils.meshToSpotMesh( out, simplify, new double[] { 1., 1., 1. }, null, new double[] { 0., 0., 0. } );
				if ( newSpot == null )
					continue;
			}
			else
			{
				newSpot = spot;
			}

			/*
			 * Try to get quality from the tracks resulting from the 2D+T image.
			 */

			// Position RA where the spot is.
			for ( int d = 0; d < 3; d++ )
				ra.setPosition( Math.round( spot.getDoublePosition( d ) / calibration[ d ] ), d );

			// Read track ID from label value.
			final int trackID = ( int ) ra.get().getRealDouble() - 1;

			// Average quality from the corresponding track.
			/*
			 * FIXME: Does not work if zmin in the interval is not 0. Probably
			 * because the spots do not 'touch' the right label.
			 */
			final double avgQuality = tm.trackSpots( trackID ).stream().mapToDouble( s -> s.getFeature( Spot.QUALITY ).doubleValue() ).average().getAsDouble();

			// Pass quality to new spot.
			newSpot.putFeature( Spot.QUALITY, Double.valueOf( avgQuality ) );

			// Shift them by interval min.
			for ( int d = 0; d < 3; d++ )
				newSpot.move( interval.min( d ) * calibration[ d ], d );

			spots.add( newSpot );
		}
		return true;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}
}
