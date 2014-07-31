package fiji.plugin.trackmate.tracking.sparselap;

import java.util.Map;

import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class SparseLAPTracker extends MultiThreadedBenchmarkAlgorithm implements SpotTracker
{
	private final static String BASE_ERROR_MESSAGE = "[SparseLAPTracker] ";

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private Logger logger;

	private final SpotCollection spots;

	private final Map< String, Object > settings;

	/*
	 * CONSTRUCTOR
	 */

	public SparseLAPTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		this.spots = spots;
		this.settings = settings;
	}

	/*
	 * METHODS
	 */

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
		if ( null == spots )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is null.";
			return false;
		}

		// Check that the objects list contains inner collections.
		if ( spots.keySet().isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}

		// Check that at least one inner collection contains an object.
		boolean empty = true;
		for ( final int frame : spots.keySet() )
		{
			if ( spots.getNSpots( frame, true ) > 0 )
			{
				empty = false;
				break;
			}
		}
		if ( empty )
		{
			errorMessage = BASE_ERROR_MESSAGE + "The spot collection is empty.";
			return false;
		}
		// Check parameters
		final StringBuilder errorHolder = new StringBuilder();
		if ( !LAPUtils.checkSettingsValidity( settings, errorHolder ) )
		{
			errorMessage = errorHolder.toString();
			return false;
		}

		/*
		 * Process.
		 */

		final long start = System.currentTimeMillis();

		final long end = System.currentTimeMillis();
		processingTime = end - start;


		return true;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

}
