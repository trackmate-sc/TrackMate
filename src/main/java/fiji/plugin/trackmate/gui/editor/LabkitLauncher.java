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
package fiji.plugin.trackmate.gui.editor;

import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import org.scijava.Context;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.gui.GuiModel;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.editor.labkit.component.TMLabKitFrame;
import fiji.plugin.trackmate.gui.editor.labkit.model.TMLabKitModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.ViewUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.Interval;
import sc.fiji.labkit.ui.labeling.Labeling;

public class LabkitLauncher
{

	private static boolean simplify = true;

	public static final TMLabKitFrame launch( final GuiModel guiModel, final int timepoint )
	{
		// Input model.
		final Model model = guiModel.getModel();

		// Input image.
		ImagePlus imp = guiModel.getSettings().imp;
		if ( null == imp )
			imp = ViewUtils.makeEmptyImagePlus( model );

		// ROI & interval.
		final Interval interval = TMUtils.createROIInterval( imp );

		// Create the LabKit model.
		final Context context = TMUtils.getContext();
		final DisplaySettings displaySettings = guiModel.getDisplaySettings();
		final TMLabKitModel lbModel = TMLabKitModel.create( model, imp, interval, displaySettings, timepoint, context );

		// Create the UI for editing.
		final TMLabKitFrame labkit = new TMLabKitFrame( lbModel );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		// Hook to perform reimport when we close the UI.
		labkit.onCloseListeners().addListener( () -> reimport( lbModel, timepoint ) );

		// Show the UI.
		labkit.setIconImage( Icons.SEGMENTATION_EDITOR_ICON.getImage() );
		labkit.setSize( 1000, 800 );
		GuiUtils.positionWindow( labkit, imp.getWindow() );
		labkit.setTitle( "TrackMate editor on " + imp.getShortTitle() );
		labkit.setVisible( true );
		return labkit;
	}

	private static void reimport( final TMLabKitModel lbModel, final int timepoint )
	{

		new Thread( "TrackMate-LabKit-Importer-thread" )
		{
			@Override
			public void run()
			{
				try
				{
					// Do we have something to reimport?
					if ( !lbModel.hasChanges() )
						return;

					// Check dimensionality.
					boolean isSingleTimePoint = true;
					final Labeling labeling = lbModel.imageLabelingModel().labeling().get();
					for ( final CalibratedAxis axis : labeling.axes() )
					{
						if ( axis.type().equals( Axes.TIME ) )
							isSingleTimePoint = false;
					}
					// Message the user.
					final String msg = ( isSingleTimePoint )
							? "Commit the changes made to the\n"
									+ "segmentation in the image?"
							: ( timepoint < 0 )
									? "Commit the changes made to the\n"
											+ "segmentation in whole movie?"
									: "Commit the changes made to the\n"
											+ "segmentation in frame " + ( timepoint + 1 ) + "?";
					final String title = "Commit edits to TrackMate";
					final JCheckBox chkbox = new JCheckBox( "Simplify the contours of modified spots" );
					chkbox.setSelected( simplify );
					final Object[] objs = new Object[] { msg, new JSeparator(), chkbox };
					final int returnedValue = JOptionPane.showConfirmDialog(
							null,
							objs,
							title,
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							Icons.SEGMENTATION_EDITOR_ICON_64x64 );
					if ( returnedValue != JOptionPane.YES_OPTION )
						return;
					simplify = chkbox.isSelected();

					// Re-import.
					lbModel.updateTrackMateModel( simplify, timepoint );

				}
				catch ( final Exception e )
				{
					e.printStackTrace();
				}
			}
		}.start();
	}

	public static void main( final String[] args )
	{
		final String filename = "samples/MAX_Merged.xml";
//		final String filename = "samples/221031_Stat_Stage55_561nm_part1Conf_crop_f4.xml";
		final TmXmlReader reader = new TmXmlReader( new File( filename ) );
		if ( !reader.isReadingOk() )
		{
			System.out.println( reader.getErrorMessage() );
			return;
		}

		final Model model = reader.getModel();
		final ImagePlus imp = reader.readImage();
		imp.setRoi( new Roi( 10, 30, 100, 100 ) );
		final Settings settings = reader.readSettings( imp );
		final DisplaySettings ds = reader.getDisplaySettings();
		final GuiModel guiModel = new GuiModel( model, settings, ds );

		// Main view.
		guiModel.getWindowManager().createHyperStackDisplayer();
		imp.setSlice( 7 );

		// Editor
		LabkitLauncher.launch( guiModel, -1 );
	}
}
