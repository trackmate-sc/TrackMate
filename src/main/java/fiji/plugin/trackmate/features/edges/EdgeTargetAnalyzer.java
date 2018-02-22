package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@SuppressWarnings( "deprecation" )
@Plugin( type = EdgeAnalyzer.class )
public class EdgeTargetAnalyzer implements EdgeAnalyzer
{

	public static final String KEY = "Edge target";

	/*
	 * FEATURE NAMES
	 */
	public static final String SPOT_SOURCE_ID = "SPOT_SOURCE_ID";

	public static final String SPOT_TARGET_ID = "SPOT_TARGET_ID";

	public static final String EDGE_COST = "LINK_COST";

	public static final List< String > FEATURES = new ArrayList< >( 3 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< >( 3 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 3 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 3 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 3 );

	static
	{
		FEATURES.add( SPOT_SOURCE_ID );
		FEATURES.add( SPOT_TARGET_ID );
		FEATURES.add( EDGE_COST );

		FEATURE_NAMES.put( SPOT_SOURCE_ID, "Source spot ID" );
		FEATURE_NAMES.put( SPOT_TARGET_ID, "Target spot ID" );
		FEATURE_NAMES.put( EDGE_COST, "Link cost" );

		FEATURE_SHORT_NAMES.put( SPOT_SOURCE_ID, "Source ID" );
		FEATURE_SHORT_NAMES.put( SPOT_TARGET_ID, "Target ID" );
		FEATURE_SHORT_NAMES.put( EDGE_COST, "Cost" );

		FEATURE_DIMENSIONS.put( SPOT_SOURCE_ID, Dimension.NONE );
		FEATURE_DIMENSIONS.put( SPOT_TARGET_ID, Dimension.NONE );
		FEATURE_DIMENSIONS.put( EDGE_COST, Dimension.NONE );

		IS_INT.put( SPOT_SOURCE_ID, Boolean.TRUE );
		IS_INT.put( SPOT_TARGET_ID, Boolean.TRUE );
		IS_INT.put( EDGE_COST, Boolean.FALSE );

	}

	private int numThreads;

	private long processingTime;

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTargetAnalyzer()
	{
		setNumThreads();
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< DefaultWeightedEdge > edges, final Model model )
	{

		if ( edges.isEmpty() ) { return; }

		final FeatureModel featureModel = model.getFeatureModel();

		final ArrayBlockingQueue< DefaultWeightedEdge > queue = new ArrayBlockingQueue< >( edges.size(), false, edges );

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "EdgeTargetAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					DefaultWeightedEdge edge;
					while ( ( edge = queue.poll() ) != null )
					{
						// Edge weight
						featureModel.putEdgeFeature( edge, EDGE_COST, model.getTrackModel().getEdgeWeight( edge ) );
						// Source & target name & ID
						final Spot source = model.getTrackModel().getEdgeSource( edge );
						featureModel.putEdgeFeature( edge, SPOT_SOURCE_ID, Double.valueOf( source.ID() ) );
						final Spot target = model.getTrackModel().getEdgeTarget( edge );
						featureModel.putEdgeFeature( edge, SPOT_TARGET_ID, Double.valueOf( target.ID() ) );
					}

				}
			};
		}

		final long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin( threads );
		final long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	@Override
	public String getKey()
	{
		return KEY;
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
	}

	@Override
	public List< String > getFeatures()
	{
		return FEATURES;
	}

	@Override
	public Map< String, String > getFeatureShortNames()
	{
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map< String, String > getFeatureNames()
	{
		return FEATURE_NAMES;
	}

	@Override
	public Map< String, Dimension > getFeatureDimensions()
	{
		return FEATURE_DIMENSIONS;
	}

	@Override
	public String getInfoText()
	{
		return null;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return KEY;
	}

	@Override
	public Map< String, Boolean > getIsIntFeature()
	{
		return IS_INT;
	}

	@Override
	public boolean isManualFeature()
	{
		return false;
	}
}
