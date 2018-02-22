package fiji.plugin.trackmate.tracking.sparselap;

import static fiji.plugin.trackmate.tracking.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.Map;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Logger.SlaveLogger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.sparselap.costmatrix.JaqamanSegmentCostMatrixCreator;
import fiji.plugin.trackmate.tracking.sparselap.linker.JaqamanLinker;
import net.imglib2.algorithm.Benchmark;

/**
 * This class tracks deals with the second step of tracking according to the LAP
 * tracking framework formulated by Jaqaman, K. et al.
 * "Robust single-particle tracking in live-cell time-lapse sequences." Nature
 * Methods, 2008.
 *
 * <p>
 * In this tracking framework, tracking is divided into two steps:
 *
 * <ol>
 * <li>Identify individual track segments</li>
 * <li>Gap closing, merging and splitting</li>
 * </ol>
 * and this class does the second step.
 * <p>
 * It first extract track segment from a specified graph, and create a cost
 * matrix corresponding to the following events: Track segments can be:
 * <ul>
 * <li>Linked end-to-tail (gap closing)</li>
 * <li>Split (the start of one track is linked to the middle of another track)</li>
 * <li>Merged (the end of one track is linked to the middle of another track</li>
 * <li>Terminated (track ends)</li>
 * <li>Initiated (track starts)</li>
 * </ul>
 * The cost matrix for this step is illustrated in Figure 1c in the paper.
 * However, there is some important deviations from the paper: The alternative
 * costs that specify the cost for track termination or initiation are all
 * equals to the same fixed value.
 * <p>
 * The class itself uses a sparse version of the cost matrix and a solver that
 * can exploit it. Therefore it is optimized for memory usage rather than speed.
 */
public class SparseLAPSegmentTracker implements SpotTracker, Benchmark
{

	private static final String BASE_ERROR_MESSAGE = "[SparseLAPSegmentTracker] ";

	private final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private final Map< String, Object > settings;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private long processingTime;

	private int numThreads;

	public SparseLAPSegmentTracker( final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph, final Map< String, Object > settings )
	{
		this.graph = graph;
		this.settings = settings;
		setNumThreads();
	}

	@Override
	public SimpleWeightedGraph< Spot, DefaultWeightedEdge > getResult()
	{
		return graph;
	}

	@Override
	public boolean checkInput()
	{
		return true;
	}

	@Override
	public boolean process()
	{
		/*
		 * Check input now.
		 */

		// Check that the objects list itself isn't null
		if ( null == graph )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The input graph is null.";
			return false;
		}

		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + errorHolder.toString();
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		/*
		 * Top-left costs.
		 */

		logger.setProgress( 0d );
		logger.setStatus( "Creating the segment linking cost matrix..." );
		final JaqamanSegmentCostMatrixCreator costMatrixCreator = new JaqamanSegmentCostMatrixCreator( graph, settings );
		costMatrixCreator.setNumThreads( numThreads );
		final SlaveLogger jlLogger = new SlaveLogger( logger, 0, 0.9 );
		final JaqamanLinker< Spot, Spot > linker = new JaqamanLinker<>( costMatrixCreator, jlLogger );
		if ( !linker.checkInput() || !linker.process() )
		{
			errorMessage = linker.getErrorMessage();
			return false;
		}


		/*
		 * Create links in graph.
		 */

		logger.setProgress( 0.9d );
		logger.setStatus( "Creating links..." );

		final Map< Spot, Spot > assignment = linker.getResult();
		final Map< Spot, Double > costs = linker.getAssignmentCosts();

		for ( final Spot source : assignment.keySet() )
		{
			final Spot target = assignment.get( source );
			final DefaultWeightedEdge edge = graph.addEdge( source, target );

			final double cost = costs.get( source );
			graph.setEdgeWeight( edge, cost );
		}

		logger.setProgress( 1d );
		logger.setStatus( "" );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		/*
		 * In this class, we just need the following. We will check later for
		 * other parameters.
		 */

		boolean ok = true;
		// Gap-closing
		ok = ok & checkParameter( settings, KEY_ALLOW_GAP_CLOSING, Boolean.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkParameter( settings, KEY_GAP_CLOSING_MAX_FRAME_GAP, Integer.class, str );
		ok = ok & checkFeatureMap( settings, KEY_GAP_CLOSING_FEATURE_PENALTIES, str );
		// Splitting
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_SPLITTING, Boolean.class, str );
		// Merging
		ok = ok & checkParameter( settings, KEY_ALLOW_TRACK_MERGING, Boolean.class, str );
		return ok;
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
	public int getNumThreads()
	{
		return numThreads;
	}
}
