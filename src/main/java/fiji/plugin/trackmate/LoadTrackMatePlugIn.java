/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2026 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JFrame;

import org.scijava.util.VersionUtils;

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactoryBase;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.components.LogPanel;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import fiji.plugin.trackmate.gui.wizard.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.gui.wizard.descriptors.LogPanelDescriptor2;
import fiji.plugin.trackmate.gui.wizard.descriptors.StartDialogDescriptor;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.SettingsPersistence;
import fiji.plugin.trackmate.io.TrackMateFileReader;
import fiji.plugin.trackmate.io.TrackMateFileReaders;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.IJ;
import ij.ImagePlus;

public class LoadTrackMatePlugIn extends TrackMatePlugIn
{

	/**
	 * Loads a TrackMate file in the GUI.
	 *
	 * @param filePath
	 *            the path to a TrackMate file to load. If <code>null</code>
	 *            or 0-length, the user will be asked to browse to a TrackMate
	 *            file.
	 */
	@Override
	public void run( final String filePath )
	{
		final Logger logger = Logger.IJ_LOGGER;
		File file;
		if ( null == filePath || filePath.length() == 0 )
		{
			final Settings lastUsedSettings = SettingsPersistence.readLastUsedSettings( null, logger );
			file = TMUtils.proposeTrackMateSaveFile( lastUsedSettings, Logger.VOID_LOGGER );
			file = IOUtils.askForFileForLoading( file, "Load a TrackMate GEFF file", null, logger );
			if ( null == file )
				return;
		}
		else
		{
			file = new File( filePath );
			if ( !file.exists() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION,
						"Could not find file with path " + filePath + "." );
				return;
			}
			if ( !file.canRead() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION,
						"Could not read file with path " + filePath + "." );
				return;
			}
		}

		// Open with the reader matching the file format.
		final TrackMateFileReader reader = TrackMateFileReaders.open( file.getPath(), logger );

		final String version = reader.getVersion();
		logger.log( "TrackMate file version: " + ( null == version ? "unknown" : version ) + '\n' );

		if ( !reader.isReadingOk() )
		{
			logger.error( reader.getErrorMessage() );
			logger.error( "Aborting.\n" );
			return;
		}

		// Since v7.0.0 we do not support TrackMate file version < 2.1.0.
		if ( null != version && VersionUtils.compare( version, "2.1.0" ) < 0 )
		{
			logger.error( "Cannot read TrackMate file version lower than 2.1.0.\nAborting.\n" );
			return;
		}

		// We could open the file. Let's remember it.
		lastLoadedFile = new File( reader.getSuggestedSaveFile() );

		openWith( reader, logger );
	}

	/**
	 * Loads the model, the settings and the GUI from the specified reader, and
	 * opens the TrackMate wizard on it. This is the assembly logic, written
	 * against the {@link TrackMateFileReader} interface so that it is
	 * independent of the file format.
	 *
	 * @param reader
	 *            the reader to load from.
	 * @param logger
	 *            the logger to use for messaging.
	 */
	private void openWith( final TrackMateFileReader reader, final Logger logger )
	{
		// Log
		final String logText = reader.getLog() + '\n';

		// Model
		final Model model = reader.getModel();
		if ( null == model )
		{
			logger.error( "Problem reading the model:\n" + reader.getErrorMessage()
					+ "\nAborting.\n" );
			return;
		}
		if ( !reader.isReadingOk() )
			logger.error( "Problem reading the model:\n" + reader.getErrorMessage() + '\n' );

		/*
		 * Read image.
		 */

		ImagePlus imp = reader.readImage();
		if ( null == imp )
			imp = ViewUtils.makeEmptyImagePlus( model );

		/*
		 * Read settings.
		 */

		Settings settings = reader.readSettings( imp );
		if ( null == settings )
		{
			logger.error( "Problem reading the settings:\n"
					+ "The file did not contain a settings element. Using default values.\n" );
			settings = new Settings( imp );
		}

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

		// Display settings.
		final DisplaySettings displaySettings = reader.getDisplaySettings();

		/*
		 * Create GuiModel.
		 */

		final GuiModel guiModel = new GuiModel( model, settings, displaySettings );

		// Main view.
		guiModel.getWindowManager().createHyperStackDisplayer();

		// GUI state
		String panelIdentifier = reader.getGUIState();

		if ( null == panelIdentifier )
			panelIdentifier = ConfigureViewsDescriptor.KEY;

		// Wizard.
		final WizardSequence sequence = createSequence( guiModel );
		sequence.setCurrent( panelIdentifier );
		final JFrame frame = sequence.run( "TrackMate on " + settings.imp.getShortTitle() );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, settings.imp.getWindow() );
		frame.setVisible( true );
		final Dimension size = frame.getSize();
		frame.setSize( size.width, size.height + 1 );

		// Text
		final LogPanelDescriptor2 logDescriptor = ( LogPanelDescriptor2 ) sequence.logDescriptor();
		final LogPanel logPanel = ( LogPanel ) logDescriptor.getPanelComponent();
		final Logger logger2 = logPanel.getLogger();

		logger2.log( "Session log saved in the file:\n"
				+ "--------------------\n"
				+ logText
				+ "--------------------\n",
				Color.GRAY );

		final String warning = reader.getErrorMessage();
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
		logger2.log( StartDialogDescriptor.PUB1_TXT + "\n", Logger.GREEN_COLOR );
		logger2.log( StartDialogDescriptor.PUB1_URL + "\n", Logger.BLUE_COLOR );
		logger2.log( "and / or:\n" );
		logger2.log( "Tinevez, JY.; Perry, N. & Schindelin, J. et al. (2017), 'TrackMate: An open and extensible platform for single-particle tracking.', "
				+ "Methods 115: 80-90, PMID 27713081.\n", Logger.GREEN_COLOR );
		logger2.log( "https://www.sciencedirect.com/science/article/pii/S1046202316303346\n", Logger.BLUE_COLOR );

		if ( !reader.isReadingOk() )
		{
			logger2.error( "Some errors occurred while reading file:\n" );
			logger2.error( reader.getErrorMessage() );
		}
	}
}
