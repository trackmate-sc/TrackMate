package fiji.plugin.trackmate.features;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.ImgPlus;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.multithreading.SimpleMultiThreading;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.spot.IndependentSpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * A class dedicated to centralizing the calculation of the numerical features
 * of spots, through {@link SpotAnalyzer}s.
 * 
 * @author Jean-Yves Tinevez - 2013
 * 
 */
@SuppressWarnings( "deprecation" )
public class SpotFeatureCalculator extends MultiThreadedBenchmarkAlgorithm
{

	private static final String BASE_ERROR_MSG = "[SpotFeatureCalculator] ";

	private final Settings settings;

	private final Model model;

	public SpotFeatureCalculator( final Model model, final Settings settings )
	{
		this.settings = settings;
		this.model = model;
	}

	/*
	 * METHODS
	 */

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
		return true;
	}

	/**
	 * Calculates the spot features configured in the {@link Settings} for all
	 * the spots of this model,
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. Since a {@link SpotAnalyzer} can compute more than a feature at
	 * once, spots might received more data than required.
	 */
	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		// Declare what you do.
		for ( final SpotAnalyzerFactory< ? > factory : settings.getSpotAnalyzerFactories() )
		{
			final Collection< String > features = factory.getFeatures();
			final Map< String, String > featureNames = factory.getFeatureNames();
			final Map< String, String > featureShortNames = factory.getFeatureShortNames();
			final Map< String, Dimension > featureDimensions = factory.getFeatureDimensions();
			final Map< String, Boolean > isIntFeature = factory.getIsIntFeature();
			model.getFeatureModel().declareSpotFeatures( features, featureNames, featureShortNames, featureDimensions, isIntFeature );
		}

		// Do it.
		computeSpotFeaturesAgent( model.getSpots(), settings.getSpotAnalyzerFactories(), true );

		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/**
	 * Calculates all the spot features configured in the {@link Settings}
	 * object for the specified spot collection. Features are calculated for
	 * each spot, using their location, and the raw image.
	 */
	public void computeSpotFeatures( final SpotCollection toCompute, final boolean doLogIt )
	{
		final List< SpotAnalyzerFactory< ? >> spotFeatureAnalyzers = settings.getSpotAnalyzerFactories();
		computeSpotFeaturesAgent( toCompute, spotFeatureAnalyzers, doLogIt );
	}

	/**
	 * The method in charge of computing spot features with the given
	 * {@link SpotAnalyzer}s, for the given {@link SpotCollection}.
	 * 
	 * @param toCompute
	 */
	private void computeSpotFeaturesAgent( final SpotCollection toCompute, final List< SpotAnalyzerFactory< ? >> analyzerFactories, final boolean doLogIt )
	{

		final Logger logger;
		if ( doLogIt )
		{
			logger = model.getLogger();
		}
		else
		{
			logger = Logger.VOID_LOGGER;
		}

		// Can't compute any spot feature without an image to compute on.
		if ( settings.imp == null )
			return;

		// Do it.
		final List< Integer > frameSet = new ArrayList<>( toCompute.keySet() );
		final int numFrames = frameSet.size();

		final AtomicInteger ai = new AtomicInteger( 0 );
		final AtomicInteger progress = new AtomicInteger( 0 );
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );

		int tc = 0;
		if ( settings != null && settings.detectorSettings != null )
		{
			// Try to extract it from detector settings target channel
			final Map< String, Object > ds = settings.detectorSettings;
			final Object obj = ds.get( KEY_TARGET_CHANNEL );
			if ( null != obj && obj instanceof Integer )
			{
				tc = ( ( Integer ) obj ) - 1;
			}
		}
		final int targetChannel = tc;

		@SuppressWarnings( "rawtypes" )
		final ImgPlus img = TMUtils.rawWraps( settings.imp );

		// Prepare the thread array
		for ( int ithread = 0; ithread < threads.length; ithread++ )
		{

			threads[ ithread ] = new Thread( "TrackMate spot feature calculating thread " + ( 1 + ithread ) + "/" + threads.length )
			{

				@Override
				public void run()
				{

					for ( int index = ai.getAndIncrement(); index < numFrames; index = ai.getAndIncrement() )
					{

						final int frame = frameSet.get( index );
						for ( final SpotAnalyzerFactory< ? > factory : analyzerFactories )
						{
							@SuppressWarnings( "unchecked" )
							final SpotAnalyzer< ? > analyzer = factory.getAnalyzer( model, img, frame, targetChannel );
							if ( analyzer instanceof IndependentSpotFeatureAnalyzer )
							{
								// Independent: we can process only the spot to update.
								@SuppressWarnings( "rawtypes" )
								final IndependentSpotFeatureAnalyzer analyzer2 = ( IndependentSpotFeatureAnalyzer ) analyzer;
								for ( final Spot spot : toCompute.iterable( frame, false ) )
								{
									analyzer2.process( spot );
								}
							}
							else
							{
								// Process all spots of the frame at once.
								analyzer.process();
							}

						}

						logger.setProgress( progress.incrementAndGet() / ( float ) numFrames );
					} // Finished looping over frames
				}
			};
		}
		logger.setStatus( "Calculating " + toCompute.getNSpots( false ) + " spots features..." );
		logger.setProgress( 0 );

		SimpleMultiThreading.startAndJoin( threads );

		logger.setProgress( 1 );
		logger.setStatus( "" );
	}

}
