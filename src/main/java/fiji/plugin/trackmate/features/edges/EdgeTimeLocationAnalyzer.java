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
public class EdgeTimeLocationAnalyzer implements EdgeAnalyzer
{

	public static final String KEY = "Edge mean location";

	/*
	 * FEATURE NAMES
	 */
	public static final String TIME = "EDGE_TIME";

	public static final String X_LOCATION = "EDGE_X_LOCATION";

	public static final String Y_LOCATION = "EDGE_Y_LOCATION";

	public static final String Z_LOCATION = "EDGE_Z_LOCATION";

	public static final List< String > FEATURES = new ArrayList< >( 4 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< >( 4 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 4 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 4 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 4 );

	static
	{
		FEATURES.add( TIME );
		FEATURES.add( X_LOCATION );
		FEATURES.add( Y_LOCATION );
		FEATURES.add( Z_LOCATION );

		FEATURE_NAMES.put( TIME, "Time (mean)" );
		FEATURE_NAMES.put( X_LOCATION, "X Location (mean)" );
		FEATURE_NAMES.put( Y_LOCATION, "Y Location (mean)" );
		FEATURE_NAMES.put( Z_LOCATION, "Z Location (mean)" );

		FEATURE_SHORT_NAMES.put( TIME, "T" );
		FEATURE_SHORT_NAMES.put( X_LOCATION, "X" );
		FEATURE_SHORT_NAMES.put( Y_LOCATION, "Y" );
		FEATURE_SHORT_NAMES.put( Z_LOCATION, "Z" );

		FEATURE_DIMENSIONS.put( TIME, Dimension.TIME );
		FEATURE_DIMENSIONS.put( X_LOCATION, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( Y_LOCATION, Dimension.POSITION );
		FEATURE_DIMENSIONS.put( Z_LOCATION, Dimension.POSITION );

		IS_INT.put( TIME, Boolean.FALSE );
		IS_INT.put( X_LOCATION, Boolean.FALSE );
		IS_INT.put( Y_LOCATION, Boolean.FALSE );
		IS_INT.put( Z_LOCATION, Boolean.FALSE );
	}

	private int numThreads;

	private long processingTime;

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTimeLocationAnalyzer()
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
			threads[ i ] = new Thread( "EdgeTimeLocationAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					DefaultWeightedEdge edge;
					while ( ( edge = queue.poll() ) != null )
					{

						final Spot source = model.getTrackModel().getEdgeSource( edge );
						final Spot target = model.getTrackModel().getEdgeTarget( edge );

						final double x = 0.5 * ( source.getFeature( Spot.POSITION_X ) + target.getFeature( Spot.POSITION_X ) );
						final double y = 0.5 * ( source.getFeature( Spot.POSITION_Y ) + target.getFeature( Spot.POSITION_Y ) );
						final double z = 0.5 * ( source.getFeature( Spot.POSITION_Z ) + target.getFeature( Spot.POSITION_Z ) );
						final double t = 0.5 * ( source.getFeature( Spot.POSITION_T ) + target.getFeature( Spot.POSITION_T ) );

						featureModel.putEdgeFeature( edge, TIME, t );
						featureModel.putEdgeFeature( edge, X_LOCATION, x );
						featureModel.putEdgeFeature( edge, Y_LOCATION, y );
						featureModel.putEdgeFeature( edge, Z_LOCATION, z );
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
