package fiji.plugin.trackmate;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.FinalInterval;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.meta.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.EdgeFeatureCalculator;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.SpotFeatureCalculator;
import fiji.plugin.trackmate.features.TrackFeatureCalculator;
import fiji.plugin.trackmate.tracking.spot.DefaultSpotCollection;
import fiji.plugin.trackmate.tracking.spot.SpotCollection;
import fiji.plugin.trackmate.tracking.trackers.Tracker;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * <p>
 * The TrackMate_ class runs on the currently active time-lapse image (2D or 3D)
 * and both identifies and tracks bright spots over time.
 * </p>
 * 
 * <p>
 * <b>Required input:</b> A 2D or 3D time-lapse image with bright blobs.
 * </p>
 * 
 * @author Nicholas Perry, Jean-Yves Tinevez, Johannes Schindelin - Institut
 *         Pasteur - July 2010 - 2011 - 2012 - 2013 - 2014
 * 
 */
public class TrackMate implements Benchmark, MultiThreaded, Algorithm
{

	public static final String PLUGIN_NAME_STR = "TrackMate";

	public static final String PLUGIN_NAME_VERSION = "2.3.0-SNAPSHOT";

	/**
	 * The model this trackmate will shape.
	 */
	protected final Model model;

	protected final Settings settings;

	protected long processingTime;

	protected String errorMessage;

	protected int numThreads = Runtime.getRuntime().availableProcessors();

	/*
	 * CONSTRUCTORS
	 */

	public TrackMate( final Settings settings )
	{
		this( new Model(), settings );
	}

	public TrackMate( final Model model, final Settings settings )
	{
		this.model = model;
		this.settings = settings;
	}

	public TrackMate()
	{
		this( new Model(), new Settings() );
	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * This method exists for the following reason:
	 * <p>
	 * The detector receives at each frame a cropped image to operate on,
	 * depending on the user specifying a ROI. It therefore returns spots whose
	 * coordinates are with respect to the top-left corner of the ROI, not of
	 * the original image.
	 * <p>
	 * This method modifies the given spots to put them back in the image
	 * coordinate system. Additionally, is a non-square ROI was specified (e.g.
	 * a polygon), it prunes the spots that are not within the polygon of the
	 * ROI.
	 *
	 * @param spotsThisFrame
	 *            the spot list to inspect
	 * @param settings
	 *            the {@link Settings} object that will be used to retrieve the
	 *            image ROI and cropping information
	 * @return a list of spot. Depending on the presence of a polygon ROI, it
	 *         might be a new, pruned list. Or not.
	 */
	protected List< Spot > translateAndPruneSpots( final List< Spot > spotsThisFrame, final Settings settings )
	{

		// Put them back in the right referential
		final double[] calibration = TMUtils.getSpatialCalibration( settings.imp );
		TMUtils.translateSpots( spotsThisFrame, settings.xstart * calibration[ 0 ], settings.ystart * calibration[ 1 ], settings.zstart * calibration[ 2 ] );
		List< Spot > prunedSpots;
		// Prune if outside of ROI
		if ( null != settings.polygon )
		{
			prunedSpots = new ArrayList< Spot >();
			for ( final Spot spot : spotsThisFrame )
			{
				if ( settings.polygon.contains( spot.getFeature( TrackMateConstants.POSITION_X ) / calibration[ 0 ], spot.getFeature( TrackMateConstants.POSITION_Y ) / calibration[ 1 ] ) )
					prunedSpots.add( spot );
			}
		}
		else
		{
			prunedSpots = spotsThisFrame;
		}
		return prunedSpots;
	}

	/*
	 * METHODS
	 */

	public Model getModel()
	{
		return model;
	}

	public Settings getSettings()
	{
		return settings;
	}

	/*
	 * PROCESSES
	 */

	/**
	 * Calculate all features for all detected spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. Features to be calculated and analyzers are taken from the
	 * settings field of this object.
	 *
	 * @param doLogIt
	 *            if <code>true</code>, the {@link Logger} of the model will be
	 *            notified.
	 * @return <code>true</code> if the calculation was performed successfully,
	 *         <code>false</code> otherwise.
	 */
	public boolean computeSpotFeatures( final boolean doLogIt )
	{
		final Logger logger = model.getLogger();
		logger.log( "Computing spot features.\n" );
		final SpotFeatureCalculator calculator = new SpotFeatureCalculator( model, settings );
		if ( calculator.checkInput() && calculator.process() )
		{
			if ( doLogIt )
			{
				logger.log( "Computation done in " + calculator.getProcessingTime() + " ms.\n" );
			}
			return true;
		}
		else
		{
			errorMessage = "Spot features calculation failed:\n" + calculator.getErrorMessage();
			return false;
		}
	}

	/**
	 * Calculate all features for all detected spots.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. Features to be calculated and analyzers are taken from the
	 * settings field of this object.
	 *
	 * @param doLogIt
	 *            if <code>true</code>, the {@link Logger} of the model will be
	 *            notified.
	 * @return <code>true</code> if the calculation was performed successfuly,
	 *         <code>false</code> otherwise.
	 */
	public boolean computeEdgeFeatures( final boolean doLogIt )
	{
		final Logger logger = model.getLogger();
		final EdgeFeatureCalculator calculator = new EdgeFeatureCalculator( model, settings );
		if ( !calculator.checkInput() || !calculator.process() )
		{
			errorMessage = "Edge features calculation failed:\n" + calculator.getErrorMessage();
			return false;
		}
		if ( doLogIt )
		{
			logger.log( "Computation done in " + calculator.getProcessingTime() + " ms.\n" );
		}
		return true;
	}

	/**
	 * Calculate all features for all tracks.
	 *
	 * @return
	 */
	public boolean computeTrackFeatures( final boolean doLogIt )
	{
		final Logger logger = model.getLogger();
		final TrackFeatureCalculator calculator = new TrackFeatureCalculator( model, settings );
		if ( calculator.checkInput() && calculator.process() )
		{
			if ( doLogIt )
			{
				logger.log( "Computation done in " + calculator.getProcessingTime() + " ms.\n" );
			}
			return true;
		}
		else
		{
			errorMessage = "Track features calculation failed:\n" + calculator.getErrorMessage();
			return false;
		}
	}

	/**
	 * Execute the tracking part.
	 * <p>
	 * This method links all the selected spots from the thresholding part using
	 * the selected tracking algorithm. This tracking process will generate a
	 * graph (more precisely a {@link SimpleWeightedGraph}) made of the spot
	 * election for its vertices, and edges representing the links.
	 * <p>
	 * The {@link ModelChangeListener}s of the model will be notified when the
	 * successful process is over.
	 *
	 * @see #getTrackGraph()
	 */
	public boolean execTracking()
	{
		final Logger logger = model.getLogger();
		logger.log( "Starting tracking process.\n" );
		final Tracker<Spot> tracker = settings.trackerFactory.create( model.getSpots(), settings.trackerSettings );
		tracker.setLogger( logger );
		if ( tracker.checkInput() && tracker.process() )
		{
			model.setTracks( tracker.getResult(), true );
			return true;
		}
		else
		{
			errorMessage = "Tracking process failed:\n" + tracker.getErrorMessage();
			return false;
		}
	}

	/**
	 * Execute the detection part.
	 * <p>
	 * This method configure the chosen {@link Settings#detectorFactory} with
	 * the source image and the detectr settings and execute the detection
	 * process for all the frames set in the {@link Settings} object of the
	 * target model.
	 *
	 * @return true if the whole detection step has executed correctly.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public boolean execDetection()
	{
		final Logger logger = model.getLogger();
		logger.log( "Starting detection process.\n" );

		final SpotDetectorFactory< ? > factory = settings.detectorFactory;
		if ( null == factory )
		{
			errorMessage = "Detector factory is null.\n";
			return false;
		}
		if ( null == settings.detectorSettings )
		{
			errorMessage = "Detector settings is null.\n";
			return false;
		}

		/*
		 * Prepare interval
		 */
		final ImgPlus img = TMUtils.rawWraps( settings.imp );

		final long[] max = new long[ img.numDimensions() ];
		final long[] min = new long[ img.numDimensions() ];

		// X, we must have it
		final int xindex = TMUtils.findXAxisIndex( img );
		if ( xindex < 0 )
		{
			errorMessage = "Source image has no X axis.\n";
			return false;
		}
		min[ xindex ] = settings.xstart;
		max[ xindex ] = settings.xend;
		// Y, we must have it
		final int yindex = TMUtils.findYAxisIndex( img );
		if ( yindex < 0 )
		{
			errorMessage = "Source image has no Y axis.\n";
			return false;
		}
		min[ yindex ] = settings.ystart;
		max[ yindex ] = settings.yend;
		// Z, we MIGHT have it
		final int zindex = TMUtils.findZAxisIndex( img );
		if ( zindex >= 0 )
		{
			min[ zindex ] = settings.zstart;
			max[ zindex ] = settings.zend;
		}
		// CHANNEL, we might have it
		final int cindex = TMUtils.findCAxisIndex( img );
		if ( cindex >= 0 )
		{
			min[ cindex ] = ( Integer ) settings.detectorSettings.get( KEY_TARGET_CHANNEL ) - 1;
			max[ cindex ] = min[ cindex ];
		}
		// TIME, we might have it, but anyway we leave the start & end
		// management to the threads below
		final int tindex = TMUtils.findTAxisIndex( img );

		/*
		 * We want to exclude time (if we have it) from out interval and source,
		 * so that we can provide the detector instance with a hyperslice that
		 * does NOT have time as a dimension.
		 */
		final long[] intervalMin;
		final long[] intervalMax;
		if ( tindex >= 0 )
		{
			intervalMin = new long[ min.length - 1 ];
			intervalMax = new long[ min.length - 1 ];
			int nindex = -1;
			for ( int d = 0; d < min.length; d++ )
			{
				if ( d == tindex )
				{
					continue;
				}
				nindex++;
				intervalMin[ nindex ] = min[ d ];
				intervalMax[ nindex ] = max[ d ];
			}
		}
		else
		{
			intervalMin = min;
			intervalMax = max;
		}
		final FinalInterval interval = new FinalInterval( intervalMin, intervalMax );

		factory.setTarget( img, settings.detectorSettings );

		final int numFrames = settings.tend - settings.tstart + 1;
		// Final results holder, for all frames
		final DefaultSpotCollection spots = new DefaultSpotCollection();
		spots.setNumThreads( numThreads );
		// To report progress
		final AtomicInteger spotFound = new AtomicInteger( 0 );
		final AtomicInteger progress = new AtomicInteger( 0 );
		// To translate spots, later
		final double[] calibration = TMUtils.getSpatialCalibration( settings.imp );

		/*
		 * Fine tune multi-threading: If we have 10 threads and 15 frames to
		 * process, we process 10 frames at once, and allocate 1 thread per
		 * frame. But if we have 10 threads and 2 frames, we process the 2
		 * frames at once, and allocate 5 threads per frame if we can.
		 */
		final int nSimultaneousFrames = Math.min( numThreads, numFrames );
		final int threadsPerFrame = Math.max( 1, numThreads / nSimultaneousFrames );

		final Thread[] threads = SimpleMultiThreading.newThreads( nSimultaneousFrames );
		final AtomicBoolean ok = new AtomicBoolean( true );

		// Prepare the thread array
		final AtomicInteger ai = new AtomicInteger( settings.tstart );
		for ( int ithread = 0; ithread < threads.length; ithread++ )
		{

			threads[ ithread ] = new Thread( "TrackMate spot detection thread " + ( 1 + ithread ) + "/" + threads.length )
			{
				private boolean wasInterrupted()
				{
					try
					{
						if ( isInterrupted() )
							return true;
						sleep( 0 );
						return false;
					}
					catch ( final InterruptedException e )
					{
						return true;
					}
				}

				@Override
				public void run()
				{

					for ( int frame = ai.getAndIncrement(); frame <= settings.tend; frame = ai.getAndIncrement() )
						try
						{
							// Yield detector for target frame
							final SpotDetector< ? > detector = factory.getDetector( interval, frame );
							if ( detector instanceof MultiThreaded )
							{
								final MultiThreaded md = ( MultiThreaded ) detector;
								md.setNumThreads( threadsPerFrame );
							}

							if ( wasInterrupted() )
								return;

							// Execute detection
							if ( ok.get() && detector.checkInput() && detector.process() )
							{
								// On success, get results.
								final List< Spot > spotsThisFrame = detector.getResult();
								List< Spot > prunedSpots;
								if ( null != settings.polygon )
								{
									prunedSpots = new ArrayList< Spot >();
									for ( final Spot spot : spotsThisFrame )
									{
										if ( settings.polygon.contains( spot.getFeature( TrackMateConstants.POSITION_X ) / calibration[ 0 ], spot.getFeature( TrackMateConstants.POSITION_Y ) / calibration[ 1 ] ) )
											prunedSpots.add( spot );
									}
								}
								else
								{
									prunedSpots = spotsThisFrame;
								}
								// Add detection feature other than position
								for ( final Spot spot : prunedSpots )
								{
									// FRAME will be set upon adding to
									// SpotCollection.
									spot.putFeature( TrackMateConstants.POSITION_T, frame * settings.dt );
								}
								// Store final results for this frame
								spots.put( frame, prunedSpots );
								// Report
								spotFound.addAndGet( prunedSpots.size() );
								logger.setProgress( progress.incrementAndGet() / ( double ) numFrames );

							}
							else
							{
								// Fail: exit and report error.
								ok.set( false );
								errorMessage = detector.getErrorMessage();
								return;
							}

						}
						catch ( final RuntimeException e )
						{
							final Throwable cause = e.getCause();
							if ( cause != null && cause instanceof InterruptedException ) { return; }
							throw e;
						}
				}
			};
		}

		logger.setStatus( "Detection..." );
		logger.setProgress( 0 );

		try
		{
			SimpleMultiThreading.startAndJoin( threads );
		}
		catch ( final RuntimeException e )
		{
			ok.set( false );
			if ( e.getCause() != null && e.getCause() instanceof InterruptedException )
			{
				errorMessage = "Detection workers interrupted.\n";
				for ( final Thread thread : threads )
					thread.interrupt();
				for ( final Thread thread : threads )
				{
					if ( thread.isAlive() )
						try
						{
							thread.join();
						}
						catch ( final InterruptedException e2 )
						{
							// ignore
						}
				}
			}
			else
			{
				throw e;
			}
		}
		model.setSpots( spots, true );

		if ( ok.get() )
		{
			logger.log( "Found " + spotFound.get() + " spots.\n" );
		}
		else
		{
			logger.error( "Detection failed after " + progress.get() + " frames:\n" + errorMessage );
			logger.log( "Found " + spotFound.get() + " spots prior failure.\n" );
		}
		logger.setProgress( 1 );
		logger.setStatus( "" );
		return ok.get();
	}

	/**
	 * Execute the initial spot filtering part.
	 * <p>
	 * Because of the presence of noise, it is possible that some of the
	 * regional maxima found in the detection step have identified noise, rather
	 * than objects of interest. This can generates a very high number of spots,
	 * which is inconvenient to deal with when it comes to computing their
	 * features, or displaying them.
	 * <p>
	 * Any {@link SpotDetector} is expected to at least compute the
	 * {@link SpotFeature#QUALITY} value for each spot it creates, so it is
	 * possible to set up an initial filtering on this Feature, prior to any
	 * other operation.
	 * <p>
	 * This method simply takes all the detected spots, and discard those whose
	 * quality value is below the threshold set by
	 * {@link #setInitialSpotFilter(Float)}. The spot field is overwritten, and
	 * discarded spots can't be recalled.
	 * <p>
	 * The {@link ModelChangeListener}s of this model will be notified with a
	 * {@link ModelChangeEvent#SPOTS_COMPUTED} event.
	 *
	 * @see #getSpots()
	 * @see #setInitialFilter(Float)
	 */
	public boolean execInitialSpotFiltering()
	{
		final Logger logger = model.getLogger();
		logger.log( "Starting initial filtering process.\n" );

		final Double initialSpotFilterValue = settings.initialSpotFilterValue;
		final FeatureFilter featureFilter = new FeatureFilter( TrackMateConstants.QUALITY, initialSpotFilterValue, true );

		SpotCollection spots = model.getSpots();
		spots.filter( featureFilter );

		spots = (SpotCollection) spots.crop();

		model.setSpots( spots, true ); // Forget about the previous one
		return true;
	}

	/**
	 * Execute the spot feature filtering part.
	 * <p>
	 * Because of the presence of noise, it is possible that some of the
	 * regional maxima found in the detection step have identified noise, rather
	 * than objects of interest. A filtering operation based on the calculated
	 * features in this step should allow to rule them out.
	 * <p>
	 * This method simply takes all the detected spots, and mark as visible the
	 * spots whose features satisfy all of the filters entered with the method
	 * {@link #addFilter(SpotFilter)}.
	 * <p>
	 * The {@link ModelChangeListener}s of this model will be notified with a
	 * {@link ModelChangeEvent#SPOTS_FILTERED} event.
	 *
	 * @param doLogIt
	 *            if true, will send a message to the {@link Model#logger}.
	 * @see #getFilteredSpots()
	 */
	public boolean execSpotFiltering( final boolean doLogIt )
	{
		if ( doLogIt )
		{
			final Logger logger = model.getLogger();
			logger.log( "Starting spot filtering process.\n" );
		}
		model.filterSpots( settings.getSpotFilters(), true );
		return true;
	}

	public boolean execTrackFiltering( final boolean doLogIt )
	{
		if ( doLogIt )
		{
			final Logger logger = model.getLogger();
			logger.log( "Starting track filtering process.\n" );
		}

		model.beginUpdate();
		try
		{
			for ( final Integer trackID : model.getTrackModel().trackIDs( false ) )
			{
				boolean trackIsOk = true;
				for ( final FeatureFilter filter : settings.getTrackFilters() )
				{
					final Double tval = filter.value;
					final Double val = model.getFeatureModel().getTrackFeature( trackID, filter.feature );
					if ( null == val )
						continue;

					if ( filter.isAbove )
					{
						if ( val < tval )
						{
							trackIsOk = false;
							break;
						}
					}
					else
					{
						if ( val > tval )
						{
							trackIsOk = false;
							break;
						}
					}
				}
				model.setTrackVisibility( trackID, trackIsOk );
			}
		}
		finally
		{
			model.endUpdate();
		}
		return true;
	}

	@Override
	public String toString()
	{
		return PLUGIN_NAME_STR + "v" + PLUGIN_NAME_VERSION;
	}

	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean checkInput()
	{
		if ( null == model )
		{
			errorMessage = "The model is null.\n";
			return false;
		}
		if ( null == settings )
		{
			errorMessage = "Settings are null";
			return false;
		}
		if ( !settings.checkValidity() )
		{
			errorMessage = settings.getErrorMessage();
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean process()
	{
		if ( !execDetection() ) { return false; }
		if ( !execInitialSpotFiltering() ) { return false; }

		if ( !computeSpotFeatures( true ) ) { return false; }

		if ( !execSpotFiltering( true ) ) { return false; }

		if ( !execTracking() ) { return false; }

		if ( !computeTrackFeatures( true ) ) { return false; }

		if ( !execTrackFiltering( true ) ) { return false; }

		if ( !computeEdgeFeatures( true ) ) { return false; }

		return true;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;

	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	};

}
