package fiji.plugin.trackmate.tracking.kalman;

import ij.ImagePlus;

import java.io.File;
import java.util.Iterator;
import java.util.NavigableSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import Jama.Matrix;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class KalmanTracker implements SpotTracker
{

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private final SpotCollection spots;

	private Model model;

	private final double maxSearchRadius;

	private final int maxFrameGap;

	/*
	 * CONSTRUCTOR
	 */

	public KalmanTracker( final SpotCollection spots, final double maxSearchRadius, final int maxFrameGap )
	{
		this.spots = spots;
		this.maxSearchRadius = maxSearchRadius;
		this.maxFrameGap = maxFrameGap;
	}

	/*
	 * PUBLIC METHODS
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
		graph = new SimpleWeightedGraph< Spot, DefaultWeightedEdge >( DefaultWeightedEdge.class );

		final NavigableSet< Integer > keySet = spots.keySet();

		final Iterator< Integer > frameIterator = keySet.iterator();

		final int firstFrame = frameIterator.next();
		final Spot firstSpot = spots.iterator( firstFrame, true ).next();

		if ( !frameIterator.hasNext() ) { return true; }

		final int secondFrame = frameIterator.next();
		final Spot secondSpot = spots.getClosestSpot( firstSpot, secondFrame, true );

		final Matrix XP = estimateInitialState( firstSpot, secondSpot );
		final LinearMotionKalmanTracker kt = new LinearMotionKalmanTracker( XP );

		Spot previousSpot = firstSpot;
		graph.addVertex( previousSpot );
		final Spot location = new Spot( previousSpot );

		model.beginUpdate(); // DEBUG
		for ( int frame = firstFrame; frame < keySet.last(); frame++ )
		{
			// Predict
			final Matrix X = kt.predict();

			// Measure: the closest spot from prediction.
			setSpotFromMatrix( location, X );
			final Spot measurement = spots.getClosestSpot( location, frame + 1, true );

			// Close enough?
			if ( measurement.squareDistanceTo( previousSpot ) > maxSearchRadius * maxSearchRadius )
			{
				// No, so we say we have an occlusion. No measurement.
				kt.update( null );
			}
			else
			{
				final Matrix Xm = toMeasurement( measurement );
				// Just for fun, we add it to the model. // DEBUG
				final Spot sm = new Spot( location );
				sm.putFeature( Spot.QUALITY, -1d );
				model.addSpotTo( sm, frame + 1 );

				// Update graph
				graph.addVertex( measurement );
				graph.addEdge( previousSpot, measurement );

				// Update state
				kt.update( Xm );
				// Loop
				previousSpot = measurement;
			}

			if ( kt.getnOcclusion() > maxFrameGap )
			{
				break;
			}
		}
		model.endUpdate(); // DEBUG
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public void setNumThreads()
	{}

	@Override
	public void setNumThreads( final int numThreads )
	{}

	@Override
	public int getNumThreads()
	{
		return 1;
	}

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}

	private static final Matrix toMeasurement( final Spot spot )
	{
		final double[] d = new double[] {
				spot.getDoublePosition( 0 ), spot.getDoublePosition( 1 ), spot.getDoublePosition( 2 )
		};
		return new Matrix( d, 3 );
	}

	private static final void setSpotFromMatrix( final Spot spot, final Matrix X )
	{
		spot.putFeature( Spot.POSITION_X, X.get( 0, 0 ) );
		spot.putFeature( Spot.POSITION_Y, X.get( 1, 0 ) );
		spot.putFeature( Spot.POSITION_Z, X.get( 2, 0 ) );
	}

	private static final Matrix estimateInitialState( final Spot first, final Spot second )
	{
		final double[] xp = new double[] { first.getDoublePosition( 0 ), first.getDoublePosition( 1 ), first.getDoublePosition( 2 ), second.diffTo( first, Spot.POSITION_X ), second.diffTo( first, Spot.POSITION_Y ), second.diffTo( first, Spot.POSITION_Z ) };
		final Matrix XP = new Matrix( xp, 6 );
		return XP;
	}

	/*
	 * MAIN METHODS
	 */

	public static void main( final String[] args )
	{
		final File file = new File( "samples/FakeTracks.xml" );
		final TmXmlReader reader = new TmXmlReader( file );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final SpotCollection spots = model.getSpots();

		final Settings settings = new Settings();
		reader.readSettings( settings, null, null, null, null, null );

		final KalmanTracker tracker = new KalmanTracker( spots, 15, 2 );

		tracker.model = model; // DEBUG

		if (!tracker.checkInput() || !tracker.process()	) {
			System.out.println(tracker.getErrorMessage());
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph= tracker.getResult();
		model.setTracks( graph, false );

		ij.ImageJ.main( args );
		final SelectionModel selectionModel = new SelectionModel( model );
		final ImagePlus imp = settings.imp;
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp );
		final SpotColorGenerator scg = new SpotColorGenerator( model );
		scg.setFeature( Spot.QUALITY );
		view.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, scg );
		view.render();
	}

}
