package fiji.plugin.trackmate.tracking.oldlap;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.tracking.kalman.KalmanTracker;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;

import java.util.Random;

public class KalmanTrackerInteractiveTest
{
	private static final int NFRAMES = 41;

	private static final double WIDTH = 400;

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new KalmanTrackerInteractiveTest().test();
	}

	private void test()
	{
		final SpotCollection spots = createSpots();

		final double maxSearchRadius = 2 * WIDTH / NFRAMES; // small
		final int maxFrameGap = 2;
		final double initialSearchRadius = 2 * WIDTH / ( NFRAMES );
		final KalmanTracker tracker = new KalmanTracker( spots, maxSearchRadius, maxFrameGap, initialSearchRadius );

		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.err.println( tracker.getErrorMessage() );
		}

		final Model model = new Model();
		model.setSpots( spots, false );
		model.setTracks( tracker.getResult(), false );

		final SpotCollection predictions = tracker.getPredictions();
		model.beginUpdate();

		try
		{
		for ( final Integer f : predictions.keySet() )
		{
			for ( final Spot s : predictions.iterable( f, true ) )
			{
				model.addSpotTo( s, f );
			}
		}
		}
		finally
		{
			model.endUpdate();
		}


		final SelectionModel selectionModel = new SelectionModel( model );
		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel );

		final TrackIndexAnalyzer ta = new TrackIndexAnalyzer();
		ta.process( model.getTrackModel().trackIDs( true ), model );

		final SpotColorGenerator scg = new SpotColorGenerator( model );
		scg.setFeature( Spot.QUALITY );
		view.setDisplaySettings( TrackMateModelView.KEY_SPOT_COLORING, scg );
		final PerTrackFeatureColorGenerator tcg = new PerTrackFeatureColorGenerator( model, TrackIndexAnalyzer.TRACK_INDEX );
		view.setDisplaySettings( TrackMateModelView.KEY_TRACK_COLORING, tcg );
		view.setDisplaySettings( TrackMateModelView.KEY_DISPLAY_SPOT_NAMES, true );
		view.render();

	}

	private SpotCollection createSpots()
	{
		final SpotCollection spots = new SpotCollection();
		final Random ran = new Random( 1l );

		/*
		 * The 4th lonely riders.
		 */
		final double[] x0 = new double[] { WIDTH / 10, WIDTH / 10, WIDTH / 2, 9 * WIDTH / 10 };
		final double[] y0 = new double[] { WIDTH / 2, WIDTH / 10, WIDTH / 10, WIDTH / 10 };
		final double[] vx0 = new double[] { WIDTH / ( NFRAMES - 1 ), WIDTH / ( NFRAMES - 1 ), 0, -WIDTH / ( NFRAMES - 1 ) };
		final double[] vy0 = new double[] { 0, WIDTH / ( NFRAMES - 1 ), WIDTH / ( NFRAMES - 1 ), WIDTH / ( NFRAMES - 1 ) };

		final double[] x = x0;
		final double[] y = y0;
		for ( int t = 0; t < NFRAMES; t++ )
		{
			for ( int i = 0; i < y.length; i++ )
			{
				final Spot spot = new Spot(
						x[ i ] + ran.nextGaussian() * WIDTH / 200,
						y[ i ] + ran.nextGaussian() * WIDTH / 200,
						0, 2, i, "T_" + i + "_S_" + t );
//				final Spot spot = new Spot( x[ i ], y[ i ], 0, 2, i, "T_" + i + "_S_" + t );
				spots.add( spot, t );

				x[ i ] += vx0[ i ];
				y[ i ] += vy0[ i ];
			}
		}
		return spots;
	}

}
