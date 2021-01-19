package fiji.plugin.trackmate.action;

import java.io.File;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImageJ;
import ij.ImagePlus;

public class SpotGaussianFitterExample
{

	public static void main( final String[] args ) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		ImageJ.main( args );
		final TmXmlReader reader = new TmXmlReader( new File( "samples/FakeTracks.xml" ) );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );

		final SpotGaussianFitter fitter = new SpotGaussianFitter( model, settings, Logger.IJ_LOGGER );
		if ( !fitter.checkInput() || !fitter.process() )
		{
			System.err.println( fitter.getErrorMessage() );
			return;
		}

		System.out.println( "Finished in " + fitter.getProcessingTime() + " ms." );

		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();

		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
		view.render();
		final TrackMateGUIController controller = new TrackMateGUIController( new TrackMate( model, settings ), ds, selectionModel );
		controller.setGUIStateString( ConfigureViewsDescriptor.KEY );
	}
}
