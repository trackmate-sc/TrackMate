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
package fiji.plugin.trackmate.gui.featureselector;

import static fiji.plugin.trackmate.gui.Icons.TRACKMATE_ICON;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class,
		label = "Configure TrackMate feature analyzers...",
		iconPath = "/icons/commands/information.png",
		menuPath = "Edit >  Options > Configure TrackMate feature analyzers...",
		description = "Shows a dialog that allows configuring what feature analyzers will be used "
				+ "in the next TrackMate session." )

public class AnalyzerSelector implements Command
{

	private final JDialog dialog;

	private final AnalyzerSelectorPanel gui;

	public AnalyzerSelector()
	{
		dialog = new JDialog( ( JFrame ) null, "TrackMate feature analyzers selection" );
		dialog.setLocationByPlatform( true );
		dialog.setLocationRelativeTo( null );
		gui = new AnalyzerSelectorPanel( AnalyzerSelectionIO.readUserDefault() );
		dialog.getContentPane().add( gui );
		dialog.setIconImage( TRACKMATE_ICON.getImage() );
		dialog.pack();
	}

	public JDialog getDialog()
	{
		return dialog;
	}

	@Override
	public void run()
	{
		dialog.setVisible( true );
	}

	public static void main( final String[] args )
	{
		new AnalyzerSelector().run();
	}
}
