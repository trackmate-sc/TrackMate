package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotMesh;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.LabelImgExporter;
import fiji.plugin.trackmate.util.TMUtils;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.mesh.alg.TaubinSmoothing;
import net.imagej.mesh.nio.BufferMesh;
import net.imglib2.Interval;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.img.display.imagej.CalibrationUtils;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

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

	private final ImgPlus< T > img;

	private final Settings settings;

	private final boolean simplify;

	private List< Spot > spots;

	private final Interval interval;

	private final double[] calibration;

	public Process2DZ( final ImgPlus< T > img, final Interval interval, final double[] calibration, final Settings settings, final boolean simplifyMeshes )
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
		if ( img.dimensionIndex( Axes.Z ) < 0 || img.dimension( img.dimensionIndex( Axes.Z ) ) < 2 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source image is not 3D.\n";
			return false;
		}
		if ( img.dimensionIndex( Axes.TIME ) > 0 && img.dimension( img.dimensionIndex( Axes.TIME ) ) > 1 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source image has more than one time-point.\n";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		spots = null;
		// Make the final single T 3D image, a 2D + T image final by making Z -> T
		final ImagePlus imp = ImageJFunctions.wrap( img, null );
		final int nChannels = ( int ) ( img.dimensionIndex( Axes.CHANNEL ) < 0 ? 1 : img.dimension( img.dimensionIndex( Axes.CHANNEL ) ) );
		final int nSlices = 1; // We force 2D.
		final int nFrames = ( int ) img.dimension( img.dimensionIndex( Axes.Z ) );
		imp.setDimensions( nChannels, nSlices, nFrames );
		CalibrationUtils.copyCalibrationToImagePlus( img, imp );

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
		final double[] calibration = TMUtils.getSpatialCalibration( lblImp );
		final ImgPlus< T > lblImg = TMUtils.rawWraps( lblImp );
		final LabelImageDetector< T > detector = new LabelImageDetector<>( lblImg, lblImg, calibration, simplify );
		if ( !detector.checkInput() || !detector.process() )
		{
			errorMessage = BASE_ERROR_MESSAGE + detector.getErrorMessage();
			return false;
		}
		
		final List< Spot > results = detector.getResult();
		spots = new ArrayList<>( results.size() );
		for ( final Spot spot : results )
		{
			if ( !spot.getClass().isAssignableFrom( SpotMesh.class ) )
				continue;
			
			final SpotMesh sm = ( SpotMesh ) spot;
			final BufferMesh out = TaubinSmoothing.smooth( sm.getMesh() );
			spots.add( new SpotMesh( out, spot.getFeature( Spot.QUALITY ) ) );
		}
		return true;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}
}
