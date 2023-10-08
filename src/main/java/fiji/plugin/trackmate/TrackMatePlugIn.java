/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2010 - 2024 TrackMate developers.
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

import javax.swing.JFrame;

import org.scijava.object.ObjectService;

import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.gui.featureselector.AnalyzerSelection;
import fiji.plugin.trackmate.gui.featureselector.AnalyzerSelectionIO;
import fiji.plugin.trackmate.gui.wizard.TrackMateWizardSequence;
import fiji.plugin.trackmate.gui.wizard.WizardSequence;
import fiji.plugin.trackmate.io.SettingsPersistence;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class TrackMatePlugIn implements PlugIn
{

	@Override
	public void run( final String imagePath )
	{
//		GuiUtils.setSystemLookAndFeel();
		final ImagePlus imp;
		if ( imagePath != null && imagePath.length() > 0 )
		{
			imp = IJ.openImage( imagePath );
			if ( imp == null || null == imp.getOriginalFileInfo() )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION, "Could not load image with path " + imagePath + "." );
				return;
			}
		}
		else
		{
			imp = WindowManager.getCurrentImage();
			if ( null == imp )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION,
						"Please open an image before running TrackMate." );
				return;
			}
			else if ( imp.getType() == ImagePlus.COLOR_RGB )
			{
				IJ.error( TrackMate.PLUGIN_NAME_STR + " v" + TrackMate.PLUGIN_NAME_VERSION,
						"TrackMate does not work on RGB images." );
				return;
			}
		}

		imp.setOpenAsHyperStack( true );
		imp.setDisplayMode( IJ.COMPOSITE );
		if ( !imp.isVisible() )
			imp.show();

		GuiUtils.userCheckImpDimensions( imp );

		// Main objects.
		final Settings settings = createSettings( imp );
		final Model model = createModel( imp );
		final TrackMate trackmate = createTrackMate( model, settings );
		final SelectionModel selectionModel = new SelectionModel( model );
		final DisplaySettings displaySettings = createDisplaySettings();

		// Main view.
		final TrackMateModelView displayer = new HyperStackDisplayer( model, selectionModel, imp, displaySettings );
		displayer.render();

		// Wizard.
		final WizardSequence sequence = createSequence( trackmate, selectionModel, displaySettings );
		final JFrame frame = sequence.run( "TrackMate on " + imp.getShortTitle() );
		frame.setIconImage( TRACKMATE_ICON.getImage() );
		GuiUtils.positionWindow( frame, imp.getWindow() );
		frame.setVisible( true );
	}

	/**
	 * Hook for subclassers: <br>
	 * Will create and position the sequence that will be played by the wizard
	 * launched by this plugin.
	 * 
	 * @param trackmate
	 *            the TrackMate instance.
	 * @param selectionModel
	 *            the selection model.
	 * @param displaySettings
	 *            the display settings.
	 * @return a new sequence.
	 */
	protected WizardSequence createSequence( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings )
	{
		return new TrackMateWizardSequence( trackmate, selectionModel, displaySettings );
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Model} instance that will be used to store data in the
	 * {@link TrackMate} instance.
	 * 
	 * @param imp
	 *            the image the tracking data will be created on.
	 * @return a new {@link Model} instance.
	 */
	protected Model createModel( final ImagePlus imp )
	{
		final Model model = new Model();
		model.setPhysicalUnits(
				imp.getCalibration().getUnit(),
				imp.getCalibration().getTimeUnit() );
		return model;
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the {@link Settings} instance that will be used to tune the
	 * {@link TrackMate} instance. It is initialized by default with values
	 * taken from the current {@link ImagePlus}.
	 *
	 * @param imp
	 *            the {@link ImagePlus} to operate on.
	 * @return a new {@link Settings} instance.
	 */
	protected Settings createSettings( final ImagePlus imp )
	{
		// Persistence.
		final Settings settings = SettingsPersistence.readLastUsedSettings( imp, Logger.DEFAULT_LOGGER );
		// Add the analyzers configured by the user.
		final AnalyzerSelection analyzerSelection = AnalyzerSelectionIO.readUserDefault();
		analyzerSelection.configure( settings );
		return settings;
	}

	/**
	 * Hook for subclassers: <br>
	 * Creates the TrackMate instance that will be controlled in the GUI.
	 * 
	 * @param model
	 *            the model to create the TrackMate instance with.
	 * @param settings
	 *            the settings to create the TrackMate instance with.
	 * @return a new {@link TrackMate} instance.
	 */
	protected TrackMate createTrackMate( final Model model, final Settings settings )
	{
		/*
		 * Since we are now sure that we will be working on this model with this
		 * settings, we need to pass to the model the units from the settings.
		 */
		final String spaceUnits = settings.imp.getCalibration().getXUnit();
		final String timeUnits = settings.imp.getCalibration().getTimeUnit();
		model.setPhysicalUnits( spaceUnits, timeUnits );

		final TrackMate trackmate = new TrackMate( model, settings );

		// Register it to the object service.
		final ObjectService objectService = TMUtils.getContext().service( ObjectService.class );
		if ( objectService != null )
			objectService.addObject( trackmate );

		// Set the num of threads from IJ prefs.
		trackmate.setNumThreads( Prefs.getThreads() );

		return trackmate;
	}

	protected DisplaySettings createDisplaySettings()
	{
		return DisplaySettingsIO.readUserDefault().copy( "CurrentDisplaySettings" );
	}

	public static void main( final String[] args )
	{
//		GuiUtils.setSystemLookAndFeel();
		ImageJ.main( args );
//		new TrackMatePlugIn().run( "samples/Stack.tif" );
//		new TrackMatePlugIn().run( "samples/Merged.tif" );
		new TrackMatePlugIn().run( "samples/MAX_Merged.tif" );
//		new TrackMatePlugIn().run( "samples/Mask.tif" );
//		new TrackMatePlugIn().run( "samples/FakeTracks.tif" );
	}
}
