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
package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.gui.Icons.ICY_ICON;

import java.awt.Frame;
import java.io.File;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.io.IOUtils;
import fiji.plugin.trackmate.io.IcyTrackFormatWriter;

public class IcyTrackExporter extends AbstractTMAction
{

	private static final String INFO_TEXT = "<html>"
			+ "Export the visible tracks in the current model to a "
			+ "XML file that can be read by the TrackManager plugin of the "
			+ "<a href='http://icy.bioimageanalysis.org/'>Icy software</a>."
			+ "</html>";

	private static final String NAME = "Export tracks to Icy";

	private static final String KEY = "ICY_EXPORTER";

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		logger.log( "Exporting tracks to Icy format.\n" );
		final Model model = trackmate.getModel();
		final int ntracks = model.getTrackModel().nTracks( true );
		if ( ntracks == 0 )
		{
			logger.log( "No visible track found. Aborting.\n" );
			return;
		}

		File folder;
		try
		{
			folder = new File( trackmate.getSettings().imp.getOriginalFileInfo().directory );
		}
		catch ( final NullPointerException npe )
		{
			folder = new File( System.getProperty( "user.dir" ) ).getParentFile().getParentFile();
		}

		File file;
		try
		{
			String filename = trackmate.getSettings().imageFileName;
			final int dotLoca = filename.indexOf( "." );
			if ( dotLoca > 0 )
				filename = filename.substring( 0, dotLoca );
			file = new File( folder.getPath() + File.separator + filename + "_Icy.xml" );
		}
		catch ( final NullPointerException npe )
		{
			file = new File( folder.getPath() + File.separator + "IcyTracks.xml" );
		}
		file = IOUtils.askForFileForSaving( file, parent );
		if ( null == file )
		{
			logger.log( "Exporting to Icy aborted.\n" );
			return;
		}

		logger.log( "  Writing to file.\n" );

		final double[] calibration = new double[ 3 ];
		calibration[ 0 ] = trackmate.getSettings().dx;
		calibration[ 1 ] = trackmate.getSettings().dy;
		calibration[ 2 ] = trackmate.getSettings().dz;
		final IcyTrackFormatWriter writer = new IcyTrackFormatWriter( file, model, calibration );

		if ( !writer.checkInput() || !writer.process() )
		{
			logger.error( writer.getErrorMessage() );
		}
		else
		{
			logger.log( "Done.\n" );
		}
	}

	@Plugin( type = TrackMateActionFactory.class, enabled = true )
	public static class Factory implements TrackMateActionFactory
	{

		@Override
		public String getInfoText()
		{
			return INFO_TEXT;
		}

		@Override
		public String getName()
		{
			return NAME;
		}

		@Override
		public String getKey()
		{
			return KEY;
		}

		@Override
		public TrackMateAction create()
		{
			return new IcyTrackExporter();
		}

		@Override
		public ImageIcon getIcon()
		{
			return ICY_ICON;
		}
	}
}
