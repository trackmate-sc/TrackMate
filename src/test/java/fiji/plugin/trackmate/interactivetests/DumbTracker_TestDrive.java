package fiji.plugin.trackmate.interactivetests;

import ij.ImageJ;

import java.util.Random;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.dumb.DumbTracker;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class DumbTracker_TestDrive
{

	public final void test()
	{

		final double ranAmpl = 0.1;

		final Random ran = new Random( 5l );
		final SpotCollection spots = new SpotCollection();

		for ( int i = 0; i < 10; i++ )
		{
			for ( int j = 0; j <= Math.min( i, 5 ); j++ )
			{
				final Spot spot = new Spot( 1d + 10 * ( j + ranAmpl * ran.nextGaussian() ), 1d + 5 * ( i + ranAmpl * ran.nextDouble() ), 0d, 1d, -1d );
				spots.add( spot, i );
			}
		}

		for ( int i = 10; i < 15; i++ )
		{
			for ( int j = 0; j < 15 - i; j++ )
			{
				final Spot spot = new Spot( 1d + 10 * ( j + ranAmpl * ran.nextGaussian() ), 1d + 5 * ( i + ranAmpl * ran.nextDouble() ), 0d, 1d, -1d );
				spots.add( spot, i );
			}
		}

		/*
		 * 2nd tier
		 */

		for ( int i = 20; i < 30; i++ )
		{
			for ( int j = 0; j <= Math.min( i, 5 ); j++ )
			{
				final Spot spot = new Spot( 1d + 10 * ( j + ranAmpl * ran.nextGaussian() ), 1d + 5 * ( i + ranAmpl * ran.nextDouble() ), 0d, 1d, -1d );
				spots.add( spot, i );
			}
		}

		for ( int i = 40; i < 50; i++ )
		{
			for ( int j = 0; j < 5; j++ )
			{
				final Spot spot = new Spot( 1d + 10 * ( j + ranAmpl * ran.nextGaussian() ), 1d + 5 * ( i - 10 + ranAmpl * ran.nextDouble() ), 0d, 1d, -1d );
				spots.add( spot, i );
			}
		}

		final DumbTracker tracker = new DumbTracker();
		tracker.setTarget( spots, null );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.err.println( tracker.getErrorMessage() );
			return;
		}

		System.out.println( "Done!" );// DEBUG

		final Model model = new Model();
		model.setSpots( spots, false );
		model.setTracks( tracker.getResult(), false );

		final SelectionModel selectionModel = new SelectionModel( model );

		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel );
		view.render();

		final TrackScheme trackscheme = new TrackScheme( model, selectionModel );
		trackscheme.render();

	}

	public static void main( final String[] args )
	{
		new ImageJ();
		new DumbTracker_TestDrive().test();
	}

}
