/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import net.imagej.axis.Axes;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.mesh.alg.TaubinSmoothing;
import net.imglib2.mesh.impl.nio.BufferMesh;
import net.imglib2.mesh.view.TranslateMesh;
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
 *            the pixel type in the image processed.
 */
public class Process2DZ< T extends RealType< T > & NativeType< T > >
		extends MultiThreadedBenchmarkAlgorithm
		implements SpotDetector< T >
{

	private static final String BASE_ERROR_MESSAGE = "[Process2DZ] ";

	private final ImgPlus< T > img;

	private final Interval interval;

	private final double[] calibration;

	private final Settings settings;

	private final boolean simplify;

	private List< Spot > spots;

	private final double smoothingScale;

	/**
	 * Creates a new {@link Process2DZ} detector.
	 * 
	 * @param img
	 *            the input data. Must be 3D or 4D (3D plus possibly channels)
	 *            and the 3 spatial dimensions must be X, Y and Z.
	 * @param interval
	 *            the interval in the input data to process. Must have the same
	 *            number of dimensions that the input data.
	 * @param calibration
	 *            the pixel size array.
	 * @param settings
	 *            a TrackMate settings object, configured to operate on the
	 *            (cropped) input data as if it was a 2D(+C)+T image.
	 * @param simplifyMeshes
	 *            whether or not to smooth and simplify meshes resulting from
	 *            merging the 2D contours.
	 * @param smoothingScale
	 *            if positive, will smooth the 3D mask by a gaussian of
	 *            specified sigma to yield smooth meshes.
	 */
	public Process2DZ(
			final ImgPlus< T > img,
			final Interval interval,
			final double[] calibration,
			final Settings settings,
			final boolean simplifyMeshes,
			final double smoothingScale )
	{
		this.img = img;
		this.interval = interval;
		this.calibration = calibration;
		this.settings = settings;
		this.simplify = simplifyMeshes;
		this.smoothingScale = smoothingScale;
	}

	@Override
	public boolean checkInput()
	{
		if ( !( img.numDimensions() == 3 || img.numDimensions() == 4 ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source image is not 3D or 4D, but " + img.numDimensions() + "D.\n";
			return false;
		}
		if ( img.dimensionIndex( Axes.TIME ) >= 0 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source image has a time dimension, but should not.\n";
			return false;
		}
		if ( img.dimensionIndex( Axes.Z ) < 0 )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Source image does not have a Z dimension.\n";
			return false;
		}
		if ( interval.numDimensions() != img.numDimensions() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Provided interval does not have the same dimensionality that of the source image. "
					+ "Interval is " + interval.numDimensions() + "D and the image is " + img.numDimensions() + "D.\n";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		spots = null;

		/*
		 * Segment and track as a 2D+T image with the specified detector and
		 * settings.
		 */

		// Make the final single T 3D image, a 2D + T image final by making Z->T
		final IntervalView< T > cropped = Views.interval( img, interval );
		final ImagePlus imp = ImageJFunctions.wrap( cropped, null );
		final int nFrames = ( int ) interval.dimension( img.dimensionIndex( Axes.Z ) );
		final int cDim = img.dimensionIndex( Axes.CHANNEL );
		final int nChannels = cDim < 0 ? 1 : ( int ) interval.dimension( cDim );
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

		/*
		 * Exposes tracked labels as a 3D image and segment them again with
		 * label image detector.
		 */

		// Back to a 3D single time-point image.
		lblImp.setDimensions( lblImp.getNChannels(), lblImp.getNFrames(), lblImp.getNSlices() );

		// Convert labels to 3D meshes.
		final ImgPlus< T > lblImg = TMUtils.rawWraps( lblImp );
		final LabelImageDetector< T > detector = new LabelImageDetector<>(
				lblImg,
				lblImg,
				calibration,
				simplify,
				smoothingScale );
		if ( !detector.checkInput() || !detector.process() )
		{
			errorMessage = BASE_ERROR_MESSAGE + detector.getErrorMessage();
			return false;
		}

		final List< Spot > results = detector.getResult();
		spots = new ArrayList<>( results.size() );

		// To read the label value (=trackID) later.
		final RandomAccess< T > ra = lblImg.randomAccess();
		final TrackModel tm = trackmate.getModel().getTrackModel();

		for ( final Spot spot : results )
		{

			/*
			 * Smooth spot?
			 */

			final Spot newSpot;
			if ( !simplify || !spot.getClass().isAssignableFrom( SpotMesh.class ) )
			{
				newSpot = spot;
			}
			else
			{
				final SpotMesh sm = ( SpotMesh ) spot;
				final BufferMesh out = TaubinSmoothing.smooth( TranslateMesh.translate( sm.getMesh(), sm ) );
				newSpot = SpotMeshUtils.meshToSpotMesh( out, simplify, new double[] { 1., 1., 1. }, null, new double[] { 0., 0., 0. } );
				if ( newSpot == null )
					continue;
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
			final Set< Spot > trackSpots = tm.trackSpots( trackID );
			final double avgQuality;
			if ( trackSpots != null )
			{
				avgQuality = trackSpots.stream()
						.mapToDouble( s -> s.getFeature( Spot.QUALITY ).doubleValue() )
						.average()
						.getAsDouble();
			}
			else
			{
				// default if something goes wrong.
				avgQuality = spot.getFeature( Spot.QUALITY );
			}

			// Pass quality to new spot.
			newSpot.putFeature( Spot.QUALITY, Double.valueOf( avgQuality ) );

			// Shift them by interval min.
			newSpot.move( interval.min( img.dimensionIndex( Axes.X ) ) * calibration[ 0 ], 0 );
			newSpot.move( interval.min( img.dimensionIndex( Axes.Y ) ) * calibration[ 1 ], 1 );
			newSpot.move( interval.min( img.dimensionIndex( Axes.Z ) ) * calibration[ 2 ], 2 );

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
