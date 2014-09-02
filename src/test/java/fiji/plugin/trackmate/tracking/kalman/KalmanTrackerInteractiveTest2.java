package fiji.plugin.trackmate.tracking.kalman;

import ij.ImagePlus;

import java.io.File;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.SpotColorGenerator;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class KalmanTrackerInteractiveTest2
{

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

		final KalmanTracker tracker = new KalmanTracker( spots, 15d, 2, 15d );
		tracker.setLogger( Logger.DEFAULT_LOGGER );
		if ( !tracker.checkInput() || !tracker.process() )
		{
			System.out.println( tracker.getErrorMessage() );
			return;
		}

		final SimpleWeightedGraph< Spot, DefaultWeightedEdge > graph = tracker.getResult();
		model.setTracks( graph, false );

		model.beginUpdate();
		try
		{
			final SpotCollection predictions = tracker.getPredictions();
			for ( final Integer frame : predictions.keySet() )
			{
				for ( final Spot spot : predictions.iterable( frame, true ) )
				{
					model.addSpotTo( spot, frame );
				}
			}
		}
		finally
		{
			model.endUpdate();
		}

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
