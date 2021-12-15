package fiji.plugin.trackmate;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;

public class TestCopy
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );
		
		final String path = "samples/FakeTracks.xml";
		final TmXmlReader reader = new TmXmlReader( new File( path ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		
		final Model model = reader.getModel();
		final Model copy = model.copy();

		final ImagePlus imp = reader.readImage();
		imp.show();
		final Settings settings = reader.readSettings( imp );

		final SelectionModel selectionModel = new SelectionModel( copy );
		final DisplaySettings ds= DisplaySettingsIO.readUserDefault();
		final HyperStackDisplayer displayer = new HyperStackDisplayer( copy, selectionModel, imp, ds );
		displayer.render();
		
		final TrackMateWizardSequence sequence = new TrackMateWizardSequence( new TrackMate( copy, settings ), selectionModel, ds );
		sequence.setCurrent( "ConfigureViews" );
		final JFrame frame = sequence.run( "Copy model" );
		frame.setLocationRelativeTo( imp.getWindow() );
		frame.setVisible( true );
	}
}
