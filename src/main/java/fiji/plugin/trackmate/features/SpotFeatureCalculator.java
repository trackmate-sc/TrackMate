package fiji.plugin.trackmate.features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.scijava.Cancelable;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.util.TMUtils;
import net.imagej.ImgPlus;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

/**
 * A class dedicated to centralizing the calculation of the numerical features
 * of spots, through {@link SpotAnalyzer}s.
 *
 * @author Jean-Yves Tinevez - 2013. Revised December 2020.
 *
 */
public class SpotFeatureCalculator extends MultiThreadedBenchmarkAlgorithm implements Cancelable
{

	private static final String BASE_ERROR_MSG = "[SpotFeatureCalculator] ";

	private final Settings settings;

	private final Model model;

	private boolean isCanceled;

	private String cancelReason;

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

		// Declare what you do.
		for ( final SpotAnalyzerFactoryBase< ? > factory : settings.getSpotAnalyzerFactories() )
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
		return true;
	}

	/**
	 * Calculates all the spot features configured in the {@link Settings}
	 * object, but only for the spots in the specified collection. Features are
	 * calculated for each spot, using their location, and the raw image.
	 */
	public void computeSpotFeatures( final SpotCollection toCompute, final boolean doLogIt )
	{
		final List< SpotAnalyzerFactoryBase< ? > > spotFeatureAnalyzers = settings.getSpotAnalyzerFactories();
		computeSpotFeaturesAgent( toCompute, spotFeatureAnalyzers, doLogIt );
	}

	/**
	 * The method in charge of computing spot features with the given
	 * {@link SpotAnalyzer}s, for the given {@link SpotCollection}.
	 *
	 * @param toCompute
	 *            the spots to compute.
	 * @param analyzerFactories
	 *            the analyzer factories to use for computation.
	 * @param doLogIt
	 *            whether we should report progress to the user.
	 */
	private void computeSpotFeaturesAgent( final SpotCollection toCompute, final List< SpotAnalyzerFactoryBase< ? > > analyzerFactories, final boolean doLogIt )
	{
		isCanceled = false;
		cancelReason = null;
		final long start = System.currentTimeMillis();
		final Logger logger = doLogIt ? model.getLogger() : Logger.VOID_LOGGER;

		// Can't compute any spot feature without an image to compute on.
		if ( settings.imp == null )
			return;

		@SuppressWarnings( "rawtypes" )
		final ImgPlus img = TMUtils.rawWraps( settings.imp );

		// Do it.
		final List< Integer > frameSet = new ArrayList<>( toCompute.keySet() );
		final int numFrames = frameSet.size();

		/*
		 * Fine tune multi-threading: If we have 10 threads and 15 frames to
		 * process, we process 10 frames at once, and allocate 1 thread per
		 * frame. But if we have 10 threads and 2 frames, we process the 2
		 * frames at once, and allocate 5 threads per frame if we can.
		 */
		final int nSimultaneousFrames = Math.max( 1, Math.min( numThreads, numFrames ) );
		final int threadsPerFrame = Math.max( 1, numThreads / nSimultaneousFrames );

		if ( doLogIt )
		{
			logger.log( "Computing spot features over "
					+ ( ( nSimultaneousFrames > 1 ) ? ( nSimultaneousFrames + " frames" ) : "1 frame" )
					+ " simultaneously and allocating "
					+ ( ( threadsPerFrame > 1 ) ? ( threadsPerFrame + " threads" ) : "1 thread" )
					+ " per frame.\n" );
			logger.setStatus( "Calculating " + toCompute.getNSpots( false ) + " spots features..." );
		}

		final AtomicInteger progress = new AtomicInteger( 0 );
		final List< Callable< Void > > tasks = new ArrayList<>( numFrames );
		final int workToDo = numFrames * analyzerFactories.size() * settings.imp.getNChannels();
		for ( int iFrame = 0; iFrame < numFrames; iFrame++ )
		{
			final int index = iFrame;
			// Create one task per frame.
			final Callable< Void > frameTask = new Callable< Void >()
			{
				@Override
				public Void call() throws Exception
				{
					final int frame = frameSet.get( index );

					for ( int channel = 0; channel < settings.imp.getNChannels(); channel++ )
					{
						for ( final SpotAnalyzerFactoryBase< ? > factory : analyzerFactories )
						{
							if ( isCanceled() )
								return null;

							@SuppressWarnings( "unchecked" )
							final SpotAnalyzer< ? > analyzer = factory.getAnalyzer( img, frame, channel );
							// Fine-tune multithreading if we can.
							if ( analyzer instanceof MultiThreaded )
								( ( MultiThreaded ) analyzer ).setNumThreads( threadsPerFrame );

							analyzer.process( toCompute.iterable( frame, false ) );

							logger.setProgress( progress.incrementAndGet() / ( double ) workToDo );
						} // Finished looping over analyzers
					} // Finished looping over channels

					return null;
				}
			};
			tasks.add( frameTask );
		}

		final ExecutorService executorService = Executors.newFixedThreadPool( nSimultaneousFrames );
		List< Future< Void > > futures;
		try
		{
			futures = executorService.invokeAll( tasks );
			for ( final Future< Void > future : futures )
				future.get();
		}
		catch ( InterruptedException | ExecutionException e )
		{
			e.printStackTrace();
		}

		executorService.shutdown();
		logger.setProgress( 1 );
		logger.setStatus( "" );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	// --- org.scijava.Cancelable methods ---

	@Override
	public boolean isCanceled()
	{
		return isCanceled;
	}

	@Override
	public void cancel( final String reason )
	{
		isCanceled = true;
		cancelReason = reason;
	}

	@Override
	public String getCancelReason()
	{
		return cancelReason;
	}
}
