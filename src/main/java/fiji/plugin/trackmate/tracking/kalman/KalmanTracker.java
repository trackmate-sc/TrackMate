package fiji.plugin.trackmate.tracking.kalman;

import ij.ImagePlus;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
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

	private final Map< String, Object > settings;

	private Model model;

	/*
	 * CONSTRUCTOR
	 */

	public KalmanTracker( final SpotCollection spots, final Map< String, Object > settings )
	{
		this.spots = spots;
		this.settings = settings;
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

		final double[] xp = new double[] {
				firstSpot.getDoublePosition( 0 ),
				firstSpot.getDoublePosition( 1 ),
				firstSpot.getDoublePosition( 2 ),
				secondSpot.diffTo( firstSpot, Spot.POSITION_X ),
				secondSpot.diffTo( firstSpot, Spot.POSITION_Y ),
				secondSpot.diffTo( firstSpot, Spot.POSITION_Z ) };
		final Matrix XP = new Matrix( xp, 6 );

		System.out.println( "Initial state:" );// DEBUG
		XP.print( 7, 1 );

		final double[] xm = new double[] {
				secondSpot.getDoublePosition( 0 ),
				secondSpot.getDoublePosition( 1 ),
				secondSpot.getDoublePosition( 2 ),
		};
		final Matrix XM = new Matrix( xm, 3 );

		final Matrix A = Matrix.identity( 6, 6 );
		final double dt = secondFrame - firstFrame;
		for ( int i = 0; i < 3; i++ )
		{
			A.set( i, 3 + i, dt );
		}
		System.out.println( "Evolution matrix:" );// DEBUG
		A.print( 7, 1 );

		final Matrix H = Matrix.identity( 3, 6 );

		Matrix P = Matrix.identity( 6, 6 ).times( 100d );
		final Matrix Q = Matrix.identity( 6, 6 ).times( 1e-2 );
		final Matrix R = Matrix.identity( 3, 3 ).times( 1e-2 );

		P = A.times( P.times( A.transpose() ) ).plus( Q );
		Matrix TEMP = H.times( P.times( H.transpose() ) ).plus( R );
		Matrix K = P.times( H.transpose() ).times( TEMP.inverse() );

		Matrix X = XP;
		Spot previousSpot = firstSpot;
		graph.addVertex( previousSpot );
		final Spot location = new Spot( previousSpot );

		model.beginUpdate(); // DEBUG
		for ( int frame = secondFrame; frame < keySet.last(); frame++ )
		{
			// Predict
			X = A.times( X );

			System.out.println( "Prediction at frame " + frame );// DEBUG
			X.print( 7, 1 );

			// Measure: the closest spot from prediction.
			location.putFeature( Spot.POSITION_X, X.get( 0, 0 ) );
			location.putFeature( Spot.POSITION_Y, X.get( 1, 0 ) );
			location.putFeature( Spot.POSITION_Z, X.get( 2, 0 ) );

			final Spot measurement = spots.getClosestSpot( location, frame + 1, true );
			XM.set( 0, 0, measurement.getDoublePosition( 0 ) );
			XM.set( 1, 0, measurement.getDoublePosition( 1 ) );
			XM.set( 2, 0, measurement.getDoublePosition( 2 ) );

			// Just for fun, we add it to the model.
			final Spot sm = new Spot( location );
			sm.putFeature( Spot.QUALITY, -1d );
			model.addSpotTo( sm, frame + 1 );

			System.out.println( "Measurement :" );// DEBUG
			XM.print( 7, 1 );

			graph.addVertex( measurement );
			graph.addEdge( previousSpot, measurement );

			// Update state
			P = A.times( P.times( A.transpose() ) ).plus( Q );
			TEMP = H.times( P.times( H.transpose() ) ).plus( R );
			K = P.times( H.transpose() ).times( TEMP.inverse() );

			X = X.plus( K.times( XM.minus( H.times( X ) ) ) );

			// Loop
			previousSpot = measurement;
		}
		model.endUpdate();
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

		final KalmanTracker tracker = new KalmanTracker( spots, null );

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
