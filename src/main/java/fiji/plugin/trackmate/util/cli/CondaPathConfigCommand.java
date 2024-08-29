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
package fiji.plugin.trackmate.util.cli;

import java.io.IOException;
import java.util.Map;

import javax.swing.JLabel;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;

import fiji.plugin.trackmate.gui.Fonts;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.gui.Icons;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;

@Plugin( type = Command.class,
		label = "Configure the path to the Conda executable used in TrackMate...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit >  Options > Configure TrackMate Conda path..." )
public class CondaPathConfigCommand implements Command
{

	@Override
	public void run()
	{
		final PrefService prefs = TMUtils.getContext().getService( PrefService.class );
		String findPath;
		try
		{
			findPath = CLIUtils.findDefaultCondaPath();
		}
		catch ( final IllegalArgumentException e )
		{
			findPath = "/usr/local/opt/micromamba/bin/micromamba";
		}

		String condaPath = prefs.get( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, findPath );

		final GenericDialogPlus dialog = new GenericDialogPlus( "TrackMate Conda path" );
		final JLabel lbl = dialog.addImage( GuiUtils.scaleImage( Icons.TRACKMATE_ICON, 64, 64 ) );
		lbl.setText( "TrackMate Conda path" );
		lbl.setFont( Fonts.BIG_FONT );
		dialog.addMessage(
				"Browse to the conda (or mamba, micromaba, ...) executable \n"
				+ "to use in the TrackMate modules that rely on Conda." );
		dialog.addMessage( "Conda executable path:" );
		dialog.addFileField( "", condaPath, 40 );
		dialog.showDialog();

		if ( dialog.wasCanceled() )
			return;

		condaPath = dialog.getNextString();
		prefs.put( CLIUtils.class, CLIUtils.CONDA_PATH_PREF_KEY, condaPath );
		test();
	}

	public void test()
	{
		try
		{
			final Map< String, String > map = CLIUtils.getEnvMap();
			final StringBuilder str = new StringBuilder();
			str.append( "Successfully retrieved the conda environment list:\n" );
			map.forEach( ( k, v ) -> str.append( String.format( " - %-20s → %s\n", k, v ) ) );
			IJ.log( str.toString() );
		}
		catch ( final IOException e )
		{
			IJ.error( "Conda executable path seems to be incorrect.\n"
					+ "Error message:\n "
					+ e.getMessage() );
		}
	}

	public static void main( final String[] args )
	{
		TMUtils.getContext().getService( CommandService.class ).run( CondaPathConfigCommand.class, false );
	}
}
