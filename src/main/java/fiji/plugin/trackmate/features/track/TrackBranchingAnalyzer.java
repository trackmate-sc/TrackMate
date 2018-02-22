package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.swing.ImageIcon;

import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;

@SuppressWarnings( "deprecation" )
@Plugin( type = TrackAnalyzer.class )
public class TrackBranchingAnalyzer implements TrackAnalyzer
{

	/*
	 * CONSTANTS
	 */
	public static final String KEY = "Branching analyzer";

	public static final String NUMBER_GAPS = "NUMBER_GAPS";

	public static final String LONGEST_GAP = "LONGEST_GAP";

	public static final String NUMBER_SPLITS = "NUMBER_SPLITS";

	public static final String NUMBER_MERGES = "NUMBER_MERGES";

	public static final String NUMBER_COMPLEX = "NUMBER_COMPLEX";

	public static final String NUMBER_SPOTS = "NUMBER_SPOTS";

	public static final List< String > FEATURES = new ArrayList< >( 5 );

	public static final Map< String, String > FEATURE_NAMES = new HashMap< >( 5 );

	public static final Map< String, String > FEATURE_SHORT_NAMES = new HashMap< >( 5 );

	public static final Map< String, Dimension > FEATURE_DIMENSIONS = new HashMap< >( 5 );

	public static final Map< String, Boolean > IS_INT = new HashMap< >( 5 );

	static
	{
		FEATURES.add( NUMBER_SPOTS );
		FEATURES.add( NUMBER_GAPS );
		FEATURES.add( LONGEST_GAP );
		FEATURES.add( NUMBER_SPLITS );
		FEATURES.add( NUMBER_MERGES );
		FEATURES.add( NUMBER_COMPLEX );

		FEATURE_NAMES.put( NUMBER_SPOTS, "Number of spots in track" );
		FEATURE_NAMES.put( NUMBER_GAPS, "Number of gaps" );
		FEATURE_NAMES.put( LONGEST_GAP, "Longest gap" );
		FEATURE_NAMES.put( NUMBER_SPLITS, "Number of split events" );
		FEATURE_NAMES.put( NUMBER_MERGES, "Number of merge events" );
		FEATURE_NAMES.put( NUMBER_COMPLEX, "Complex points" );

		FEATURE_SHORT_NAMES.put( NUMBER_SPOTS, "N spots" );
		FEATURE_SHORT_NAMES.put( NUMBER_GAPS, "Gaps" );
		FEATURE_SHORT_NAMES.put( LONGEST_GAP, "Longest gap" );
		FEATURE_SHORT_NAMES.put( NUMBER_SPLITS, "Splits" );
		FEATURE_SHORT_NAMES.put( NUMBER_MERGES, "Merges" );
		FEATURE_SHORT_NAMES.put( NUMBER_COMPLEX, "Complex" );

		FEATURE_DIMENSIONS.put( NUMBER_SPOTS, Dimension.NONE );
		FEATURE_DIMENSIONS.put( NUMBER_GAPS, Dimension.NONE );
		FEATURE_DIMENSIONS.put( LONGEST_GAP, Dimension.NONE );
		FEATURE_DIMENSIONS.put( NUMBER_SPLITS, Dimension.NONE );
		FEATURE_DIMENSIONS.put( NUMBER_MERGES, Dimension.NONE );
		FEATURE_DIMENSIONS.put( NUMBER_COMPLEX, Dimension.NONE );

		IS_INT.put( NUMBER_SPOTS, Boolean.TRUE );
		IS_INT.put( NUMBER_GAPS, Boolean.TRUE );
		IS_INT.put( LONGEST_GAP, Boolean.TRUE );
		IS_INT.put( NUMBER_SPLITS, Boolean.TRUE );
		IS_INT.put( NUMBER_MERGES, Boolean.TRUE );
		IS_INT.put( NUMBER_COMPLEX, Boolean.TRUE );
	}

	private int numThreads;

	private long processingTime;

	public TrackBranchingAnalyzer()
	{
		setNumThreads();
	}

	@Override
	public boolean isLocal()
	{
		return true;
	}

	@Override
	public void process( final Collection< Integer > trackIDs, final Model model )
	{

		if ( trackIDs.isEmpty() ) { return; }

		final ArrayBlockingQueue< Integer > queue = new ArrayBlockingQueue< >( trackIDs.size(), false, trackIDs );

		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "TrackBranchingAnalyzer thread " + i )
			{
				@Override
				public void run()
				{
					Integer trackID;
					while ( ( trackID = queue.poll() ) != null )
					{

						final Set< Spot > track = model.getTrackModel().trackSpots( trackID );

						int nmerges = 0;
						int nsplits = 0;
						int ncomplex = 0;
						for ( final Spot spot : track )
						{
							final Set< DefaultWeightedEdge > edges = model.getTrackModel().edgesOf( spot );

							// get neighbors
							final Set< Spot > neighbors = new HashSet< >();
							for ( final DefaultWeightedEdge edge : edges )
							{
								neighbors.add( model.getTrackModel().getEdgeSource( edge ) );
								neighbors.add( model.getTrackModel().getEdgeTarget( edge ) );
							}
							neighbors.remove( spot );

							// inspect neighbors relative time position
							int earlier = 0;
							int later = 0;
							for ( final Spot neighbor : neighbors )
							{
								if ( spot.diffTo( neighbor, Spot.FRAME ) > 0 )
								{
									earlier++; // neighbor is before in time
								}
								else
								{
									later++;
								}
							}

							// Test for classical spot
							if ( earlier == 1 && later == 1 )
							{
								continue;
							}

							// classify spot
							if ( earlier <= 1 && later > 1 )
							{
								nsplits++;
							}
							else if ( later <= 1 && earlier > 1 )
							{
								nmerges++;
							}
							else if ( later > 1 && earlier > 1 )
							{
								ncomplex++;
							}
						}

						int ngaps = 0, longestgap = 0;
						for ( final DefaultWeightedEdge edge : model.getTrackModel().trackEdges( trackID ) )
						{
							final Spot source = model.getTrackModel().getEdgeSource( edge );
							final Spot target = model.getTrackModel().getEdgeTarget( edge );
							final int gaplength = (int)Math.abs( target.diffTo( source, Spot.FRAME ) ) - 1;
							if ( gaplength > 0 )
							{
								ngaps++;
								if ( longestgap < gaplength )
								{
									longestgap = gaplength;
								}
							}
						}

						// Put feature data
						model.getFeatureModel().putTrackFeature( trackID, NUMBER_GAPS, Double.valueOf( ngaps ) );
						model.getFeatureModel().putTrackFeature( trackID, LONGEST_GAP, Double.valueOf( longestgap ) );
						model.getFeatureModel().putTrackFeature( trackID, NUMBER_SPLITS, Double.valueOf( nsplits ) );
						model.getFeatureModel().putTrackFeature( trackID, NUMBER_MERGES, Double.valueOf( nmerges ) );
						model.getFeatureModel().putTrackFeature( trackID, NUMBER_COMPLEX, Double.valueOf( ncomplex ) );
						model.getFeatureModel().putTrackFeature( trackID, NUMBER_SPOTS, Double.valueOf( track.size() ) );

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
	public String getKey()
	{
		return KEY;
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
