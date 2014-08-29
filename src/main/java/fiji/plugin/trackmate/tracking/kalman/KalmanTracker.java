package fiji.plugin.trackmate.tracking.kalman;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import Jama.Matrix;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class KalmanTracker implements SpotTracker
{

	private SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph;

	private String errorMessage;

	private Logger logger = Logger.VOID_LOGGER;

	private final SpotCollection spots;

	private final Map< String, Object > settings;

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


		final double[] xm = new double[] {
				secondSpot.getDoublePosition( 0 ),
				secondSpot.getDoublePosition( 1 ),
				secondSpot.getDoublePosition( 2 ),
				secondSpot.diffTo( firstSpot, Spot.POSITION_X ),
				secondSpot.diffTo( firstSpot, Spot.POSITION_Y ),
				secondSpot.diffTo( firstSpot, Spot.POSITION_Z ) };
		final Matrix XM = new Matrix( xm, 6 );
		
		final Matrix A = Matrix.identity( 6, 6 );
		final double dt = secondFrame - firstFrame;
		for ( int i = 0; i < 3; i++ )
		{
			A.set( 3 + i, i, dt );
		}

		final Matrix H = Matrix.identity( 3, 6 );

		final Matrix P = Matrix.identity( 6, 6 ).times( 100d );
		final Matrix Q = Matrix.identity( 6, 6 ).times( 1e-2 );
		final Matrix R = Matrix.identity( 3, 3 ).times( 1e-2 );

		final Matrix PP = A.times( P.times( A.transpose() ) ).plus( Q );

		final Matrix TEMP = H.times( PP.times( H.transpose() ) ).plus( R );

		final Matrix K = PP.times( H.transpose() ).times( TEMP.inverse() );

		final Matrix X = XP.plus( K.times( XM.minus( H.times( XP ) ) ) );

		System.out.println( X );// DEBUG

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

}
