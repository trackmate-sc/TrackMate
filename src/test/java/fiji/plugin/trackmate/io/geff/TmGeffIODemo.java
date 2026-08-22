package fiji.plugin.trackmate.io.geff;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import org.apache.commons.io.FileUtils;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import fiji.plugin.trackmate.gui.wizard.descriptors.LogPanelDescriptor2;
import fiji.plugin.trackmate.gui.wizard.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.ImagePlus;

public class TmGeffIODemo
{

	public static void main( final String[] args ) throws IOException
	{
		final String path = GeffTestDeserializationTest.SPOT_MIXED_PATH;
		System.out.println( "Reading from " + path );
		final TmXmlReader reader = new TmXmlReader( new File( path ) );
		if ( !reader.isReadingOk() )
		{
			System.err.println( reader.getErrorMessage() );
			return;
		}
		System.out.println( "Done." );
		final String savePath = path.replace( ".xml", ".geff" );

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		final Settings settings = reader.readSettings( imp );

		System.out.println( "Deleting previous " + savePath );
		FileUtils.deleteDirectory( new File( savePath ) );
		System.out.println( "Done." );

		System.out.println( "Writing to " + savePath );

		final TmGeffWriter geffWriter = new TmGeffWriter( savePath );
		geffWriter.appendModel( model );
		geffWriter.appendSettings( settings );
		geffWriter.appendDisplaySettings( reader.getDisplaySettings() );
		geffWriter.appendLog( reader.getLog() );
		geffWriter.appendGUIState( reader.getGUIState() );
		geffWriter.write();

		System.out.println( "Done." );

		System.out.println( "Reading the model back from " + savePath );
		readBack( savePath );
	}

	private static void readBack( final String savePath ) throws IOException
	{
		final TmGeffReader geffReader = new TmGeffReader( savePath );

		// Model
		final Model model = geffReader.getModel();

		// Image
		final ImagePlus imp = geffReader.readImage();

		// Settings
		final Settings settings = geffReader.readSettings( imp );

		/*
		 * Declare the analyzers that are in the settings to the model. This is
		 * required when we are loading a file that does not have the analyzer
		 * presents at runtime declared in the settings section of the file.
		 */
		for ( final SpotAnalyzerFactoryBase< ? > analyzer : settings.getSpotAnalyzerFactories() )
			model.getFeatureModel().declareSpotFeatures(
					analyzer.getFeatures(),
					analyzer.getFeatureNames(),
					analyzer.getFeatureShortNames(),
					analyzer.getFeatureDimensions(),
					analyzer.getIsIntFeature() );

		for ( final EdgeAnalyzer analyzer : settings.getEdgeAnalyzers() )
			model.getFeatureModel().declareEdgeFeatures(
					analyzer.getFeatures(),
					analyzer.getFeatureNames(),
					analyzer.getFeatureShortNames(),
					analyzer.getFeatureDimensions(),
					analyzer.getIsIntFeature() );
		for ( final TrackAnalyzer analyzer : settings.getTrackAnalyzers() )
			model.getFeatureModel().declareTrackFeatures(
					analyzer.getFeatures(),
					analyzer.getFeatureNames(),
					analyzer.getFeatureShortNames(),
					analyzer.getFeatureDimensions(),
					analyzer.getIsIntFeature() );

		// Display settings
		final DisplaySettings displaySettings = geffReader.getDisplaySettings();

		// GUI state
		final String panelIdentifier = geffReader.getGUIState();

		// Log
		final String log = geffReader.getLog();


		// Main view.
		final GuiModel guiModel = new GuiModel( model, settings, displaySettings );
		final TrackMateModelView displayer = new HyperStackDisplayer( guiModel );
		displayer.render();

		// Wizard.
		final WizardSequence sequence = new TrackMateWizardSequence( guiModel );
		sequence.setCurrent( panelIdentifier );
		final JFrame frame = sequence.run( "From GEFF: " + savePath );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );

		// Text
		final LogPanelDescriptor2 logDescriptor = ( LogPanelDescriptor2 ) sequence.logDescriptor();
		final LogPanel logPanel = ( LogPanel ) logDescriptor.getPanelComponent();
		final Logger logger2 = logPanel.getLogger();

		logger2.log( "Session log saved in the file:\n"
				+ "--------------------\n"
				+ log
				+ "--------------------\n",
				Color.GRAY );

		final String warning = "";
		if ( !warning.isEmpty() )
		{
			logger2.log( "Warnings occurred during reading the file:\n"
					+ "--------------------\n"
					+ warning
					+ "--------------------\n",
					Color.ORANGE.darker() );
		}
		logger2.log( "File loaded on " + TMUtils.getCurrentTimeString() + '\n', Logger.BLUE_COLOR );
		final String welcomeMessage = TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION + '\n';
		// Log GUI processing start
		logger2.log( welcomeMessage, Logger.BLUE_COLOR );
		logger2.log( "Please note that TrackMate is available through Fiji, and is based on a publication. "
				+ "If you use it successfully for your research please be so kind to cite our work:\n" );
		logger2.log( StartDialogDescriptor.PUB2_TXT + "\n", Logger.GREEN_COLOR );
		logger2.log( StartDialogDescriptor.PUB2_URL + "\n", Logger.BLUE_COLOR );
		logger2.log( "and / or:\n" );
		logger2.log( StartDialogDescriptor.PUB1_TXT + "\n", Logger.GREEN_COLOR );
		logger2.log( StartDialogDescriptor.PUB1_URL + "\n", Logger.BLUE_COLOR );
		logger2.log( "and / or:\n" );
		logger2.log( StartDialogDescriptor.PUB0_TXT + "\n", Logger.GREEN_COLOR );
		logger2.log( StartDialogDescriptor.PUB0_URL + "\n", Logger.BLUE_COLOR );

		frame.setVisible( true );
	}
}
