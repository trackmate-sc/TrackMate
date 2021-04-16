package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.io.File;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
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
		final TmXmlReader reader = new TmXmlReader( new File( "D:\\Projects\\TSabate\\Data\\TestGauss3D_02\\testimage_gauss_0.0005_poisson_false_0.40.trackmate.xml" ) );
		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		imp.show();
		final Settings settings = reader.readSettings( imp );
		final TrackMate trackmate = new TrackMate( model, settings );
		trackmate.setNumThreads( 1 );

		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
		final TrackMateWizardSequence sequence = new TrackMateWizardSequence( trackmate, selectionModel, ds );
		sequence.setCurrent( "Actions" );
		final JFrame frame = sequence.run( "Test Gauss-fitting action" );

		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );
		frame.setVisible( true );

		final SpotGaussianFitter fitter = new SpotGaussianFitter( model, settings, Logger.IJ_LOGGER );
		fitter.setNumThreads( trackmate.getNumThreads() );
		if ( !fitter.checkInput() || !fitter.process() )
		{
			System.err.println( fitter.getErrorMessage() );
			return;
		}

		System.out.println( "Finished in " + fitter.getProcessingTime() + " ms." );

		final HyperStackDisplayer view = new HyperStackDisplayer( model, selectionModel, imp, ds );
		view.render();

	}
}
