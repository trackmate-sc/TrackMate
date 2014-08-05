package fiji.plugin.trackmate.tracking.sparselap;

import static fiji.plugin.trackmate.tracking.LAPUtils.checkFeatureMap;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;
import ij.ImageJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.OutputAlgorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.tracking.jonkervolgenant.JVSUtils;
import fiji.plugin.trackmate.tracking.jonkervolgenant.SparseCostMatrix;
import fiji.plugin.trackmate.tracking.sparselap.linker.CostFunction;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class SegmentMergingCostMatrixCreator implements Benchmark, OutputAlgorithm< SparseCostMatrix >
{

	private static final String BASE_ERROR_MESSAGE = "[SegmentMergingCostMatrixCreator] ";

	private final List< Spot > segmentEnds;

	private final List< List< Spot >> segmentMiddles;

	private final Map< String, Object > settings;

	private String errorMessage;

	private SparseCostMatrix scm;

	private long processingTime;

	private ArrayList< Spot > uniqueSourcesAsList;

	private ArrayList< Spot > uniqueTargets;

	public SegmentMergingCostMatrixCreator( final List< Spot > segmentEnds, final List< List< Spot >> segmentMiddles, final Map< String, Object > settings )
	{
		this.segmentEnds = segmentEnds;
		this.segmentMiddles = segmentMiddles;
		this.settings = settings;
	}

	@Override
	public boolean checkInput()
	{
		if ( segmentEnds.isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Segment ends list is empty.";
			return false;
		}
		if ( segmentMiddles.isEmpty() )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Segment middles list is empty.";
			return false;
		}
		final StringBuilder str = new StringBuilder();
		if ( !checkSettingsValidity( settings, str ) )
		{
			errorMessage = BASE_ERROR_MESSAGE + str.toString();
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
		final long start = System.currentTimeMillis();

		@SuppressWarnings( "unchecked" )
		final Map< String, Double > featurePenalties = ( Map< String, Double > ) settings.get( KEY_MERGING_FEATURE_PENALTIES );
		final CostFunction< Spot > costFunction = getCostFunctionFor( featurePenalties );
		final double maxDistance = ( Double ) settings.get( KEY_MERGING_MAX_DISTANCE );
		final double costThreshold = maxDistance * maxDistance;

		/*
		 * Top-left quadrant
		 */

		// Sources and targets. They are sorted by increasing column
		// then
		// increasing line (scanning row by row).
		final ResizableIntArray sourceIndexes = new ResizableIntArray();
		final ArrayList< Spot > targetIndexes = new ArrayList< Spot >();
		// Corresponding costs.
		final ResizableDoubleArray linkCosts = new ResizableDoubleArray();

		// Loop over all sources and all targets, and find max cost.
		for ( int i = 0; i < segmentEnds.size(); i++ )
		{
			final Spot source = segmentEnds.get( i );

			for ( int j = 0; j < segmentMiddles.size(); j++ )
			{
				if ( i == j )
				{
					// Cannot link to yourself.
					continue;
				}

				final List< Spot > trackMiddle = segmentMiddles.get( j );

				for ( int k = 0; k < trackMiddle.size(); k++ )
				{

					final Spot target = trackMiddle.get( k );

					// Check frame interval, must be 1.
					final int tdiff = ( int ) target.diffTo( source, Spot.FRAME );
					if ( tdiff != 1 )
					{
						continue;
					}

					// Check max distance
					final double cost = costFunction.linkingCost( source, target );
					if ( cost > costThreshold )
					{
						continue;
					}

					sourceIndexes.add( i );
					targetIndexes.add( target );
					linkCosts.add( cost );
				}
			}
		}

		sourceIndexes.trimToSize();
		targetIndexes.trimToSize();
		linkCosts.trimToSize();

		final int[] uniqueSources = JVSUtils.unique( sourceIndexes.data );
		uniqueSourcesAsList = new ArrayList< Spot >( uniqueSources.length );
		for ( int i = 0; i < uniqueSources.length; i++ )
		{
			uniqueSourcesAsList.add( segmentEnds.get( i ) );
		}
		uniqueTargets = new ArrayList< Spot >( new HashSet< Spot >( targetIndexes ) );
		final Comparator< Spot > idComparator = new Comparator< Spot >()
				{
			@Override
			public int compare( final Spot o1, final Spot o2 )
			{
				return o1.ID() - o2.ID();
			}
				};
				Collections.sort( uniqueTargets, idComparator );

				// Build the cost matrix
				final int nCols = uniqueTargets.size();
				final int nRows = uniqueSources.length;
				final double[] cc = new double[ sourceIndexes.size ];
				final int[] kk = new int[ sourceIndexes.size ];
				final int[] number = new int[ nRows ];

				int psi = sourceIndexes.data[ 0 ];
				int rowIndex = 0;
				int index = -1;
				int rowCount = -1;
				for ( int i = 0; i < sourceIndexes.size; i++ )
				{
					index++;
					rowCount++;
					final int si = sourceIndexes.data[ i ];
					if ( si != psi )
					{
						// new source
						psi = si;
						// Store the number of element in the line
						number[ rowIndex ] = rowCount;
						// Increment row index, global index and reset row count;
						rowIndex++;
						rowCount = 0;
						// You are now on a new line.
					}

					cc[ index ] = linkCosts.data[ i ];
					final Spot target = targetIndexes.get( i );
					final int sp = Collections.binarySearch( uniqueTargets, target, idComparator );
					kk[ index ] = sp;
				}
				// Terminate the last row
				// Store the number of element in the line
				rowCount++;
				number[ rowIndex ] = rowCount;

				scm = new SparseCostMatrix( cc, kk, number, nCols );

				final long end = System.currentTimeMillis();
				processingTime = end - start;
				return true;
	}

	protected CostFunction< Spot > getCostFunctionFor( final Map< String, Double > featurePenalties )
	{
		final CostFunction< Spot > costFunction;
		if ( null == featurePenalties || featurePenalties.isEmpty() )
		{
			costFunction = new DefaultCostFunction();
		}
		else
		{
			costFunction = new FeaturePenaltyCostFunction( featurePenalties );
		}
		return costFunction;
	}

	@Override
	public SparseCostMatrix getResult()
	{
		return scm;
	}

	/**
	 * Returns the index of the specified source in the generated cost matrix.
	 * 
	 * @return the list of Spot, such that <code>sourceIndex.get( i )</code> is
	 *         the index in the specified segment ends list of the row
	 *         <code>i</code> in the generated cost matrix.
	 * @see #getTargetIndex()
	 * @see #getResult()
	 */
	public List< Spot > getSourceIndex()
	{
		return uniqueSourcesAsList;
	}

	/**
	 * Returns the index of the specified target in the generated cost matrix.
	 * 
	 * @return the list of Spot, such that <code>targetIndex.get( j )</code> is
	 *         the index in the specified segment starts list of the column
	 *         <code>j</code> in the generated cost matrix.
	 * @see #getSourceIndex()
	 * @see #getResult()
	 */
	public List< Spot > getTargetIndex()
	{
		return uniqueTargets;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

	private static final boolean checkSettingsValidity( final Map< String, Object > settings, final StringBuilder str )
	{
		if ( null == settings )
		{
			str.append( "Settings map is null.\n" );
			return false;
		}

		boolean ok = true;
		// Merging
		ok = ok & checkParameter( settings, KEY_MERGING_MAX_DISTANCE, Double.class, str );
		ok = ok & checkFeatureMap( settings, KEY_MERGING_FEATURE_PENALTIES, str );

		// Check keys
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_MERGING_MAX_DISTANCE );
		final List< String > optionalKeys = new ArrayList< String >();
		optionalKeys.add( KEY_MERGING_FEATURE_PENALTIES );
		ok = ok & checkMapKeys( settings, mandatoryKeys, optionalKeys, str );

		return ok;
	}

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();
		System.out.println( spots );

		final Settings settings0 = new Settings();
		reader.readSettings( settings0, null, new TrackerProvider(), null, null, null );

		final Map< String, Object > settings1 = new HashMap< String, Object >();
		settings1.put( KEY_LINKING_MAX_DISTANCE, 15d );
		settings1.put( KEY_ALTERNATIVE_LINKING_COST_FACTOR, 1.05 );
		final SparseLAPFrameToFrameTracker ftfTracker = new SparseLAPFrameToFrameTracker( spots, settings1 );
		if ( !ftfTracker.checkInput() || !ftfTracker.process() )
		{
			System.err.println( ftfTracker.getErrorMessage() );
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = ftfTracker.getResult();
		model.setTracks( graph, true );

		// Track merging
		final Map< String, Object > settings2 = new HashMap< String, Object >();
		settings2.put( KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE );
		settings2.put( KEY_MERGING_FEATURE_PENALTIES, new HashMap< String, Double >( DEFAULT_MERGING_FEATURE_PENALTIES ) );

		final GraphSegmentSplitter segmentSplitter = new GraphSegmentSplitter( graph );
		final List< Spot > segmentEnds = segmentSplitter.getSegmentEnds();
		final List< List< Spot >> segmentMiddles = segmentSplitter.getSegmentMiddles();

		final SegmentMergingCostMatrixCreator costMatrixCreator = new SegmentMergingCostMatrixCreator( segmentEnds, segmentMiddles, settings2 );
		if ( !costMatrixCreator.checkInput() || !costMatrixCreator.process() )
		{
			System.err.println( costMatrixCreator.getErrorMessage() );
			return;
		}

		System.out.println( costMatrixCreator.getResult() );
		System.out.println( "Sources: " + costMatrixCreator.getSourceIndex() );
		System.out.println( "Targets: " + costMatrixCreator.getTargetIndex() );

		final SelectionModel sm = new SelectionModel( model );
		final TrackScheme trackScheme = new TrackScheme( model, sm );
		trackScheme.render();
		ImageJ.main( args );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, sm );
		view.render();
	}

}
