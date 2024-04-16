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
package fiji.plugin.trackmate.action.closegaps;

import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JFrame;
import javax.swing.JLabel;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.EverythingDisablerAndReenabler;
import fiji.plugin.trackmate.util.Threads;

public class CloseGapsController
{

	private final TrackMate trackmate;

	private final Logger logger;

	private final CloseGapsPanel gui;

	public CloseGapsController( final TrackMate trackmate, final Logger logger )
	{
		this.trackmate = trackmate;
		this.logger = logger;

		final Collection< GapClosingMethod > gapClosingMethods = new ArrayList<>( 2 );
		gapClosingMethods.add( new CloseGapsByLinearInterpolation() );
		gapClosingMethods.add( new CloseGapsByDetection() );

		this.gui = new CloseGapsPanel( gapClosingMethods );
		gui.btnRun.addActionListener( e -> run( ( ( GapClosingMethod ) gui.cmbboxMethod.getSelectedItem() ) ) );
	}

	private void run( final GapClosingMethod gapClosingMethod )
	{
		final EverythingDisablerAndReenabler disabler = new EverythingDisablerAndReenabler( gui, new Class[] { JLabel.class } );
		disabler.disable();
		Threads.run( "TrackMateGapClosingThread", () ->
		{
			try
			{
				logger.log( "Applying gap-closing method: " + gapClosingMethod.toString() + ".\n" );
				logger.setStatus( "Gap-closing" );
				gapClosingMethod.execute( trackmate, logger );
				logger.log( "Gap-closing done.\n" );
			}
			finally
			{
				disabler.reenable();
			}
		} );
	}

	public void show()
	{
		if ( gui.getParent() != null && gui.getParent().isVisible() )
			return;

		final JFrame frame = new JFrame( "TrackMate gap-closing" );
		frame.setIconImage( CloseGapsAction.ICON.getImage() );
		frame.setSize( 300, 500 );
		frame.getContentPane().add( gui );
		GuiUtils.positionWindow( frame, trackmate.getSettings().imp.getCanvas() );
		frame.setVisible( true );
	}
}
