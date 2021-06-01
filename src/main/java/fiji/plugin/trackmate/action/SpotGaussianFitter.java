/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2021 Fiji developers.
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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.fit.Gaussian2DFixedRadius;
import fiji.plugin.trackmate.action.fit.Gaussian3D;
import fiji.plugin.trackmate.action.fit.Gaussian3DFixedRadius;
import fiji.plugin.trackmate.action.fit.MLGaussian2DFixedRadiusEstimator;
import fiji.plugin.trackmate.action.fit.MLGaussian3DEstimator;
import fiji.plugin.trackmate.action.fit.MLGaussian3DFixedRadiusEstimator;
import fiji.plugin.trackmate.detection.DetectionUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.algorithm.localization.FitFunction;
import net.imglib2.algorithm.localization.FunctionFitter;
import net.imglib2.algorithm.localization.Gaussian;
import net.imglib2.algorithm.localization.LevenbergMarquardtSolver;
import net.imglib2.algorithm.localization.MLGaussianEstimator;
import net.imglib2.algorithm.localization.PeakFitter;
import net.imglib2.algorithm.localization.StartPointEstimator;
import net.imglib2.type.numeric.RealType;

public class SpotGaussianFitter extends MultiThreadedBenchmarkAlgorithm
{

	private static final String BASE_ERROR_MSG = "[SpotGaussianFitter] ";

	private final Model model;

	private final Settings settings;

	private final Logger logger;

	private final boolean fixRadius;

	public SpotGaussianFitter( final Model model, final Settings settings, final Logger logger, final boolean fixRadius )
	{
		this.model = model;
		this.settings = settings;
		this.logger = logger;
		this.fixRadius = fixRadius;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Prepare the image.
		 */
		final Object obj = settings.detectorSettings.get( KEY_TARGET_CHANNEL );
		final int channel = ( Integer ) obj - 1;

		@SuppressWarnings( "rawtypes" )
		final ImgPlus img = TMUtils.rawWraps( settings.imp );

		// Do it. Frame by frame.
		final List< Integer > frameSet = new ArrayList<>( model.getSpots().keySet() );
		final int numFrames = frameSet.size();
		for ( int i = 0; i < numFrames; i++ )
		{
			logger.setProgress( ( double ) i / numFrames );
			final int frame = frameSet.get( i );
			@SuppressWarnings( "rawtypes" )
			final ImgPlus imgCT = TMUtils.hyperSlice( img, channel, frame );
			final MultiThreadedBenchmarkAlgorithm algo = spotSubLocalizer( imgCT, model.getSpots().iterable( frame, true ), fixRadius );
			algo.setNumThreads( numThreads );
			if ( !algo.checkInput() || !algo.process() )
			{
				errorMessage = BASE_ERROR_MSG + algo.getErrorMessage();
				logger.setProgress( 1. );
				logger.setStatus( "Error in spot gaussian fitter at frame " + frame );
				final long end = System.currentTimeMillis();
				processingTime = end - start;
				return false;
			}
		}

		logger.setProgress( 1. );
		logger.setStatus( "" );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	public static final < T extends RealType< T > > MultiThreadedBenchmarkAlgorithm spotSubLocalizer( final ImgPlus< T > img, final Iterable< Spot > spots, final boolean fixRadius )
	{
		return new SingleFrameGaussLocalizer<>( img, spots, fixRadius );
	}

	@Override
	public boolean checkInput()
	{
		if ( null == model )
		{
			errorMessage = BASE_ERROR_MSG + "Model object is null.";
			return false;
		}
		if ( null == settings )
		{
			errorMessage = BASE_ERROR_MSG + "Settings object is null.";
			return false;
		}
		if ( null == settings.detectorSettings )
		{
			errorMessage = BASE_ERROR_MSG + "Detector settings map is null.";
			return false;
		}
		final Object obj = settings.detectorSettings.get( KEY_TARGET_CHANNEL );
		if ( obj == null )
		{
			errorMessage = BASE_ERROR_MSG + "Detector settings map lacks a specification of the target channel.";
			return false;
		}
		return true;
	}

	private static final class SingleFrameGaussLocalizer< T extends RealType< T > > extends MultiThreadedBenchmarkAlgorithm
	{

		private final ImgPlus< T > img;

		private final Iterable< Spot > spots;

		private final boolean fixRadius;

		public SingleFrameGaussLocalizer( final ImgPlus< T > img, final Iterable< Spot > spots, final boolean fixRadius )
		{
			this.img = img;
			this.spots = spots;
			this.fixRadius = fixRadius;
		}

		@Override
		public boolean checkInput()
		{
			return true;
		}

		@Override
		public boolean process()
		{
			final double[] calibration = TMUtils.getSpatialCalibration( img );
			final Map< Spot, Localizable > points = new HashMap<>();
			/*
			 * We treat 2D and 3D differently. In 2D we can assume the pixels
			 * are square (hehehehe) and use an isotropic gaussian (fewer
			 * params). In 3D, we cannot, and have to use an orthogonal
			 * gaussian.
			 */
			final StartPointEstimator estimator;
			final FitFunction function;
			if ( DetectionUtils.is2D( img ) )
			{
				double sumSigma = 0.;
				for ( final Spot spot : spots )
				{
					sumSigma += spot.getFeature( Spot.RADIUS ) / calibration[ 0 ] / Math.sqrt( 2. );

					final long[] pixelPos = new long[ 2 ];
					for ( int d = 0; d < 2; d++ )
						pixelPos[ d ] = Math.round( spot.getDoublePosition( d ) / calibration[ d ] );

					points.put( spot, Point.wrap( pixelPos ) );
				}
				final double typicalSigma = sumSigma / points.size();
				if (fixRadius)
				{
					estimator = new MLGaussian2DFixedRadiusEstimator( typicalSigma );
					function = new Gaussian2DFixedRadius();
				}
				else
				{
					estimator = new MLGaussianEstimator( typicalSigma, 2 );
					function = new Gaussian();
				}
			}
			else
			{
				final double[] typicalSigmas = new double[ 3 ];
				for ( final Spot spot : spots )
				{
					final long[] pixelPos = new long[ 3 ];
					for ( int d = 0; d < 3; d++ )
					{
						typicalSigmas[ d ] += spot.getFeature( Spot.RADIUS ) / calibration[ d ] / Math.sqrt( 3. );
						pixelPos[ d ] = Math.round( spot.getDoublePosition( d ) / calibration[ d ] );
					}
					points.put( spot, Point.wrap( pixelPos ) );
				}
				// Average typical sigmas.
				for ( int d = 0; d < 3; d++ )
					typicalSigmas[ d ] /= points.size();

				if ( fixRadius )
				{
					estimator = new MLGaussian3DFixedRadiusEstimator( typicalSigmas );
					function = new Gaussian3DFixedRadius();
				}
				else
				{
					estimator = new MLGaussian3DEstimator( typicalSigmas );
					function = new Gaussian3D();
				}
			}

			final FunctionFitter solver = new LevenbergMarquardtSolver();

			// Execute.
			final PeakFitter< T > fitter = new PeakFitter< T >( img, points.values(), solver, function, estimator );
			fitter.setNumThreads( numThreads );
			if ( !fitter.checkInput() || !fitter.process() )
			{
				errorMessage = fitter.getErrorMessage();
				return false;
			}

			// Update spot position.
			final Map< Localizable, double[] > fitParams = fitter.getResult();
			if ( DetectionUtils.is2D( img ) )
			{
				for ( final Spot spot : spots )
				{
					final Localizable localizable = points.get( spot );
					final double[] fitParam = fitParams.get( localizable );
					final double x0 = fitParam[ 0 ];
					final double y0 = fitParam[ 1 ];
					final double sigma = 1. / Math.sqrt( fitParam[ 3 ] );

					spot.putFeature( Spot.POSITION_X, x0 * calibration[ 0 ] );
					spot.putFeature( Spot.POSITION_Y, y0 * calibration[ 1 ] );
					spot.putFeature( Spot.RADIUS, sigma * calibration[ 0 ] * Math.sqrt( 2 ) );
				}
			}
			else
			{
				for ( final Spot spot : spots )
				{
					final Localizable localizable = points.get( spot );
					final double[] fitParam = fitParams.get( localizable );
					final double x0 = fitParam[ 0 ];
					final double y0 = fitParam[ 1 ];
					final double z0 = fitParam[ 2 ];

					spot.putFeature( Spot.POSITION_X, x0 * calibration[ 0 ] );
					spot.putFeature( Spot.POSITION_Y, y0 * calibration[ 1 ] );
					spot.putFeature( Spot.POSITION_Z, z0 * calibration[ 2 ] );
					if ( !fixRadius )
					{
						final double sigmaX = fitParam[ 4 ];
						spot.putFeature( Spot.RADIUS, sigmaX * calibration[ 0 ] * Math.sqrt( 3 ) );
					}
				}
			}
			return true;
		}
	}

	public static class SpotGaussianFitterAction extends AbstractTMAction
	{

		@Override
		public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
		{
			logger.log( "Refining the position of spots using gaussian fitting.\n" );
			logger.log( "Fitting " + trackmate.getModel().getSpots().getNSpots( true ) + " visible spots using " + "threads.\n" );

			final SpotGaussianFitter fitter = new SpotGaussianFitter( trackmate.getModel(), trackmate.getSettings(), logger, false );
			fitter.setNumThreads( trackmate.getNumThreads() );
			if ( !fitter.checkInput() || !fitter.process() )
			{
				logger.error( fitter.getErrorMessage() );
				return;
			}
			logger.log( String.format( "Fitting completed in %.1f s.\n", ( fitter.getProcessingTime() / 1000. ) ) );
		}
	}

	@Plugin( type = TrackMateActionFactory.class )
	public static class Factory implements TrackMateActionFactory
	{

		public static final String NAME = "Refine spot position with gaussian fitting";

		public static final String KEY = "GAUSS_FIT";

		public static final String INFO_TEXT = "<html>" +
				"This action performs sub-localization of spots using gaussian peak fitting. "
				+ "<p>"
				+ "The fit process will update the spot position and their radius, using the "
				+ "results from the gaussian fit. Of course it works best when the peaks in the image "
				+ "ressemble gaussian functions. The fitting process uses the spots information (position "
				+ "and radius) as initial values for the fit."
				+ "<p>"
				+ "It works for both 2D and 3D images. "
				+ "In 3D it accounts for non-isotropic calibration (and possible PSF "
				+ "deformation in the Z direction) thanks to an elliptic gaussian function, "
				+ "with axes constrained to be along X, Y and Z. "
				+ "In 2D we use an isotropic gaussian."
				+ "</html>";

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new SpotGaussianFitterAction();
		}

		@Override
		public ImageIcon getIcon()
		{
			return Icons.SPOT_ICON_64x64;
		}

		@Override
		public String getName()
		{
			return NAME;
		}
	}
}
