package fiji.plugin.trackmate.visualization.table;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.TmXmlReader;
import ij.ImageJ;

public class TrackMateTableExample
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );

		final TmXmlReader reader = new TmXmlReader( new File( "samples/FakeTracks.xml" ) );
		final Model model = reader.getModel();
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = reader.getDisplaySettings();
		model.getSpots().iterable( 1, true ).forEach( selectionModel::addSpotToSelection );

		new TrackTableView( model, selectionModel, ds ).render();
	}
}
